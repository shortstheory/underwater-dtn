package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.nodeinfo.NodeInfoParam
import org.arl.unet.phy.RxFrameNtf

/**
 * A UnetAgent for single-copy Disruption Tolerant Networking in UnetStack
 * Stores datagrams and chooses the best Link for sending a datagram to the next hop
 * Supports FRAGMENTATION of large datagrams and RELIABILITY
 *
 * Please see: https://github.com/shortstheory/underwater-dtn/ for more information
 *
 * @author: Arnav Dhamija
 */

@CompileStatic
class DtnLink extends UnetAgent {
    public static final int HEADER_SIZE  = 8
    public static final int DTN_PROTOCOL = 50

    int MTU

    private DtnStorage storage
    public int nodeAddress
    private String directory

    private ArrayList<AgentID> linksWithReliability
    private AgentID notify
    private AgentID nodeInfo

    private CyclicBehavior datagramCycle
    private PoissonBehavior beaconBehavior
    private PoissonBehavior datagramResetBehavior
    private WakerBehavior resetState
    private TickerBehavior GCBehavior

    private DtnLinkManager linkManager
    private LinkState linkState

    private String outboundDatagramID
    private String originalDatagramID

    private String outboundPayloadFragmentID
    private String originalPayloadID
    private int outboundPayloadBytesSent

    public Random random

    /**
     * Parameters with units in milliseconds
     */
    int beaconTimeout = 100*1000        // timeout before sending a Beacon on an idle link
    int resetStateTime = 300*1000       // timeout for waiting for the DDN/DFN on a link
    int GCPeriod = 100*1000             // time period for deleting expired messages on non-volatile storage
    int datagramResetPeriod = 10*1000   // time period for sending a pending datagram
    int randomDelay = 5*1000            // delays sending a datagram from [0, randomDelay] ms to avoid collisions

    /**
     * Parameter with unit in seconds
     */
    int linkExpiryTime = 3*3600        // timeout before a link expires

    enum DatagramPriority {
        ARRIVAL, EXPIRY, RANDOM
    }

    DatagramPriority datagramPriority
    ArrayList<AgentID> linkPriority

    enum LinkState {
        READY, WAITING
    }

    private DtnLink() {
        random = new Random()
        linkPriority = new ArrayList<>()
        linkState = LinkState.READY
    }

    DtnLink(String dir) {
        this()
        datagramPriority = DatagramPriority.EXPIRY
        directory = dir
    }

