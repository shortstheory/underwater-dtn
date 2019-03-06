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
import org.arl.unet.Protocol
import org.arl.unet.Services
import org.arl.unet.UnetAgent
import org.arl.unet.nodeinfo.NodeInfoParam
import org.arl.unet.phy.BadFrameNtf
import org.arl.unet.phy.CollisionNtf
import org.arl.unet.phy.RxFrameNtf

//@TypeChecked
@CompileStatic
class DtnLink extends UnetAgent {
    /////////////////////// Constants

    /**
     * DtnLink header comprises of the message TTL and protocol number
     */
    public static final int HEADER_SIZE = 5
    public static final int DTN_PROTOCOL = 50

    int MTU

    /**
     * Collects statistics for the simulation
     */
    public DtnStats stats

    /**
     * Manages the storage of pending datagrams
     */
    private DtnStorage storage
    private int nodeAddress
    private String directory

    private ArrayList<AgentID> linksWithReliability
    private AgentID notify
    private AgentID nodeInfo

    private CyclicBehavior datagramCycle
    private PoissonBehavior beaconBehavior

    private DtnLinkInfo utility
    private LinkState linkState
    private Random random

    int BEACON_PERIOD = 100*1000
    int SWEEP_PERIOD = 100*1000
    int DATAGRAM_PERIOD = 10*1000
    int RANDOM_DELAY = 5*1000
    int MAX_RETRIES = 1
    int LINK_EXPIRY_TIME = 60*60
    DatagramPriority DATAGRAM_PRIORITY


    List<Parameter> getParameterList() {
        Class[] paramClasses = new Class[2]
        paramClasses[0] = DtnLinkParameters.class
        paramClasses[1] = DatagramParam.class
        return allOf(paramClasses)
    }

    enum DatagramPriority {
        ARRIVAL, EXPIRY, RANDOM
    }

    enum LinkState {
        READY, WAITING
    }

    DtnLink(String dir) {
        directory = dir
        random = new Random()
        DATAGRAM_PRIORITY = DatagramPriority.EXPIRY
        linkState = LinkState.READY
    }

    DtnLink(String dir, DatagramPriority datagramPriority) {
        directory = dir
        random = new Random()
        DATAGRAM_PRIORITY = datagramPriority
        linkState = LinkState.READY
    }

    int currentTimeSeconds() {
        return (currentTimeMillis()/1000).intValue()
    }

