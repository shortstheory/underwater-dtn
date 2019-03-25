package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.nodeinfo.NodeInfoParam
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
    int beaconPeriod = 10*1000
    int sweepPeriod = 100*1000
    int datagramPeriod = 10*1000
    int RANDOM_DELAY = 5*1000

    // uses seconds
    int linkExpiryTime = 10*6000

    DatagramPriority datagramPriority
    ArrayList<AgentID> linkPriority

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
        linkPriority = new ArrayList<>()
        linkState = LinkState.READY
    }

    DtnLink(String dir) {
        this()
        datagramPriority = DatagramPriority.EXPIRY
        directory = dir
    }

    DtnLink(String dir, DatagramPriority datagramPriority) {
        this()
        this.datagramPriority = datagramPriority
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
        utility = new DtnLinkInfo(this)

        linksWithReliability = getLinksWithReliability()

        if (!linksWithReliability.size()) {
            println("No reliable links available")
            return
        }

        for (AgentID link : linksWithReliability) {
            linkPriority.add(link)
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

        add(new TickerBehavior(sweepPeriod) {
            @Override
            void onTick() {
                storage.deleteFiles()
            }
        })

        add(new PoissonBehavior(datagramPeriod) {
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
    }

    // If we're sending a payload, ARRIVAL & EXPIRY will always choose it again
    // But RANDOM could be well, random
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
            AgentID link = utility.getLinkForPhy(phy)
            if (link != null) {
                utility.addLinkForNode(msg.getFrom(), link)
            }
        } else if (msg instanceof DatagramNtf) {
            // we will do this for every message? Can't hurt much
            AgentID link = utility.getLink(msg.getSender())
            utility.addLinkForNode(msg.getFrom(), link)
            utility.updateLastTransmission(link)

            if (msg.getProtocol() == DTN_PROTOCOL) {
                byte[] pduBytes = msg.getData()
                HashMap<String, Integer> map = DtnStorage.decodePdu(pduBytes)
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
                    }
                }
            }
            // once we have received a DDN/DFN, we can send another one
        } else if (msg instanceof DatagramDeliveryNtf) {
            int node = msg.getTo()
            String newMessageID = msg.getInReplyTo()
            String originalMessageID = storage.getOriginalMessageID(newMessageID)
            String[] split = originalMessageID.split("_")
            // for payloads
            if (split.size() == 2) {
                String payloadID = split[0]
                int endPtr = Integer.valueOf(split[1])
                String datagramID = storage.getOriginalMessageID(payloadID)
                DtnPduMetadata metadata = storage.getMetadata(datagramID)
                metadata.bytesSent = endPtr
                println("Datagram: " + datagramID + " Bytes Sent " + metadata.bytesSent)
                if (metadata.bytesSent == metadata.size) {
                    DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalMessageID, to: node)
                    notify.send(deliveryNtf)
                    storage.setDelivered(originalMessageID)
                }
                // for non-payloads
            } else {
                // it can happen that the DDN comes just after a TTL
                if (originalMessageID != null) {
                    DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalMessageID, to: node)
                    notify.send(deliveryNtf)
                    storage.setDelivered(originalMessageID)
                }
            }
            linkState = LinkState.READY
            datagramCycle.restart()
        } else if (msg instanceof DatagramFailureNtf) {
            // FIXME: this is only for debugging, we don't really need this anymore
            String newMessageID = msg.getInReplyTo()
            String originalMessageID = storage.getOriginalMessageID(newMessageID)
            String[] split = originalMessageID.split("_")

            println("Datagram: " + split[0] + "DFN")

            if (split.length == 2) {
                // This means it's a payload ID
                String payloadID = split[0]
                String datagramID = storage.getOriginalMessageID(payloadID)
                storage.getMetadata(datagramID).attempts++
            } else {
                String messageID = split[0]
                storage.removeTracking(messageID)
                storage.getMetadata(messageID).attempts++
            }
            linkState = LinkState.READY
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
                                    byte[] pduBytes = DtnStorage.encodePdu(pduData,
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
                                Message rsp = nodeLink.request(datagramReq, 1000)
                                if (rsp.getPerformative() == Performative.AGREE && rsp.getInReplyTo() == datagramReq.getMessageID()) {
                                    storage.trackDatagram(datagramReq.getMessageID(), messageID)
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
                                                    expiryTime,
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
                                    storage.trackDatagram(datagramReq.getMessageID(), trackerID)
                                } else {
                                    linkState = LinkState.READY
                                }
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
        return new PoissonBehavior(beaconPeriod) {
            @Override
            void onTick() {
                int beaconPeriod = (beaconPeriod / 1000).intValue()
                for (Map.Entry entry : utility.getLinkInfo()) {
                    AgentID linkID = (AgentID)entry.getKey()
                    DtnLinkInfo.LinkMetadata metadata = (DtnLinkInfo.LinkMetadata)entry.getValue()
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
        // FIXME: check if this MTU value is correct
        return 0x7FFFFF - HEADER_SIZE
    }

    void setBeaconPeriod(int period) {
        beaconPeriod = period
        beaconBehavior.stop()
        if (beaconPeriod == 0) {
            println "Stopped beacon"
        } else {
            println "Changed beacon interval"
            beaconBehavior = (PoissonBehavior)add(createBeaconBehavior())
        }
    }

    ArrayList<AgentID> getLinkPriority() {
        return linkPriority
    }

    void setLinkPriority(ArrayList<AgentID> links) {
        linkPriority = links
    }

    Set<Integer> getDiscoveredNodes() {
        return utility.getDestinationNodes()
    }
}