    DtnLink(String dir, DatagramPriority priority) {
        this()
        datagramPriority = priority
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

        storage = new DtnStorage(this, directory)
        linkManager = new DtnLinkManager(this)

        linksWithReliability = getLinksWithReliability()

        if (!linksWithReliability.size()) {
            println("No reliable links available")
            return
        }

        for (AgentID link : linksWithReliability) {
            linkPriority.add(link)
            linkManager.addLink(link)
        }

        // Initialise behaviors
        beaconBehavior = (PoissonBehavior)add(createBeaconBehavior())
        GCBehavior = (TickerBehavior)add(createGCBehavior())
        datagramResetBehavior = (PoissonBehavior)add(createDatagramBehavior())
        datagramCycle = (CyclicBehavior)add(new CyclicBehavior() {
            @Override
            void action() {
                for (Integer node : linkManager.getDestinationNodes()) {
                    if (node) {
                        AgentID nodeLink = linkManager.getBestLink(node)
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
    }

    void sendFailureNtf(String messageID, int nextHop) {
        notify.send(new DatagramFailureNtf(inReplyTo: messageID,
                                            to: nextHop))
        println "Datagram - " + messageID + " has expired"
    }

    /**
     * Chooses the next datagram to send based on the strategy set in datagramPriority
     */
    String selectNextDatagram(ArrayList<String> datagrams) {
        switch (datagramPriority) {
            case DatagramPriority.ARRIVAL:
                int minArrivalTime = Integer.MAX_VALUE
                String messageID = null
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
                String messageID = null
                for (String id : datagrams) {
                    DtnPduMetadata metadata = storage.getMetadata(id)
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
            if (msg.getTtl().isNaN() || !storage.saveDatagram(msg)) {
                println("Invalid Datagram!")
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
            AgentID phy = msg.getRecipient().getOwner().getAgentID()
            AgentID link = linkManager.getLinkForPhy(phy)
            if (link != null) {
                linkManager.addLinkForNode(msg.getFrom(), link)
            }
        } else if (msg instanceof DatagramNtf) {
            // Update the time of last message transmission when we receive a new DatagramNtf
            AgentID link = linkManager.getLink(msg.getSender())
            linkManager.addLinkForNode(msg.getFrom(), link)
            linkManager.updateLastTransmission(link)

            // Receiving a DTN_PROTOCOL DatagramNtf could either be for the Router or a fragment
            if (msg.getProtocol() == DTN_PROTOCOL) {
                byte[] pduBytes = msg.getData()
                HashMap<String, Integer> map = DtnStorage.decodePdu(pduBytes)
                byte[] data = storage.getPDUData(pduBytes)
                if (map != null) {
                    int ttl = map.get(DtnStorage.TTL_MAP)
                    int protocol = map.get(DtnStorage.PROTOCOL_MAP)
                    boolean tbc = (map.get(DtnStorage.TBC_BIT_MAP)) ? true : false
                    int payloadID = map.get(DtnStorage.PAYLOAD_ID_MAP)
                    int startPtr = map.get(DtnStorage.START_PTR_MAP)
                    int src = msg.getFrom()
                    // Only fragments have non-zero payloadIDs
                    if (payloadID) {
                        storage.saveFragment(src, payloadID, protocol, startPtr, ttl, data)
                        if (tbc) {
                            byte[] msgBytes = storage.getPDUData(storage.readPayload(src, payloadID))
                            notify.send(new DatagramNtf(protocol: protocol, from: msg.getFrom(), to: msg.getTo(), data: msgBytes, ttl: ttl))
                            String messageID = Integer.valueOf(src) + "_" + Integer.valueOf(payloadID)
                            storage.getMetadata(messageID).setDelivered()
                        }
                    } else {
                        // If it doesn't have a PayloadID sent, it probably means its a ROUTING PDU, so we can just
                        // broadcast it on our topic for anyone who's listening (read: ROUTER)
                        // Non DTNL-PDUs skip all this entirely and go straight to the agent they need to
                        notify.send(new DatagramNtf(protocol: protocol, from: msg.getFrom(), to: msg.getTo(), data: data, ttl: ttl))
                    }
                }
            }
        // once we have received a DDN/DFN, we can send another one
        } else if (msg instanceof DatagramDeliveryNtf) {
            int node = msg.getTo()
            String newMessageID = msg.getInReplyTo()
            if (newMessageID == outboundDatagramID) {
                DtnPduMetadata metadata = storage.getMetadata(originalDatagramID)
                // happens when the message has been sent before TTL
                if (metadata != null) {
                    metadata.setDelivered()
                    DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalDatagramID, to: node)
                    notify.send(deliveryNtf)
                }
            } else if (newMessageID == outboundPayloadFragmentID) {
                DtnPduMetadata metadata = storage.getMetadata(originalPayloadID)
                if (metadata != null) {
                    metadata.bytesSent = outboundPayloadBytesSent
                    println("Datagram: " + originalPayloadID + " Bytes Sent " + metadata.bytesSent)
                    if (metadata.bytesSent == metadata.size) {
                        DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalPayloadID, to: node)
                        notify.send(deliveryNtf)
                        metadata.setDelivered()
                    }
                }
            } else {
                println("This should never happen! " + newMessageID)
            }
            resetState.stop()
            linkState = LinkState.READY
            datagramCycle.restart()
        } else if (msg instanceof DatagramFailureNtf) {
            String newMessageID = msg.getInReplyTo()
            if (newMessageID == outboundDatagramID) {
                println("Datagram: " + originalDatagramID + "DFN")
                DtnPduMetadata metadata = storage.getMetadata(originalDatagramID)
                if (metadata != null) {
                    metadata.attempts++
                }
            } else if (newMessageID == outboundPayloadFragmentID) {
                println("Payload: " + originalPayloadID + "DFN")
                DtnPduMetadata metadata = storage.getMetadata(originalPayloadID)
                if (metadata != null) {
                    metadata.attempts++
                }
            } else {
                println("This should never happen! " + newMessageID)
            }
            resetState.stop()
            linkState = LinkState.READY
        }
    }

    void sendDatagram(String messageID, int node, AgentID nodeLink) {
        if (linkState == LinkState.READY) {
            linkState = LinkState.WAITING
            add(new WakerBehavior(random.nextInt(randomDelay)) {
                @Override
                void onWake() {
                    HashMap<String, Integer> parsedPdu = storage.getPDUInfo(messageID)
                    DtnPduMetadata metadata
                    int ttl
                    if (parsedPdu != null
                        && ((metadata = storage.getMetadata(messageID)) != null)
                        && !metadata.delivered
                        && (ttl = metadata.expiryTime - currentTimeSeconds()) > 0) {

                        // we are decoding the PDU twice, not good!
                        int linkMTU = linkManager.getLinkMetadata(nodeLink).linkMTU
                        int pduProtocol = parsedPdu.get(DtnStorage.PROTOCOL_MAP)
                        byte[] pduData = storage.getMessageData(messageID)

                        println("Time Left: " + messageID + " " + (ttl))

                        metadata.size = pduData.length
                        DatagramReq datagramReq

                        if (pduData.length + HEADER_SIZE <= linkMTU) {
                            // this is for short-circuiting PDUs
                            if (pduProtocol == Protocol.ROUTING) {
                                byte[] pduBytes = DtnStorage.encodePdu(pduData,
                                        ttl,
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
                            Message rsp = nodeLink.request(datagramReq, 1000)
                            if (rsp.getPerformative() == Performative.AGREE && rsp.getInReplyTo() == datagramReq.getMessageID()) {
                                originalDatagramID = messageID
                                outboundDatagramID = datagramReq.getMessageID()
                                resetState = (WakerBehavior)add(createResetStateBehavior(messageID))
                            } else {
                                linkState = LinkState.READY
                            }
                        } else {
                            int startPtr = metadata.bytesSent
                            int endPtr = Math.min(startPtr + (linkMTU - HEADER_SIZE), pduData.length)
                            boolean tbc = (endPtr == pduData.length)
                            byte[] data = Arrays.copyOfRange(pduData, startPtr, endPtr)
                            int payloadID = storage.getPayloadID(messageID)
                            byte[] pduBytes = DtnStorage.encodePdu(data,
                                    ttl,
                                    parsedPdu.get(DtnStorage.PROTOCOL_MAP),
                                    tbc,
                                    payloadID,
                                    startPtr)
                                    .toByteArray()
                            // separator should not conflict with a regular DReq
                            String trackerID = Integer.toString(payloadID) + "_" + endPtr
                            datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                                    data: pduBytes,
                                    to: node,
                                    reliability: true)
                            Message rsp = nodeLink.request(datagramReq, 1000)
                            if (rsp.getPerformative() == Performative.AGREE && rsp.getInReplyTo() == datagramReq.getMessageID()) {
                                originalPayloadID = messageID
                                outboundPayloadFragmentID = datagramReq.getMessageID()
                                outboundPayloadBytesSent = endPtr
                                resetState = (WakerBehavior) add(createResetStateBehavior(trackerID))
                            } else {
                                linkState = LinkState.READY
                            }
                        }
                    } else {
                        linkState = LinkState.READY
                    }
                }
            })
        }
    }

    PoissonBehavior createBeaconBehavior() {
        return new PoissonBehavior(beaconTimeout) {
            @Override
            void onTick() {
                int beaconPeriod = (beaconTimeout / 1000).intValue()
                for (Map.Entry entry : linkManager.getLinkInfo()) {
                    AgentID linkID = (AgentID)entry.getKey()
                    DtnLinkManager.LinkMetadata linkMetadata = (DtnLinkManager.LinkMetadata)entry.getValue()
                    int lastTransmission = linkMetadata.lastTransmission
                    if (currentTimeSeconds() - lastTransmission >= beaconPeriod) {
                        linkID.send(new DatagramReq(to: Address.BROADCAST))
                    }
                }
            }
        }
    }

    TickerBehavior createGCBehavior() {
        return new TickerBehavior(GCPeriod) {
            @Override
            void onTick() {
                storage.deleteFiles()
            }
        }
    }

    PoissonBehavior createDatagramBehavior() {
        return new PoissonBehavior(datagramResetPeriod) {
            @Override
            void onTick() {
                datagramCycle.restart()
            }
        }
    }

    WakerBehavior createResetStateBehavior(String ID) {
        return new WakerBehavior(resetStateTime) {
            @Override
            void onWake() {
                println("No DDN/DFN received, resetting DtnLink for " + ID)
                linkState = LinkState.READY
            }
        }
    }

    Object getProperty(AgentID aid, Parameter param) {
        return get(aid, param)
    }

    int getMTU() {
        // (8388607-8) = 8,388,599
        return 0x7FFFFF - HEADER_SIZE
    }

    void setBeaconTimeout(int period) {
        beaconTimeout = period
        beaconBehavior.stop()
        if (beaconTimeout == 0) {
            println("Stopped beacon")
        } else {
            println("Changed beacon interval")
            beaconBehavior = (PoissonBehavior)add(createBeaconBehavior())
        }
    }

    void setGCPeriod(int period) {
        GCPeriod = period
        GCBehavior.stop()
        if (GCPeriod == 0) {
            println("Stopped GC")
        } else {
            println("Changed GC interval")
            GCBehavior = (TickerBehavior)add(createGCBehavior())
        }
    }

    void setDatagramResetPeriod(int period) {
        datagramResetPeriod = period
        datagramResetBehavior.stop()
        if (datagramResetPeriod == 0) {
            println("Stopped Datagram Reset Period")
        } else {
            println("Changed Reset Interval")
            datagramResetBehavior = (PoissonBehavior)add(createDatagramBehavior())
        }
    }

    ArrayList<AgentID> getLinkPriority() {
        return linkPriority
    }

    void setLinkPriority(ArrayList<AgentID> links) {
        // FIXME: what if a Link doesn't exist here? Should it be best-effort, or ignore entirely?
        if (links != null && links.size()) {
            for (AgentID link : links) {
                if (!linkManager.linkExists(link)) {
                    return
                }
            }
            linkPriority = links
        }
    }

    Set<Integer> getDiscoveredNodes() {
        return linkManager.getDestinationNodes()
    }

    List<Parameter> getParameterList() {
        Class[] paramClasses = new Class[2]
        paramClasses[0] = DtnLinkParameters.class
        paramClasses[1] = DatagramParam.class
        return allOf(paramClasses)
    }
}
