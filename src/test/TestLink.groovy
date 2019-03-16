package test

import dtn.DtnLink
import dtn.DtnStorage
import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

import java.nio.file.Files

@CompileStatic
class TestLink extends UnetAgent {
    DtnTest.Tests test

    public boolean successfulDeliveryResult = false
    public boolean routerMessageResult = false
    public boolean maxRetryResult = false
    public boolean arrivalPriorityResult = false // set to false if OoO
    public boolean expiryPriorityResult = false
    public boolean randomPriorityResult = false
    public boolean timeoutD1Success = false
    public boolean timeoutD2Failed = true // we're never supposed to get D2

    public boolean linkPriorityExpectMessage
    public boolean linkPriorityReceivedMessage = false
    public boolean beaconReceived = false

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
        add(new WakerBehavior(300*1000) {
            @Override
            void onWake() {
                dtnlink.send(new DatagramNtf(from: DtnTest.DEST_ADDRESS))
            }
        })
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
                            successfulDeliveryResult = true
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
                        HashMap<String, Integer> pduInfo = decodePdu(msg.getData())
                        if (pduInfo.get(DtnStorage.TTL_MAP) > 0
                            && pduInfo.get(DtnStorage.TTL_MAP) < DtnTest.MESSAGE_TTL
                            && pduInfo.get(DtnStorage.PROTOCOL_MAP) == Protocol.ROUTING
                            && Arrays.copyOfRange(msg.getData(), DtnLink.HEADER_SIZE, msg.getData().length) == DtnTest.MESSAGE_DATA.getBytes()) {
                            routerMessageResult = true
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
                            maxRetryResult = true
                            add(new WakerBehavior(10 * 1000) {
                                @Override
                                void onWake() {
                                    dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                                                        inReplyTo: messageID))
                                }
                            })
                        } else {
                            maxRetryResult = false
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
                            arrivalPriorityResult = true
                        } else {
                            arrivalPriorityResult = false
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
                            expiryPriorityResult = true
                        } else {
                            expiryPriorityResult = false
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
                            randomPriorityResult = true
                        } else {
                            randomPriorityResult = false
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
                            timeoutD1Success = true
                            add(new WakerBehavior(10 * 1000) {
                                @Override
                                void onWake() {
                                    dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                            inReplyTo: msg.getMessageID()))
                                }
                            })
                        }
                        if (msg.getData()[0] == 2) {
                            timeoutD2Failed = false
                        }
                    }
                }
                break
            case DtnTest.Tests.MULTI_LINK:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                            linkPriorityReceivedMessage = true
                        add(new WakerBehavior(10 * 1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                        inReplyTo: msg.getMessageID()))
                            }
                        })
                    } else {
                        beaconReceived = true
                    }
                }
                break
            case DtnTest.Tests.PAYLOAD_FRAGEMENTATION:
                if (msg instanceof DatagramReq) {
                    if (msg.getProtocol() == DtnLink.DTN_PROTOCOL) {
                        add(new WakerBehavior(10 * 1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                        inReplyTo: msg.getMessageID()))
                            }
                        })
                    } else {
                        beaconReceived = true
                    }
                }
                break
            case DtnTest.Tests.PAYLOAD_REASSEMBLY:
                File dir = new File(DtnTest.storagePath)
                File[] files = dir.listFiles()
                for (File file : files) {
                    if (file.isFile()) {
                        byte[] data = readFile(file.getName())
                        println(file.getName())
                        add(new WakerBehavior(30*1000) {
                            @Override
                            void onWake() {
                                DatagramNtf ntf = new DatagramNtf(protocol: DtnLink.DTN_PROTOCOL,
                                                                data: data)
                                dtnlink.send(ntf)
                            }
                        })
                    }
                }
                break
        }
        return null
    }

    int getMTU() {
        return 1600
    }

    public byte[] readFile(String messageID) {
        byte[] pduBytes = Files.readAllBytes(new File(DtnTest.storagePath, messageID).toPath())
    }

    HashMap decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)
        HashMap<String, Integer> map = new HashMap<>()
        map.put(DtnStorage.TTL_MAP, (int)pdu.read24())
        map.put(DtnStorage.PROTOCOL_MAP, (int)pdu.read8())
        map.put(DtnStorage.PAYLOAD_ID_MAP, (int)pdu.read16())
        map.put(DtnStorage.SEGMENT_NUM_MAP, (int)pdu.read16())
        map.put(DtnStorage.TOTAL_SEGMENTS_MAP, (int)pdu.read16())
        return map
    }
}
