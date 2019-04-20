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
    public static final int MAX_PAYLOADS = 256

    enum DatagramPriority {
        ARRIVAL, EXPIRY, RANDOM
    }

    private DtnStorage storage
    public int nodeAddress
    private String directory
    private int destinationNodeIndex

    private HashMap<Integer, Boolean> alternatingBitMap
    private HashMap<Integer, Integer> lastDatagramHash // this won't work if RANDOM is set

    private AgentID notify
    private AgentID nodeInfo

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
    int beaconTimeout       // timeout before sending a Beacon on an idle link
    int resetStateTime      // timeout for waiting for the DDN/DFN on a link
    int GCPeriod            // time period for deleting expired messages on non-volatile storage
    int datagramResetPeriod // time period for sending a pending datagram
    int randomDelay         // delays sending a datagram from [0, randomDelay] ms to avoid collisions


    /**
     * Parameter with unit in seconds
     */
    int linkExpiryTime

    DatagramPriority datagramPriority
    ArrayList<AgentID> linkPriority

    /**
     *  This option makes DtnLink send messages without PDU headers to save header space
     *  However, doing so might result in duplicates of datagrams reaching the destination
     */
    boolean shortCircuit

    enum LinkState {
        READY, WAITING
    }

    private DtnLink() {
        random = new Random()
        linkPriority = new ArrayList<>()
        alternatingBitMap = new HashMap<>()
        lastDatagramHash = new HashMap<>()

        beaconTimeout        = 100*1000
        resetStateTime       = 300*1000
        GCPeriod             = 100*1000
        datagramResetPeriod  = 10*1000
        randomDelay          = 5*1000
        linkExpiryTime       = 3*3600

        linkState = LinkState.READY
        shortCircuit = false
        destinationNodeIndex = 0
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
        storage.recoverMetadataMap()

        linkManager = new DtnLinkManager(this)

        ArrayList<AgentID> linksWithReliability = getLinksWithReliability()

        if (!linksWithReliability.size()) {
            log.info("No reliable links available")
            return
        }

        for (AgentID link : linksWithReliability) {
            linkPriority.add(link)
            linkManager.addLink(link)
        }

        // Initialise behaviors
        beaconBehavior = addBeaconBehavior()
        GCBehavior = addGCBehavior()
        datagramResetBehavior = addDatagramBehavior()
    }

    void restartSending() {
        add(new WakerBehavior(random.nextInt(randomDelay)) {
            @Override
            void onWake() {
                if (linkState == LinkState.READY && linkManager.getDestinationNodes().size()) {
                    destinationNodeIndex = (destinationNodeIndex + 1) % linkManager.getDestinationNodes().size()
                    int node = linkManager.getDestinationNodes().get(destinationNodeIndex)
                    AgentID nodeLink = linkManager.getBestLink(node)
                    if (nodeLink != null) {
                        // FIXME: later choose links based on bitrate
                        ArrayList<String> datagrams = storage.getNextHopDatagrams(node)
                        String messageID = selectNextDatagram(datagrams)
                        if (alternatingBitMap.get(node) == null) {
                            alternatingBitMap.put(node, false)
                        }
                        if (messageID != null && sendDatagram(messageID, node, nodeLink)) {
                            linkState = LinkState.WAITING
                        } else {
                            linkState = LinkState.READY
                        }
                    }
                }
            }
        })
    }

    void sendFailureNtf(String messageID, int nextHop) {
        notify.send(new DatagramFailureNtf(inReplyTo: messageID,
                                            to: nextHop))
        log.info("Datagram - " + messageID + " has expired")
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
                log.warning("This should never happen!")
                return null
        }
    }

    @Override
    protected Message processRequest(Message msg) {
        if (msg instanceof DatagramReq) {
            if (msg.getTtl().isNaN() || msg.getTo() <= 0 || !storage.saveDatagram(msg)) {
                log.fine("Invalid Datagram!")
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
                linkManager.addNodeLink(msg.getFrom(), link)
                linkManager.updateLastReception(link)
            }
        } else if (msg instanceof DatagramNtf) {
            // Update the time of last message transmission when we receive a new DatagramNtf
            // We don't care which protocol number it has over here
            AgentID link = linkManager.getLink(msg.getSender())
            int src = msg.getFrom()
            linkManager.addNodeLink(src, link)
            linkManager.updateLastReception(link)

            if (msg.getProtocol() == DTN_PROTOCOL) {
                byte[] pduBytes = msg.getData()
                HashMap<String, Integer> map = DtnStorage.decodePdu(pduBytes)
                byte[] data = Arrays.copyOfRange(pduBytes, HEADER_SIZE, pduBytes.length)
                if (map != null) {
                    int ttl = map.get(DtnStorage.TTL_MAP)
                    int protocol = map.get(DtnStorage.PROTOCOL_MAP)
                    boolean tbc = (map.get(DtnStorage.TBC_BIT_MAP)) ? true : false
                    boolean altBit = map.get(DtnStorage.ALT_BIT_MAP) ? true : false
                    int payloadID = map.get(DtnStorage.PAYLOAD_ID_MAP)
                    int startPtr = map.get(DtnStorage.START_PTR_MAP)

                    // If the hash is the same as the previous, it's an unecessary Re-Tx and we can ignore it
                    int hashCode = Arrays.hashCode(data)
                    hashCode = (altBit) ? (hashCode ^ 0xFFFFFFFF).toInteger() : hashCode

                    if (hashCode != lastDatagramHash.get(src)) {
                        // Only fragments have non-zero payloadIDs
                        lastDatagramHash.put(src, hashCode)
                        if (isPayload(map)) {
                            storage.saveFragment(src, payloadID, protocol, startPtr, ttl, data)
                            if (!tbc) {
                                log.fine("Received Payload " + payloadID)
                                byte[] msgBytes = storage.readPayload(src, payloadID)
                                notify.send(new DatagramNtf(protocol: protocol, from: msg.getFrom(), to: msg.getTo(), data: msgBytes, ttl: ttl))
                                String messageID = Integer.valueOf(src) + "_" + Integer.valueOf(payloadID)
                                storage.removeDatagram(messageID)
                            }
                        } else {
                            // If it doesn't have a PayloadID sent, we can just
                            // broadcast it on our topic for anyone who's listening
                            // Messages which are short circuited skip all this entirely
                            // and go straight to the agent they need to
                            notify.send(new DatagramNtf(protocol: protocol, from: msg.getFrom(), to: msg.getTo(), data: data, ttl: ttl))
                        }
                    } else {
                        log.info("Seen hash#" + msg.getData().hashCode() + " before!")
                    }
                }
            }
        // once we have received a DDN/DFN, we can send the next DatagramReq
        } else if (msg instanceof DatagramDeliveryNtf) {
            int node = msg.getTo()
            String newMessageID = msg.getInReplyTo()
            if (newMessageID == outboundDatagramID) {
                alternatingBitMap.put(node, !alternatingBitMap.get(node)) // toggle the alt-bit every successful message
                DtnPduMetadata metadata = storage.getMetadata(originalDatagramID)
                if (metadata != null) {
                    DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalDatagramID, to: node)
                    notify.send(deliveryNtf)
                    storage.removeDatagram(originalDatagramID)
                }
            } else if (newMessageID == outboundPayloadFragmentID) {
                DtnPduMetadata metadata = storage.getMetadata(originalPayloadID)
                if (metadata != null) {
                    metadata.bytesSent = outboundPayloadBytesSent
                    log.fine("Datagram: " + originalPayloadID + " Bytes Sent " + metadata.bytesSent)
                    if (metadata.bytesSent == metadata.size) {
                        DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalPayloadID, to: node)
                        notify.send(deliveryNtf)
                        storage.removeDatagram(originalPayloadID)
                    }
                }
            } else {
                log.warning("This should never happen! " + newMessageID)
            }
            prepareLink()
        } else if (msg instanceof DatagramFailureNtf) {
            String newMessageID = msg.getInReplyTo()
            linkManager.removeNodeLink(msg.getTo(), msg.getSender())
            if (newMessageID == outboundDatagramID) {
                log.fine("DFN for " + originalDatagramID)
            } else if (newMessageID == outboundPayloadFragmentID) {
                log.fine("DFN for " + originalPayloadID)
            } else {
                log.warning("This should never happen! " + newMessageID)
            }
//            linkManager.removeNodeLink()
            prepareLink()
        }
    }

    /**
     * Resets the state of DtnLink so we can send the next pending DatagramReq
     */
    void prepareLink() {
        resetState.stop()
        linkState = LinkState.READY
        restartSending()
    }

    boolean isPayload(HashMap<String, Integer> map) {
        if (map.get(DtnStorage.START_PTR_MAP) || map.get(DtnStorage.TBC_BIT_MAP)) {
            return true
        }
        return false
    }

    boolean sendDatagram(String messageID, int dest, AgentID nodeLink) {
        HashMap<String, Integer> parsedPdu = storage.getPDUInfo(messageID)
        DtnPduMetadata metadata
        int ttl
        if (parsedPdu != null
                && ((metadata = storage.getMetadata(messageID)) != null)
                && (ttl = metadata.expiryTime - currentTimeSeconds()) > 0) {
            // we are reading the file twice, not good!
            int linkMTU = linkManager.getLinkMetadata(nodeLink).linkMTU
            int pduProtocol = parsedPdu.get(DtnStorage.PROTOCOL_MAP)
            boolean alternatingBit = alternatingBitMap.get(dest)
            byte[] pduData = storage.getMessageData(messageID)
            DatagramReq datagramReq
            if (pduData.length + HEADER_SIZE <= linkMTU) {
                // Short circuit datagrams straight to the appropriate agent
                if (shortCircuit && pduProtocol != Protocol.ROUTING) {
                    datagramReq = new DatagramReq(protocol: pduProtocol,
                            data: pduData,
                            to: dest,
                            reliability: true)
                } else {
                    byte[] pduBytes = DtnStorage.encodePdu(pduData,
                            ttl,
                            pduProtocol,
                            alternatingBit,
                            false,
                            0,
                            0)
                            .toByteArray()
                    datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                            data: pduBytes,
                            to: dest,
                            reliability: true)
                }
                Message rsp = nodeLink.request(datagramReq, 1000)
                if (rsp.getPerformative() == Performative.AGREE && rsp.getInReplyTo() == datagramReq.getMessageID()) {
                    originalDatagramID = messageID
                    outboundDatagramID = datagramReq.getMessageID()
                    resetState = addResetStateBehavior(messageID)
                    return true
                }
            } else {
                int startPtr = metadata.bytesSent
                int endPtr = Math.min(startPtr + (linkMTU - HEADER_SIZE), pduData.length)
                boolean tbc = !(endPtr == pduData.length)
                byte[] data = Arrays.copyOfRange(pduData, startPtr, endPtr)
                int payloadID = storage.getPayloadID(messageID)
                byte[] pduBytes = DtnStorage.encodePdu(data,
                        ttl,
                        parsedPdu.get(DtnStorage.PROTOCOL_MAP),
                        alternatingBit,
                        tbc,
                        payloadID,
                        startPtr)
                        .toByteArray()
                String trackerID = Integer.toString(payloadID) + "_" + endPtr
                datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                        data: pduBytes,
                        to: dest,
                        reliability: true)
                Message rsp = nodeLink.request(datagramReq, 1000)
                if (rsp.getPerformative() == Performative.AGREE && rsp.getInReplyTo() == datagramReq.getMessageID()) {
                    originalPayloadID = messageID
                    outboundPayloadFragmentID = datagramReq.getMessageID()
                    outboundPayloadBytesSent = endPtr
                    resetState = addResetStateBehavior(trackerID)
                    return true
                }
            }
        }
        return false
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
                log.fine("Candidate Link " + link.getName())
                reliableLinks.add(link)
            }
        }
        return reliableLinks
    }

    PoissonBehavior addBeaconBehavior() {
        return (PoissonBehavior)add(new PoissonBehavior(beaconTimeout) {
            @Override
            void onTick() {
                int beaconPeriod = (beaconTimeout / 1000).intValue()
                for (Map.Entry entry : linkManager.getLinkInfo()) {
                    AgentID linkID = (AgentID)entry.getKey()
                    DtnLinkManager.LinkMetadata linkMetadata = (DtnLinkManager.LinkMetadata)entry.getValue()
                    int lastTransmission = linkMetadata.lastTransmission
                    // no need to send a Beacon if we've sent one on that link within beaconPeriod
                    if (currentTimeSeconds() - lastTransmission >= beaconPeriod) {
                        linkID.send(new DatagramReq(to: Address.BROADCAST))
                        linkManager.updateLastTransmission(linkID)
                    }
                }
            }
        })
    }

    TickerBehavior addGCBehavior() {
        return (TickerBehavior)add(new TickerBehavior(GCPeriod) {
            @Override
            void onTick() {
                storage.deleteFiles()
            }
        })
    }

    PoissonBehavior addDatagramBehavior() {
        return (PoissonBehavior)add(new PoissonBehavior(datagramResetPeriod) {
            @Override
            void onTick() {
                if (linkState == LinkState.READY) {
                    restartSending()
                }
            }
        })
    }

    WakerBehavior addResetStateBehavior(String ID) {
        return (WakerBehavior)add(new WakerBehavior(resetStateTime) {
            @Override
            void onWake() {
                log.info("No DDN/DFN received, resetting DtnLink for " + ID)
                linkState = LinkState.READY
            }
        })
    }

    Object getProperty(AgentID aid, Parameter param) {
        return get(aid, param)
    }

    int getMTU() {
        // (4194303-8) = 4194295
        return 0x3FFFFF - HEADER_SIZE
    }

    void setBeaconTimeout(int period) {
        beaconTimeout = period
        beaconBehavior.stop()
        if (beaconTimeout == 0) {
            log.info("Stopped beacon")
        } else {
            log.info("Changed beacon interval to " + period)
            beaconBehavior = addBeaconBehavior()
        }
    }

    void setGCPeriod(int period) {
        GCPeriod = period
        GCBehavior.stop()
        if (GCPeriod == 0) {
            log.info("Stopped GC")
        } else {
            log.info("Changed GC interval to " + period)
            GCBehavior = addGCBehavior()
        }
    }

    void setDatagramResetPeriod(int period) {
        datagramResetPeriod = period
        datagramResetBehavior.stop()
        if (datagramResetPeriod == 0) {
            log.info("Stopped Datagram Reset Period")
        } else {
            log.info("Changed Reset Interval to " + period)
            datagramResetBehavior = addDatagramBehavior()
        }
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


    ArrayList<AgentID> getLinkPriority() {
        return linkPriority
    }

    List<Integer> getDiscoveredNodes() {
        return linkManager.getDestinationNodes()
    }

    List<Parameter> getParameterList() {
        Class[] paramClasses = new Class[2]
        paramClasses[0] = DtnLinkParameters.class
        paramClasses[1] = DatagramParam.class
        return allOf(paramClasses)
    }
}
