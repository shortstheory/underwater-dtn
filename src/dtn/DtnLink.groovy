package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.AgentID
import org.arl.fjage.Message
import org.arl.fjage.OneShotBehavior
import org.arl.fjage.Performative
import org.arl.fjage.TickerBehavior
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

import javax.jws.Oneway

//@TypeChecked
@CompileStatic
class DtnLink extends UnetAgent {
    private final int HEADER_SIZE = 12
    private final int DTN_PROTOCOL = 99

    private DtnStorage storage
    private int nodeAddress
    private long lastReceivedTime = 0

    private AgentID link
    private AgentID notify
    private AgentID nodeInfo
    private AgentID phy

    int BEACON_DURATION = 100*1000
    int SWEEP_DURATION = 100*1000

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
        nodeAddress = (int)get(nodeInfo, NodeInfoParam.address)
        notify = topic()

        storage = new DtnStorage(Integer.toString(nodeAddress))

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

        add(new TickerBehavior(BEACON_DURATION) {
            @Override
            void onTick() {
                super.onTick()
                if (System.currentTimeSeconds() - lastReceivedTime >= BEACON_DURATION) {
                    link.send(new DatagramReq(to: Address.BROADCAST))
                    lastReceivedTime = System.currentTimeSeconds()
                }
            }
        })

        add(new TickerBehavior(SWEEP_DURATION) {
            @Override
            void onTick() {
                super.onTick()
                ArrayList<Tuple2> expiredDatagrams = storage.deleteExpiredDatagrams()
                // now send DFNs for all of these
                for (Tuple2 expiredDatagram : expiredDatagrams) {
                    notify.send(new DatagramFailureNtf(inReplyTo: (String)expiredDatagram.getFirst(),
                                                        to: (int)expiredDatagram.getSecond()))
                }
            }
        })
    }

    AgentID getLinkWithReliability() {
        AgentID[] links = agentsForService(Services.LINK)

        for (AgentID link : links) {
            CapabilityReq req = new CapabilityReq(link, DatagramCapability.RELIABILITY)
            Message rsp = request(req, 500)
            if (rsp.getPerformative() == Performative.CONFIRM &&
                (int)get(link, DatagramParam.MTU) > HEADER_SIZE &&
                link != getAgentID()) {
                return link
            }
        }
        return null
    }

    @Override
    protected Message processRequest(Message msg) {
        if (msg instanceof DatagramReq) {
            // FIXME: check for buffer space too, probably in saveDatagram!
            if (msg.getTtl() == Float.NaN || !storage.saveDatagram(msg)) {
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
            int node = msg.getFrom()
            add(new OneShotBehavior() {
                @Override
                void action() {
                    super.action()
                    ArrayList<String> datagrams = storage.getNextHopDatagrams(node)
                    for (String messageID : datagrams) {
                        sendDatagram(messageID)
                    }
                }
            })
        } else if (msg instanceof DatagramNtf) {
            if (msg.getProtocol() == DTN_PROTOCOL) {
                // FIXME: check for buffer space, or abstract it
                byte[] pduBytes = msg.getData()
                Tuple pduTuple = storage.decodePdu(pduBytes)

                int ttl = (int)pduTuple.get(0)
                int protocol = (int)pduTuple.get(1)
                byte[] data = (byte[])pduTuple.get(2)

                DatagramNtf ntf = new DatagramNtf()
                ntf.setProtocol(protocol)
                ntf.setData(data)
                // FIXME: ntf.setTtl(ttl)
                notify.send(ntf)
            }
            // we don't need to handle other protocols
        } else if (msg instanceof DatagramDeliveryNtf) {
            int node = msg.to
            String messageID = msg.inReplyTo
            String originalMessageID = storage.getOriginalMessageID(messageID)

            DatagramDeliveryNtf deliveryNtf = new DatagramDeliveryNtf(inReplyTo: originalMessageID, to: node)
            notify.send(deliveryNtf)
        } else if (msg instanceof DatagramFailureNtf) {
            // we don't need to do anything for failure
        }
    }

    void sendDatagram(String messageID) {
        byte[] pdu = storage.getPDU(messageID)
        if (pdu != null) {
            DatagramReq datagramReq = new DatagramReq(protocol: DTN_PROTOCOL,
                                                      data: pdu)
            storage.trackDatagram(datagramReq.getMessageID(), messageID)
            link.send(datagramReq)
        }
    }

    int getMTU() {
        if (link != null) {
            return (int)get(link, DatagramParam.MTU) - HEADER_SIZE
        }
        return 0
    }
}
