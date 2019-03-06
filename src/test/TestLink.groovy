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
    public boolean MAX_RETRY_RESULT = false
    public boolean ARRIVAL_PRIORITY_RESULT = false // set to false if OoO
    public boolean EXPIRY_PRIORITY_RESULT = false
    public boolean RANDOM_PRIORITY_RESULT = false
    public boolean TIMEOUT_D1_SUCCESS = false
    public boolean TIMEOUT_D2_FAILED = true // we're never supposed to get D2

    int DATAGRAM_ATTEMPTS = 0
    int NEXT_EXPECTED_DATAGRAM = 0
    int DATAGRAMS_RECEIVED = 0

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
//        switch(test) {
//            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
//            case DtnTest.Tests.ROUTER_MESSAGE:
//            case DtnTest.Tests.MAX_RETRIES:
//            case DtnTest.Tests.ARRIVAL_PRIORITY:
//            case DtnTest.Tests.EXPIRY_PRIORITY:
//            case DtnTest.Tests.RANDOM_PRIORITY:
                // first we send a (fake) beacon message to the node
                add(new WakerBehavior(300*1000) {
                    @Override
                    void onWake() {
                        dtnlink.send(new DatagramNtf(from: DtnTest.DEST_ADDRESS))
                    }
                })
//                break
//        }
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
            case DtnTest.Tests.MAX_RETRIES:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                        DATAGRAM_ATTEMPTS++
                        String messageID = msg.getMessageID()

                        if (DATAGRAM_ATTEMPTS < DtnTest.DTN_MAX_RETRIES) {
                            add(new WakerBehavior(10 * 1000) {
                                @Override
                                void onWake() {
                                    dtnlink.send(new DatagramFailureNtf(to: DtnTest.DEST_ADDRESS,
                                                                        inReplyTo: messageID))
                                }
                            })
                        } else if (DATAGRAM_ATTEMPTS == DtnTest.DTN_MAX_RETRIES) {
                            MAX_RETRY_RESULT = true
                            add(new WakerBehavior(10 * 1000) {
                                @Override
                                void onWake() {
                                    dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                                                        inReplyTo: messageID))
                                }
                            })
                        } else {
                            MAX_RETRY_RESULT = false
                        }
                    }
                    return new Message(msg, Performative.AGREE)
                }
                break
            case DtnTest.Tests.ARRIVAL_PRIORITY:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                        String messageID = msg.getMessageID()
                        int msgCount = (int)msg.getData()[0]
                        if (msgCount == NEXT_EXPECTED_DATAGRAM) {
                            NEXT_EXPECTED_DATAGRAM++ // if a datagram is OoO it will never pass
                        }
                        if (NEXT_EXPECTED_DATAGRAM == DtnTest.PRIORITY_MESSAGES) {
                            ARRIVAL_PRIORITY_RESULT = true
                        } else {
                            ARRIVAL_PRIORITY_RESULT = false
                        }
                        add(new WakerBehavior(10 * 1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                        inReplyTo: messageID))
                            }
                        })
                    }
                }
                break
            case DtnTest.Tests.EXPIRY_PRIORITY:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                        String messageID = msg.getMessageID()
                        int msgCount = (int)msg.getData()[0]
                        if (msgCount == NEXT_EXPECTED_DATAGRAM) {
                            NEXT_EXPECTED_DATAGRAM++ // if a datagram is OoO it will never pass
                        }
                        if (NEXT_EXPECTED_DATAGRAM == DtnTest.PRIORITY_MESSAGES) {
                            EXPIRY_PRIORITY_RESULT = true
                        } else {
                            EXPIRY_PRIORITY_RESULT = false
                        }
                        add(new WakerBehavior(10 * 1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                        inReplyTo: messageID))
                            }
                        })
                    }
                }
                break
            case DtnTest.Tests.RANDOM_PRIORITY:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                        String messageID = msg.getMessageID()
                        int msgCount = (int)msg.getData()[0]
                        if (msgCount >= 0 && msgCount < DtnTest.PRIORITY_MESSAGES) {
                            DATAGRAMS_RECEIVED++
                        }
                        if (DATAGRAMS_RECEIVED == DtnTest.PRIORITY_MESSAGES) {
                            RANDOM_PRIORITY_RESULT = true
                        } else {
                            RANDOM_PRIORITY_RESULT = false
                        }
                        add(new WakerBehavior(10 * 1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                        inReplyTo: messageID))
                            }
                        })
                    }
                }
                break
            case DtnTest.Tests.TIMEOUT:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                        if (msg.getData()[0] == 1) {
                            TIMEOUT_D1_SUCCESS = true
                            add(new WakerBehavior(10 * 1000) {
                                @Override
                                void onWake() {
                                    dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                            inReplyTo: msg.getMessageID()))
                                }
                            })
                        }
                        if (msg.getData()[0] == 2) {
                            TIMEOUT_D2_FAILED = false
                        }
                    }
                }
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
