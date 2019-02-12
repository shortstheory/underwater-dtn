package dtn

import groovy.transform.CompileStatic
import org.apache.commons.lang3.tuple.MutablePair
import org.apache.commons.lang3.tuple.Pair
import org.arl.fjage.AgentID
import org.arl.fjage.CyclicBehavior
import org.arl.fjage.Message
import org.arl.fjage.Performative
import org.arl.fjage.TickerBehavior
import org.arl.fjage.WakerBehavior
import org.arl.unet.Address
import org.arl.unet.CapabilityReq
import org.arl.unet.DatagramCapability
import org.arl.unet.DatagramDeliveryNtf
import org.arl.unet.DatagramFailureNtf
import org.arl.unet.DatagramNtf
import org.arl.unet.DatagramParam
import org.arl.unet.DatagramReq
import org.arl.unet.Parameter
import org.arl.unet.Services
import org.arl.unet.UnetAgent
import org.arl.unet.link.ReliableLinkParam
import org.arl.unet.nodeinfo.NodeInfoParam
import org.arl.unet.phy.Physical
import org.arl.unet.phy.RxFrameNtf

//@TypeChecked
@CompileStatic
class DtnLink extends UnetAgent {
    public static final int HEADER_SIZE = 8
    public static final int DTN_PROTOCOL = 50

    private final int RANDOM_DELAY = 5000

    int dtnGramsRec = 0
    int MTU

    private DtnStorage storage
    private int nodeAddress

    // and we are using only 1 link
    private AgentID link
    private AgentID notify
    private AgentID nodeInfo
    private AgentID phy
    private AgentID mac
    private AgentID[] links

    private CyclicBehavior datagramCycle
    private TickerBehavior beaconBehavior

    private DtnUtility utility

    int BEACON_PERIOD = 100*1000
    int SWEEP_PERIOD = 100*1000
    int DATAGRAM_PERIOD = 10*1000

    public int currentTimeSeconds() {
        return (currentTimeMillis()/1000).intValue()
    }

