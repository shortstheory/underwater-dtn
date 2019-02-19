package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.AgentID
import org.arl.fjage.CyclicBehavior
import org.arl.fjage.Message
import org.arl.fjage.Performative
import org.arl.fjage.PoissonBehavior
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
import org.arl.unet.phy.BadFrameNtf
import org.arl.unet.phy.CollisionNtf
import org.arl.unet.phy.Physical
import org.arl.unet.phy.RxFrameNtf
import org.omg.CORBA.INTERNAL
import sun.awt.image.ImageWatched

import javax.xml.crypto.Data
import java.lang.reflect.Method

//@TypeChecked
@CompileStatic
class DtnLink extends UnetAgent {
    public static final int HEADER_SIZE = 8
    public static final int DTN_PROTOCOL = 50

    int MTU

    public DtnStats stats

    private DtnStorage storage
    private int nodeAddress
    private String directory

    // and we are using only 1 link
    private AgentID link
    private AgentID notify
    private AgentID nodeInfo
    private AgentID phy
    private AgentID[] links

    private CyclicBehavior datagramCycle
    private PoissonBehavior beaconBehavior

    private DtnLinkInfo utility
    private LinkState linkState
    private DatagramPriority priority
    private Random random

    int BEACON_PERIOD = 100*1000
    int SWEEP_PERIOD = 100*1000
    int DATAGRAM_PERIOD = 10*1000
    int RANDOM_DELAY = 5000

    List<Parameter> getParameterList() {
        allOf(DtnLinkParameters)
    }

    enum DatagramPriority {
        ARRIVAL, EXPIRY, RANDOM
    }

    enum LinkState {
        READY, WAITING
    }

    DtnLink(String dir) {
        directory = dir
        priority = DatagramPriority.EXPIRY

        random = new Random()
        linkState = LinkState.READY
    }

    int currentTimeSeconds() {
        return (currentTimeMillis()/1000).intValue()
    }