    @Override
    protected void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)
        addCapability(DatagramCapability.RELIABILITY)
    }

    @Override
    protected void startup() {
        nodeInfo = agentForService(Services.NODE_INFO)

        nodeAddress = (get(nodeInfo, NodeInfoParam.address) != null) ? (int)get(nodeInfo, NodeInfoParam.address) : 1
        notify = topic()

        stats = new DtnStats(nodeAddress)
        storage = new DtnStorage(this, directory)
        utility = new DtnLinkInfo(this)

        linksWithReliability = getLinksWithReliability()

        if (!linksWithReliability.size()) {
            println("No reliable links available")
            return
        }

        for (AgentID link : linksWithReliability) {
            utility.addLink(link)
        }

        beaconBehavior = (PoissonBehavior)add(createBeaconBehavior())

        datagramCycle = (CyclicBehavior)add(new CyclicBehavior() {
            @Override
            void action() {
                for (Integer node : utility.getDestinationNodes()) {
                    Set<AgentID> nodeLinks = utility.getLinksForNode(node)
                    if (!nodeLinks.isEmpty()) {
                        // FIXME: later choose links based on bitrate
                        AgentID nodeLink = nodeLinks.getAt(0)
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

        add(new PoissonBehavior(DATAGRAM_PERIOD) {
            @Override
            void onTick() {
                datagramCycle.restart()
            }
        })
    }

    String selectNextDatagram(ArrayList<String> datagrams) {
        switch (DATAGRAM_PRIORITY) {
        case DatagramPriority.ARRIVAL:
            int minArrivalTime = Integer.MAX_VALUE
            String messageID
            // FIXME: why calling gAT twice?
            for (String id : datagrams) {
                int arrivalTime = storage.getArrivalTime(id)
                if (arrivalTime >= 0 && arrivalTime < minArrivalTime) {
                    messageID = id
                    minArrivalTime = storage.getArrivalTime(id)
                }
            }
            return messageID
        case DatagramPriority.EXPIRY:
            int minExpiryTime = Integer.MAX_VALUE
            String messageID
            for (String id : datagrams) {
                DtnPduMetadata metadata = storage.getDatagramMetadata(id)
                if (metadata != null && metadata.expiryTime < minExpiryTime) {
                    messageID = id
                    minExpiryTime = metadata.expiryTime
                }
            }
            return messageID
        case DatagramPriority.RANDOM:
            return (datagrams.size()) ? datagrams.get(random.nextInt(datagrams.size())) : null
        default:
            println("This should never happen!")
            return null
        }
    }

    ArrayList<AgentID> getLinksWithReliability() {
        AgentID[] links = agentsForService(Services.LINK)
        ArrayList<AgentID> reliableLinks = new ArrayList<>()
        for (AgentID link : links) {
            CapabilityReq req = new CapabilityReq(link, DatagramCapability.RELIABILITY)
            Message rsp = request(req, 2000)
            if (rsp != null && rsp.getPerformative() == Performative.CONFIRM &&
                (int)get(link, DatagramParam.MTU) > HEADER_SIZE &&
                link.getName() != getName()) { // we don't want to use the DtnLink!
                println("Candidate Link " + link.getName())
                reliableLinks.add(link)
            }
        }
        return reliableLinks
    }

    @Override
    protected Message processRequest(Message msg) {
        if (msg instanceof DatagramReq) {
            // FIXME: check for buffer space too, probably in saveDatagram!
            if (!storage.saveDatagram(msg) || msg.getTtl().isNaN()) {
                println("Invalid Datagram!")
                return new Message(msg, Performative.REFUSE)
            } else {
                int x = currentTimeSeconds()
                println(x)
                stats.datagrams_requested++
                return new Message(msg, Performative.AGREE)
            }
        }
        return null
    }

    @Override
    protected void processMessage(Message msg) {
         if (msg instanceof RxFrameNtf) {
             stats.beacons_snooped++
             AgentID phy = msg.getRecipient().getOwner().getAgentID()
             AgentID link = utility.getLinkForPhy(phy)
             if (link != null) {
                 utility.addLinkForNode(msg.getFrom(), link)
             }
         } else if (msg instanceof DatagramNtf) {
             // we will do this for every message? Can't hurt much
             AgentID link = utility.getLinkForTopic(msg.getRecipient())
             utility.addLinkForNode(msg.getFrom(), link)
             utility.updateLastTransmission(link)

             if (msg.getProtocol() == DTN_PROTOCOL) {
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
            } else {
                stats.datagrams_received++
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
            linkState = LinkState.WAITING
            add(new WakerBehavior(random.nextInt(RANDOM_DELAY)) {
                @Override
                void onWake() {
                    byte[] pduBytes = storage.getPDU(messageID, true).toByteArray()
                    if (pduBytes != null) {
                        DtnPduMetadata metadata = storage.getMetadata(messageID)
                        if (metadata != null && !metadata.delivered) {
                            if (metadata.attempts > 0) {
                                stats.datagrams_resent++
                                println("Resending datagram: " + messageID + " attempt " + storage.getMetadata(messageID).attempts)
                            }
                            // check for protocol number here?
                            // we are decoding the PDU twice, not good!
                            Tuple parsedPdu = storage.decodePdu(pduBytes)
                            int pduProtocol = (int)parsedPdu.get(1)
                            byte[] pduData = (byte[])parsedPdu.get(2)

                            DatagramReq datagramReq

                            // this is for short-circuiting PDUs
                            if (pduProtocol == Protocol.ROUTING) {
                                datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                                                                data: pduBytes,
                                                                to: node,
                                                                reliability: true)
                            } else {
                                datagramReq = new DatagramReq(protocol: pduProtocol,
                                                                data: pduData,
                                                                to: node,
                                                                reliability: true)
                            }
                            storage.trackDatagram(datagramReq.getMessageID(), messageID)
                            // FIXME: use send or request here?
                            nodeLink.send(datagramReq)
                            stats.datagrams_sent++
                        }
                    } else {
                        linkState = LinkState.READY
                    }
                }
            })
        }
    }

    PoissonBehavior createBeaconBehavior() {
        return new PoissonBehavior(BEACON_PERIOD) {
            @Override
            void onTick() {
                int beaconPeriod = (BEACON_PERIOD / 1000).intValue()
                for (Map.Entry entry : utility.getLinkInfo()) {
                    AgentID linkID = entry.getKey()
                    DtnLinkInfo.LinkMetadata metadata = entry.getValue()
                    int lastTransmission = metadata.lastTransmission
                    if (currentTimeSeconds() - lastTransmission >= beaconPeriod) {
                        linkID.send(new DatagramReq(to: Address.BROADCAST))
                    }
                }
            }
        }
    }

    Object getProperty(AgentID aid, Parameter param) {
        return get(aid, param)
    }

    int getMTU() {
        int minMTU = Integer.MAX_VALUE
        for (DtnLinkInfo.LinkMetadata metadata : utility.getLinkInfo().values()) {
            minMTU = Math.min(metadata.linkMTU, minMTU)
        }
        return minMTU - HEADER_SIZE
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
