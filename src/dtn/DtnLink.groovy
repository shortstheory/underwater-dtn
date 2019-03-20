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
    public static final int HEADER_SIZE  = 8
    public static final int DTN_PROTOCOL = 50

    int MTU

    /**
     * Collects statistics for the simulation
     */
    public DtnStats stats

    /**
     * Manages the storage of pending segmentSet
     */
    private DtnStorage storage
    public int nodeAddress
    private String directory

    private ArrayList<AgentID> linksWithReliability
    private AgentID notify
    private AgentID nodeInfo

    private CyclicBehavior datagramCycle
    private PoissonBehavior beaconBehavior

    private DtnLinkInfo utility
    private LinkState linkState

    public Random random

    // all units are in milliseconds below
    int BEACON_PERIOD = 10*1000
    int SWEEP_PERIOD = 100*1000
    int DATAGRAM_PERIOD = 10*1000
    int RANDOM_DELAY = 5*1000
    int MAX_RETRIES = 5
    // uses seconds
    int LINK_EXPIRY_TIME = 10*6000
    DatagramPriority DATAGRAM_PRIORITY
    ArrayList<AgentID> LINK_PRIORITY

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

    private DtnLink() {
        random = new Random()
        LINK_PRIORITY = new ArrayList<>()
        linkState = LinkState.READY
    }

    DtnLink(String dir) {
        this()
        DATAGRAM_PRIORITY = DatagramPriority.EXPIRY
        directory = dir
    }

    DtnLink(String dir, DatagramPriority datagramPriority) {
        this()
        DATAGRAM_PRIORITY = datagramPriority
        directory = dir
    }

    int currentTimeSeconds() {
        return (currentTimeMillis()/1000).intValue()
    }

    @Override
    protected void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)
        addCapability(DatagramCapability.RELIABILITY)
        addCapability(DatagramCapability.FRAGMENTATION)
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
            LINK_PRIORITY.add(link)
            utility.addLink(link)
        }

        beaconBehavior = (PoissonBehavior)add(createBeaconBehavior())

        datagramCycle = (CyclicBehavior)add(new CyclicBehavior() {
            @Override
            void action() {
                for (Integer node : utility.getDestinationNodes()) {
                    if (node) {
                        AgentID nodeLink = utility.getBestLink(node)
                        if (nodeLink != null) {
                            // FIXME: later choose links based on bitrate
                            ArrayList<String> datagrams = storage.getNextHopDatagrams(node)
                            String messageID = selectNextDatagram(datagrams)
                            if (messageID != null) {
                                sendDatagram(messageID, node, nodeLink)
                            }
                        }
                    }
                }
                block()
            }
        })

        add(new TickerBehavior(SWEEP_PERIOD) {
            @Override
            void onTick() {
                storage.deleteFiles()
            }
        })

        add(new PoissonBehavior(DATAGRAM_PERIOD) {
            @Override
            void onTick() {
                datagramCycle.restart()
            }
        })
    }

    void sendFailureNtf(String messageID, int nextHop) {
        notify.send(new DatagramFailureNtf(inReplyTo: messageID,
                                            to: nextHop))
        println "Datagram - " + messageID + " has expired"
        stats.datagrams_expired++
    }

    // If we're sending a payload, ARRIVAL & EXPIRY will always choose it again
    // But RANDOM could be well, random
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
                        minArrivalTime = arrivalTime
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
            if (msg.getTtl().isNaN() || !storage.saveDatagram(msg)) {
                println("Invalid Datagram!")
                return new Message(msg, Performative.REFUSE)
            } else {
                int x = currentTimeSeconds()
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
            AgentID link = utility.getLink(msg.getSender())
//            AgentID link = utility.getLinkForTopic(msg.getRecipient())
            utility.addLinkForNode(msg.getFrom(), link)
            utility.updateLastTransmission(link)

            if (msg.getProtocol() == DTN_PROTOCOL) {
                byte[] pduBytes = msg.getData()
                HashMap<String, Integer> map = storage.decodePdu(pduBytes)
                byte[] data = storage.getDataFromPDU(pduBytes)
                if (map != null) {
                    int ttl = map.get(DtnStorage.TTL_MAP)
                    int protocol = map.get(DtnStorage.PROTOCOL_MAP)
                    boolean tbc = (map.get(DtnStorage.TBC_BIT_MAP)) ? true : false
                    int payloadID = map.get(DtnStorage.PAYLOAD_ID_MAP)
                    int startPtr = map.get(DtnStorage.START_PTR_MAP)
                    int src = msg.getFrom()
                    if (payloadID) {
                        storage.saveFragment(src, payloadID, protocol, startPtr, ttl, data)
                        if (tbc) {
                            // FIXME: ntf.setTtl(ttl)
                            byte[] msgBytes = storage.getDataFromPDU(storage.readPayload(src, payloadID))
                            notify.send(new DatagramNtf(protocol: protocol, from: msg.getFrom(), to: msg.getTo(), data: msgBytes))
                            String messageID = Integer.valueOf(src) + "_" + Integer.valueOf(payloadID)
                            storage.setDelivered(messageID)
//                            storage.deletePayload(src, payloadID)
                        }
                    } else {
                        // If it doesn't have a PayloadID sent, it probably means its a ROUTING PDU, so we can just
                        // broadcast it on our topic for anyone who's listening (read: ROUTER)

                        // Non DTNL-PDUs skip all this entirely and go straight to the agent they need to
                        // FIXME: ntf.setTtl(ttl)
                        notify.send(new DatagramNtf(protocol: protocol, from: msg.getFrom(), to: msg.getTo(), data: data))
                        stats.datagrams_received++
                    }
                }
            } else {
                stats.datagrams_received++
            }
            // once we have received a DDN/DFN, we can send another one
        } else if (msg instanceof DatagramDeliveryNtf) {
            int node = msg.getTo()
            String[] split = msg.getInReplyTo().split("_")
            String messageID = split[0]
            String originalMessageID = storage.getOriginalMessageID(messageID)
            // for payloads
            if (split.size() == 2) {
                int endPtr = Integer.valueOf(split[1])
                DtnPduMetadata metadata = storage.getMetadata(originalMessageID)
                metadata.bytesSent = endPtr
                if (metadata.bytesSent == metadata.size) {
                    DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalMessageID, to: node)
                    notify.send(deliveryNtf)
                    storage.setDelivered(originalMessageID)
                }
                // for non-payloads
            } else {
                // it can happen that the DDN comes just after a TTL
                if (originalMessageID != null) {
                    int deliveryTime = currentTimeSeconds() - storage.getArrivalTime(originalMessageID)
                    if (deliveryTime >= 0) {
                        stats.delivery_times.add(deliveryTime)
                    }
                    DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalMessageID, to: node)
                    notify.send(deliveryNtf)
                    stats.datagrams_success++
                    storage.setDelivered(originalMessageID)
                }
            }
            linkState = LinkState.READY
            datagramCycle.restart()
        } else if (msg instanceof DatagramFailureNtf) {
            stats.datagrams_failed++
            // FIXME: increment retries of payloads here
            String[] split = msg.getInReplyTo().split("_")
            String messageID = split[0]
            if (split.length == 2) {
                // This means it's a payload ID
                storage.getMetadata(messageID).attempts++
            } else {
                storage.removeFailedEntry(messageID)
            }
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
                    int linkMTU = utility.getLinkMetadata(nodeLink).linkMTU
                    HashMap<String, Integer> parsedPdu = storage.getParsedPDU(messageID)
                    if (parsedPdu != null && parsedPdu.get(DtnStorage.TTL_MAP) > 0) {
                        DtnPduMetadata metadata = storage.getMetadata(messageID)
                        if (metadata != null && !metadata.delivered) {
                            if (metadata.attempts > 0) {
                                stats.datagrams_resent++
                                println("Resending datagram: " + messageID + " attempt " + storage.getMetadata(messageID).attempts)
                            }
                            // check for protocol number here?
                            // we are decoding the PDU twice, not good!
                            int pduProtocol = parsedPdu.get(DtnStorage.PROTOCOL_MAP)
                            byte[] pduData = storage.getPDUData(messageID)
                            int expiryTime = metadata.expiryTime - currentTimeSeconds()
                            metadata.size = pduData.length
                            DatagramReq datagramReq
                            if (pduData.length + HEADER_SIZE <= linkMTU) {
                                // this is for short-circuiting PDUs
                                if (pduProtocol == Protocol.ROUTING) {
                                    byte[] pduBytes = storage.encodePdu(pduData,
                                            expiryTime,
                                            pduProtocol,
                                            true,
                                            0,
                                            0)
                                            .toByteArray()
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
                                // FIXME: use send or request here?
                                storage.trackDatagram(datagramReq.getMessageID(), messageID)
                                stats.datagrams_sent++
                            } else {
                                int startPtr = metadata.bytesSent
                                int endPtr = Math.min(startPtr + (linkMTU - HEADER_SIZE), pduData.length)
                                boolean tbc = (endPtr == pduData.length) ? true : false
                                byte[] data = Arrays.copyOfRange(pduData, startPtr, endPtr)
                                int payloadID = storage.getPayloadID(messageID)
                                byte[] pduBytes = storage.encodePdu(data,
                                                    expiryTime,
                                                    parsedPdu.get(DtnStorage.PROTOCOL_MAP),
                                                    tbc,
                                                    payloadID,
                                                    startPtr)
                                                    .toByteArray()
                                // separator should not conflict with a regular DReq
                                String dreqID = Integer.toString(payloadID) + "_" + endPtr
                                datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                                                data: pduBytes,
                                                to: node,
                                                reliability: true,
                                                messageID: dreqID)
                            }
                            nodeLink.send(datagramReq)
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
//        int minMTU = Integer.MAX_VALUE
//        for (DtnLinkInfo.LinkMetadata metadata : utility.getLinkInfo().values()) {
//            minMTU = Math.min(metadata.linkMTU, minMTU)
//        }
//        return minMTU - HEADER_SIZE
        return 8388607 - HEADER_SIZE
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

    ArrayList<AgentID> getLINK_PRIORITY() {
        return LINK_PRIORITY
    }

    void setLINK_PRIORITY(ArrayList<AgentID> links) {
        LINK_PRIORITY = links
    }

    Set<Integer> getDISCOVERED_NODES() {
        return utility.getDestinationNodes()
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