    @Override
    protected void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)
        // FIXME: do we really support reliability
        addCapability(DatagramCapability.RELIABILITY)
    }

    @Override
    protected void startup() {
        nodeInfo = agentForService(Services.NODE_INFO)
        links = agentsForService(Services.LINK)

        nodeAddress = (int)get(nodeInfo, NodeInfoParam.address)
        notify = topic()

        stats = new DtnStats(nodeAddress)
        storage = new DtnStorage(this, directory)
        utility = new DtnLinkInfo(this)

        link = getLinkWithReliability()
        utility.addLink(link)

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

        beaconBehavior = (PoissonBehavior)add(createBeaconBehavior())
        add(new TickerBehavior(SWEEP_PERIOD) {
            @Override
            void onTick() {
                ArrayList<Tuple2> expiredDatagrams = storage.deleteExpiredDatagrams()
                // now send DFNs for all of these
                for (Tuple2 expiredDatagram : expiredDatagrams) {
                    notify.send(new DatagramFailureNtf(inReplyTo: (String)expiredDatagram.getFirst(),
                                                       to: (int)expiredDatagram.getSecond()))
                    println "Datagram - " + expiredDatagram.getFirst() + " has expired"
                    stats.datagrams_expired++
                }
            }
        })

        datagramCycle = (CyclicBehavior)add(new CyclicBehavior() {
            @Override
            void action() {
                for (Map.Entry<Integer, AgentID> entry : utility.getNodeLiveLinks().entrySet()) {
                    if (entry != null) {
                        int node = entry.getKey()
                        AgentID nodeLink = entry.getValue()
                        ArrayList<String> datagrams = storage.getNextHopDatagrams(node)
                        String messageID = selectNextDatagram(datagrams)
                        if (messageID != null) {
                            sendDatagram(messageID, node, nodeLink)
                        }
                    }
                }
                block()
            }
        })

        add(new PoissonBehavior(DATAGRAM_PERIOD) {
            @Override
            void onTick() {
                utility.deleteExpiredLinks()
                datagramCycle.restart()
            }
        })

    }

    String selectNextDatagram(ArrayList<String> datagrams) {
        switch (priority) {
        case DatagramPriority.ARRIVAL:
            return (datagrams.size()) ? datagrams.get(0) : null
        case DatagramPriority.EXPIRY:
            int minExpiryTime = Integer.MAX_VALUE
            String messageID
            for (String id : datagrams) {
                DtnPduMetadata metadata = storage.getDatagramMetadata(id)
                if (metadata != null & metadata.expiryTime < minExpiryTime) {
                    messageID = id
                    minExpiryTime = metadata.expiryTime
                }
            }
            return messageID
        case DatagramPriority.RANDOM:
            return (datagrams.size()) ? datagrams.get(random.nextInt(datagrams.size())) : null
        }
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
                stats.datagrams_requested++
//                println "New DGram added"
                return new Message(msg, Performative.AGREE)
            }
        }
        return null
    }

    @Override
    protected void processMessage(Message msg) {
         if (msg instanceof RxFrameNtf) {
             stats.beacons_snooped++
             utility.updateLiveLinks(msg.getFrom(), msg.getRecipient())
         } else if (msg instanceof DatagramNtf) {
            if (msg.getProtocol() == DTN_PROTOCOL) {
                // FIXME: check for buffer space, or abstract it
                byte[] pduBytes = msg.getData()
                Tuple pduTuple = storage.decodePdu(pduBytes)
                if (pduTuple != null) {
                    int ttl = (int)pduTuple.get(0)
                    int protocol = (int)pduTuple.get(1)
                    byte[] data = (byte[])pduTuple.get(2)

                    DatagramNtf ntf = new DatagramNtf()
                    ntf.setProtocol(protocol)
                    ntf.setData(data)
                    // FIXME: ntf.setTtl(ttl)
                    notify.send(ntf)
                    stats.datagrams_received++
                }
            }
        // once we have received a DDN/DFN, we can send another one
        } else if (msg instanceof DatagramDeliveryNtf) {
            int node = msg.getTo()
            String messageID = msg.getInReplyTo()
            String originalMessageID = storage.getOriginalMessageID(messageID)
             // it can happen that the DDN comes just after a TTL
            if (originalMessageID != null) {
                int deliveryTime = storage.getTimeSinceArrival(originalMessageID)
                if (deliveryTime >= 0) {
                    stats.delivery_times.add(deliveryTime)
                }
                storage.setDelivered(originalMessageID)
                DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalMessageID, to: node)
                notify.send(deliveryNtf)
                stats.datagrams_success++
            }
            linkState = LinkState.READY
            datagramCycle.restart()
        } else if (msg instanceof DatagramFailureNtf) {
            stats.datagrams_failed++
            storage.removeFailedEntry(msg.getInReplyTo())
            linkState = LinkState.READY
        } else if (msg instanceof CollisionNtf) {
            stats.frame_collisions++
        } else if (msg instanceof BadFrameNtf) {
            stats.bad_frames++
         }
    }

    void sendDatagram(String messageID, int node, AgentID nodeLink) {
        if (linkState == LinkState.READY) {
            byte[] pdu = storage.getPDU(messageID, true)
            if (pdu != null) {
                linkState = LinkState.WAITING
                add(new WakerBehavior(random.nextInt(RANDOM_DELAY)) {
                    @Override
                    void onWake() {
                        DtnPduMetadata metadata = storage.getMetadata(messageID)
                        if (metadata != null && !metadata.delivered) {
                            if (metadata.attempts > 0) {
                                stats.datagrams_resent++
                                println("Resending datagram: " + messageID + " attempt " + storage.getMetadata(messageID).attempts)
                            }
                            DatagramReq datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                                    data: pdu,
                                    to: node,
                                    reliability: true)
                            storage.trackDatagram(datagramReq.getMessageID(), messageID)
                            nodeLink.send(datagramReq)
                            stats.datagrams_sent++
                        } else {
                            linkState = LinkState.READY
                        }
                    }
                })
            }
        }
    }

    PoissonBehavior createBeaconBehavior() {
        return new PoissonBehavior(BEACON_PERIOD) {
            @Override
            void onTick() {
                int beaconPeriod = (BEACON_PERIOD / 1000).intValue()
                for (AgentID linkID : utility.getLinkPhyMap().keySet()) {
                    int lastTransmission = utility.getLastTransmission(linkID)
//                    if (currentTimeSeconds() - lastTransmission >= beaconPeriod) {
                        linkID.send(new DatagramReq(to: Address.BROADCAST))
//                    }
                }
            }
        }
    }

    Object getProperty(AgentID aid, Parameter param) {
        return get(aid, param)
    }

    int getMTU() {
        if (link != null) {
            return (int)get(link, DatagramParam.MTU) - HEADER_SIZE
        }
        return 0
    }

    void setBEACON_PERIOD(int period) {
        BEACON_PERIOD = period
        beaconBehavior.stop()
        if (BEACON_PERIOD == 0) {
            println "Stopped beacon"
        } else {
            println "Changed beacon interval"
            beaconBehavior = (PoissonBehavior)add(createBeaconBehavior())
        }
    }

    DtnStats getStats() {
        return stats
    }

    @Override
    void stop() {
        super.stop()
        stats.writeToFile()
    }
}