    @Override
    protected void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)

        addCapability(DatagramCapability.RELIABILITY)
    }

    List<Parameter> getParameterList() {
        allOf(DtnLinkParameters)
    }

    @Override
    protected void startup() {
        nodeInfo = agentForService(Services.NODE_INFO)
        mac = agentForService(Services.MAC)
        links = agentsForService(Services.LINK)

        nodeAddress = (int)get(nodeInfo, NodeInfoParam.address)
        notify = topic()

        storage = new DtnStorage(this, Integer.toString(nodeAddress))
        utility = new DtnUtility()

        link = getLinkWithReliability()

        if (link != null) {
            subscribe(link)
            phy = agent((String)get(link, ReliableLinkParam.phy))
            if (phy != null) {
                subscribe(phy)
                subscribe(topic(phy, Physical.SNOOP))
            } else {
                println "PHY not provided for link"
            }
        }

        beaconBehavior = (TickerBehavior)add(createBeaconBehavior())
        add(new TickerBehavior(SWEEP_PERIOD) {
            @Override
            void onTick() {
                ArrayList<Tuple2> expiredDatagrams = storage.deleteExpiredDatagrams()
                // now send DFNs for all of these
                for (Tuple2 expiredDatagram : expiredDatagrams) {
                    notify.send(new DatagramFailureNtf(inReplyTo: (String)expiredDatagram.getFirst(),
                                                       to: (int)expiredDatagram.getSecond()))
                }
            }
        })

        add(new TickerBehavior(DATAGRAM_PERIOD) {
            @Override
            void onTick() {
                datagramCycle.restart()
            }
        })

        datagramCycle = (CyclicBehavior)add(new CyclicBehavior() {
            @Override
            void action() {
                for (Map.Entry<Integer, AgentID> entry : liveLinks.entrySet()) {
                    if (entry != null) {
                        int node = entry.getKey()
                        AgentID nodeLink = entry.getValue()
                        String messageID = storage.getNextHopDatagrams(node)[0]
                        // this logic blocks the queue if we get a errant DG! Not good!
                        if (messageID != null && storage.db.get(messageID).attempts == 0) {
                            sendDatagram(messageID, node, nodeLink)
                        }
                    }
                }
                block()
            }
        })
    }

    AgentID getLinkWithReliability() {
        for (AgentID link : links) {
            CapabilityReq req = new CapabilityReq(link, DatagramCapability.RELIABILITY)
            Message rsp = request(req, 1000)
            if (rsp != null && rsp.getPerformative() == Performative.CONFIRM &&
                (int)get(link, DatagramParam.MTU) > HEADER_SIZE &&
                link.getName() != getName()) { // we don't want to use the DtnLink!
                println("Candidate Link " + link.getName())
                return link
            }
        }
        println("No link with reliability found")
        return null
    }

    @Override
    protected Message processRequest(Message msg) {
        if (msg instanceof DatagramReq) {
            // FIXME: check for buffer space too, probably in saveDatagram!
            if (msg.getTtl() == Float.NaN || !storage.saveDatagram(msg)) {
                println("No Datagram!")
                return new Message(msg, Performative.REFUSE)
            } else {
                return new Message(msg, Performative.AGREE)
            }
        }
        return null
    }

    @Override
    protected void processMessage(Message msg) {
         if (msg instanceof RxFrameNtf) {
            // FIXME: should this only be for SNOOP?

//             liveLinks.put(msg.getFrom(), new Tuple2<AgentID, Integer>(agent("link"), currentTimeSeconds())
        } else if (msg instanceof DatagramNtf) {
            if (msg.getProtocol() == DTN_PROTOCOL) {
                // FIXME: check for buffer space, or abstract it
                byte[] pduBytes = msg.getData()
                Tuple pduTuple = storage.decodePdu(pduBytes)
                if (pduTuple != null) {
                    int ttl = (int) pduTuple.get(0)
                    int protocol = (int) pduTuple.get(1)
                    byte[] data = (byte[]) pduTuple.get(2)

                    DatagramNtf ntf = new DatagramNtf()
                    ntf.setProtocol(protocol)
                    ntf.setData(data)
                    // FIXME: ntf.setTtl(ttl)
                    notify.send(ntf)
                }
            }
        // we don't need to handle other protocol numbers
        } else if (msg instanceof DatagramDeliveryNtf) {
            int node = msg.getTo()
            String messageID = msg.getInReplyTo()
            String originalMessageID = storage.getOriginalMessageID(messageID)
            storage.deleteFile(originalMessageID)
            DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalMessageID, to: node)
            notify.send(deliveryNtf)
            datagramCycle.restart()
        } else if (msg instanceof DatagramFailureNtf) {
             // we reset the sent flag in hope of resending the message later on
//             String messageID = msg.getInReplyTo()
//             String originalMessageID = storage.getOriginalMessageID(messageID)
//             storage.db.get(originalMessageID).
        }
    }

    void sendDatagram(String messageID, int node, AgentID nodeLink) {
        byte[] pdu = storage.getPDU(messageID)
        if (pdu != null) {
            DatagramReq datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                                                      data: pdu,
                                                      to: node,
                                                      reliability: true)
            storage.trackDatagram(datagramReq.getMessageID(), messageID)
            nodeLink.send(datagramReq)
        }
    }

    TickerBehavior createBeaconBehavior() {
        return new TickerBehavior(BEACON_PERIOD) {
            @Override
            void onTick() {
                super.onTick()
                int currentTime = currentTimeSeconds()
                int gapTime = (BEACON_PERIOD / 1000).intValue()
                if (currentTime - lastReceivedTime >= gapTime) {
                    int randomDelay = (int) (Math.random() * RANDOM_DELAY)
                    add(new WakerBehavior(randomDelay) {
                        @Override
                        void onWake() {
                            link.send(new DatagramReq(to: Address.BROADCAST))
                        }
                    })
                }
            }
        }
    }

    int getMTU() {
        if (link != null) {
            return (int)get(link, DatagramParam.MTU) - HEADER_SIZE
        }
        return 0
    }

// FIXME: The period of a TickerBehavior cannot be modified!
    void setBEACON_PERIOD(int period) {
        BEACON_PERIOD = period
        beaconBehavior.stop()
        if (BEACON_PERIOD == 0) {
            println "Stopped beacon"
        } else {
            println "Changed beacon interval"
            beaconBehavior = (TickerBehavior)add(createBeaconBehavior())
        }
    }
}
