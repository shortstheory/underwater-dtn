package test

import dtn.DtnLink
import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestLink extends UnetAgent {
    DtnTest.Tests test

    public boolean SUCCESSFUL_DELIVERY_RESULT = false
    public boolean ROUTER_MESSAGE_RESULT = false

    AgentID dtnlink

    TestLink(DtnTest.Tests t) {
        test = t
    }

    List<Parameter> getParameterList() {
        return allOf(DatagramParam.class)
    }

    void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)
        addCapability(DatagramCapability.RELIABILITY)
    }

    void startup() {
        dtnlink = agent("dtnlink")
        switch(test) {
            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
            case DtnTest.Tests.ROUTER_MESSAGE:
                // first we send a (fake) beacon message to the node
                dtnlink.send(new DatagramNtf(from: DtnTest.DEST_ADDRESS, msgID: "mymsg"))
                break
        }
    }

    Message processRequest(Message msg) {
        switch(test) {
            case DtnTest.Tests.TRIVIAL_MESSAGE:
                if (msg instanceof DatagramReq) {
                    return new Message(msg, Performative.AGREE)
                }
                break
            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                        String messageID = msg.getMessageID()
                        add(new WakerBehavior(10*1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                                                     inReplyTo: messageID))
                            }
                        })
                        if (msg.getTo() == DtnTest.DEST_ADDRESS && msg.getData() == DtnTest.MESSAGE_DATA.getBytes()) {
                            SUCCESSFUL_DELIVERY_RESULT = true
                        }
                    }
                    return new Message(msg, Performative.AGREE)
                }
                break
            case DtnTest.Tests.ROUTER_MESSAGE:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnLink.DTN_PROTOCOL) {
                        String messageID = msg.getMessageID()
                        add(new WakerBehavior(10*1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                                                     inReplyTo: messageID))
                            }
                        })
                        Tuple pduInfo = decodePdu(msg.getData())
                        if ((int)pduInfo.get(0) > 0
                            && (int)pduInfo.get(0) < DtnTest.MESSAGE_TTL
                            && pduInfo.get(1) == Protocol.ROUTING
                            && pduInfo.get(2) == DtnTest.MESSAGE_DATA.getBytes()) {
                            println(pduInfo.get(0))
                            ROUTER_MESSAGE_RESULT = true
                        }
                    }
                    return new Message(msg, Performative.AGREE)
                }
                break
        }
        return null
    }

    int getMTU() {
        return 1600
    }

    Tuple decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)

        int ttl = (int)pdu.read32()
        int protocol = (int)pdu.read8()
        // the data follows the 8 byte header
        byte[] data = Arrays.copyOfRange(pduBytes, DtnLink.HEADER_SIZE, pduBytes.length)
        return new Tuple(ttl, protocol, data)
    }
}
