package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestApp extends UnetAgent {
    AgentID dtnlink

    public boolean TRIVIAL_MESSAGE_RESULT = false
    public boolean SUCCESSFUL_DELIVERY_RESULT = false
    public boolean ROUTER_MESSAGE_RESULT = false
    public boolean MAX_RETRY_RESULT = false
    public boolean ARRIVAL_PRIORITY_RESULT = false
    public boolean EXPIRY_PRIORITY_RESULT = false
    public boolean RANDOM_PRIORITY_RESULT = false
    public boolean TIMEOUT_D1_SUCCESS = false
    public boolean TIMEOUT_D2_FAILED = false

    int NEXT_EXPECTED_DATAGRAM = 0
    int DATAGRAMS_RECEIVED = 0

    Random random

    DtnTest.Tests test

    TestApp(DtnTest.Tests t) {
        test = t
        random = new Random()
    }

    void setup() {
    }

    void startup() {
        dtnlink = agent("dtnlink")
        subscribe(topic(dtnlink))
        switch(test) {
            case DtnTest.Tests.TRIVIAL_MESSAGE:
                DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                            ttl: DtnTest.MESSAGE_TTL,
                            msgID: DtnTest.MESSAGE_ID,
                            protocol: DtnTest.MESSAGE_PROTOCOL)
                sendDatagram(req)
                break
            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
                DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                            ttl: DtnTest.MESSAGE_TTL,
                            msgID: DtnTest.MESSAGE_ID,
                            protocol: DtnTest.MESSAGE_PROTOCOL,
                            data: DtnTest.MESSAGE_DATA.getBytes())
            sendDatagram(req)
                break
            case DtnTest.Tests.ROUTER_MESSAGE:
                DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                            ttl: DtnTest.MESSAGE_TTL,
                            msgID: DtnTest.MESSAGE_ID,
                            protocol: Protocol.ROUTING,
                            data: DtnTest.MESSAGE_DATA.getBytes())
                sendDatagram(req)
                break
            case DtnTest.Tests.MAX_RETRIES:
                ParameterReq parameterReq = new ParameterReq().set(dtn.DtnLinkParameters.MAX_RETRIES, DtnTest.DTN_MAX_RETRIES)
                ParameterRsp rsp = (ParameterRsp)dtnlink.request(parameterReq, 1000)

                DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                            ttl: DtnTest.MESSAGE_TTL*DtnTest.DTN_MAX_RETRIES, // we are going to refuse the message 5 times
                            msgID: DtnTest.MESSAGE_ID,
                            protocol: DtnTest.MESSAGE_PROTOCOL,
                            data: DtnTest.MESSAGE_DATA.getBytes())
                sendDatagram(req)
                break
            case DtnTest.Tests.ARRIVAL_PRIORITY:
                ParameterReq p = new ParameterReq().set(dtn.DtnLinkParameters.DATAGRAM_PRIORITY,
                        dtn.DtnLink.DatagramPriority.ARRIVAL)
                ParameterRsp rsp = (ParameterRsp)dtnlink.request(p, 1000)
                p = new ParameterReq().set(dtn.DtnLinkParameters.LINK_EXPIRY_TIME, DtnTest.DELAY_TIME/1000*DtnTest.PRIORITY_MESSAGES)
                rsp = (ParameterRsp)dtnlink.request(p, 1000)

                for (int i = 0; i < DtnTest.PRIORITY_MESSAGES; i++) {
                    byte[] b = new byte[1]
                    b[0] = (byte)i
                    DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                            ttl: DtnTest.MESSAGE_TTL + random.nextInt(1000), // so EXPIRY would fail
                            msgID: Integer.toString(i),
                            protocol: DtnTest.MESSAGE_PROTOCOL,
                            data: b)
                    add(new WakerBehavior(i*1000) {
                        @Override
                        void onWake() {
                            sendDatagram(req)
                        }
                    })
                }
                break
            case DtnTest.Tests.EXPIRY_PRIORITY:
                ParameterReq p = new ParameterReq().set(dtn.DtnLinkParameters.DATAGRAM_PRIORITY,
                        dtn.DtnLink.DatagramPriority.EXPIRY)
                ParameterRsp rsp = (ParameterRsp)dtnlink.request(p, 1000)
                p = new ParameterReq().set(dtn.DtnLinkParameters.LINK_EXPIRY_TIME, DtnTest.DELAY_TIME/1000*DtnTest.PRIORITY_MESSAGES)
                rsp = (ParameterRsp)dtnlink.request(p, 1000)

                for (int i = 0; i < DtnTest.PRIORITY_MESSAGES; i++) {
                    byte[] b = new byte[1]
                    b[0] = (byte)(DtnTest.PRIORITY_MESSAGES - 1 - i)
                    DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                            ttl: DtnTest.MESSAGE_TTL - i*20,
                            msgID: Integer.toString(DtnTest.PRIORITY_MESSAGES - 1 - i),
                            protocol: DtnTest.MESSAGE_PROTOCOL,
                            data: b)
                    add(new WakerBehavior(i*1000) {
                        @Override
                        void onWake() {
                            sendDatagram(req)
                        }
                    })
                }
                break
            case DtnTest.Tests.RANDOM_PRIORITY:
                ParameterReq p = new ParameterReq().set(dtn.DtnLinkParameters.DATAGRAM_PRIORITY,
                        dtn.DtnLink.DatagramPriority.RANDOM)
                ParameterRsp rsp = (ParameterRsp)dtnlink.request(p, 1000)
                p = new ParameterReq().set(dtn.DtnLinkParameters.LINK_EXPIRY_TIME, DtnTest.DELAY_TIME/1000*DtnTest.PRIORITY_MESSAGES)
                rsp = (ParameterRsp)dtnlink.request(p, 1000)

                for (int i = 0; i < DtnTest.PRIORITY_MESSAGES; i++) {
                    byte[] b = new byte[1]
                    b[0] = (byte)i
                    DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                            ttl: DtnTest.MESSAGE_TTL, // so EXPIRY would fail
                            msgID: Integer.toString(i),
                            protocol: DtnTest.MESSAGE_PROTOCOL,
                            data: b)
                    add(new WakerBehavior(i*1000) {
                        @Override
                        void onWake() {
                            sendDatagram(req)
                        }
                    })
                }
                break
            case DtnTest.Tests.TIMEOUT:
                ParameterReq parameterReq = new ParameterReq().set(dtn.DtnLinkParameters.LINK_EXPIRY_TIME, 10*60)
                ParameterRsp rsp = (ParameterRsp)dtnlink.request(parameterReq, 1000)

                byte[] d1_data = new byte[1]
                d1_data[0] = (byte)1

                byte[] d2_data = new byte[1]
                d2_data[0] = (byte)2

                DatagramReq d1 = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                        ttl: DtnTest.MESSAGE_TTL,
                        msgID: "1",
                        protocol: DtnTest.MESSAGE_PROTOCOL,
                        data: d1_data)
                DatagramReq d2 = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                        ttl: DtnTest.MESSAGE_TTL,
                        msgID: "2",
                        protocol: DtnTest.MESSAGE_PROTOCOL,
                        data: d2_data)
                sendDatagram(d1)
                add(new WakerBehavior(1000*1500) {
                    @Override
                    void onWake() {
                        sendDatagram(d2)
                    }
                })
        }
    }

    void sendDatagram(DatagramReq req) {
        dtnlink.send(req)
    }

    Message processRequest(Message msg) {
        return null
    }

    void processMessage(Message msg) {
        switch(test) {
            case DtnTest.Tests.TRIVIAL_MESSAGE:
                if (msg.getPerformative() == Performative.AGREE) {
                    TRIVIAL_MESSAGE_RESULT = true
                }
                break
            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == DtnTest.MESSAGE_ID && msg.getTo() == DtnTest.DEST_ADDRESS) {
                        SUCCESSFUL_DELIVERY_RESULT = true
                    }
                }
                break
            case DtnTest.Tests.ROUTER_MESSAGE:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == DtnTest.MESSAGE_ID && msg.getTo() == DtnTest.DEST_ADDRESS) {
                        ROUTER_MESSAGE_RESULT = true
                    }
                }
                break
            case DtnTest.Tests.MAX_RETRIES:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == DtnTest.MESSAGE_ID && msg.getTo() == DtnTest.DEST_ADDRESS) {
                        MAX_RETRY_RESULT = true
                    }
                }
                break
            case DtnTest.Tests.ARRIVAL_PRIORITY:
                if (msg instanceof DatagramDeliveryNtf) {
                    int msgCount = Integer.valueOf(msg.getInReplyTo())
                    if (msgCount == NEXT_EXPECTED_DATAGRAM) {
                        NEXT_EXPECTED_DATAGRAM++ // if a datagram is OoO it will never pass
                    }
                    if (NEXT_EXPECTED_DATAGRAM == DtnTest.PRIORITY_MESSAGES) {
                        ARRIVAL_PRIORITY_RESULT = true
                    } else {
                        ARRIVAL_PRIORITY_RESULT = false
                    }
                }
                break
            case DtnTest.Tests.EXPIRY_PRIORITY:
                if (msg instanceof DatagramDeliveryNtf) {
                    int msgCount = Integer.valueOf(msg.getInReplyTo())
                    if (msgCount == NEXT_EXPECTED_DATAGRAM) {
                        NEXT_EXPECTED_DATAGRAM++ // if a datagram is OoO it will never pass
                    }
                    if (NEXT_EXPECTED_DATAGRAM == DtnTest.PRIORITY_MESSAGES) {
                        EXPIRY_PRIORITY_RESULT = true
                    } else {
                        EXPIRY_PRIORITY_RESULT = false
                    }
                }
                break
            case DtnTest.Tests.RANDOM_PRIORITY:
                if (msg instanceof DatagramDeliveryNtf) {
                    int msgCount = Integer.valueOf(msg.getInReplyTo())
                    if (msgCount >= 0 && msgCount < DtnTest.PRIORITY_MESSAGES) {
                        DATAGRAMS_RECEIVED++
                    }
                    if (DATAGRAMS_RECEIVED == DtnTest.PRIORITY_MESSAGES) {
                        RANDOM_PRIORITY_RESULT = true
                    } else {
                        RANDOM_PRIORITY_RESULT = false
                    }
                }
                break
            case DtnTest.Tests.TIMEOUT:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == "1") {
                        TIMEOUT_D1_SUCCESS = true
                    } else {
                        TIMEOUT_D1_SUCCESS = false
                    }
                }
                if (msg instanceof DatagramFailureNtf) {
                    if (msg.getInReplyTo() == "2") {
                        TIMEOUT_D2_FAILED = true
                    } else {
                        TIMEOUT_D2_FAILED = false
                    }
                }
                break
        }
    }
}
