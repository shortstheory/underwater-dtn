package test


import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestApp extends UnetAgent {
    AgentID dtnlink

    public boolean trivialMessageResult = false
    public boolean successfulDeliveryResult = false
    public boolean routerMessageResult = false
    public boolean maxRetryResult = false
    public boolean arrivalPriorityResult = false
    public boolean expiryPriorityResult = false
    public boolean randomPriorityResult = false
    public boolean timeoutD1Success = false
    public boolean timeoutD2Failed = false
    public boolean multiLinkResult = false
    public boolean fragementationResult = false
    public boolean reassemblyResult = false

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
                    sendDatagram(req)
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
                add(new WakerBehavior(1000*15) {
                    @Override
                    void onWake() {
                        sendDatagram(d2)
                    }
                })
                break
            case DtnTest.Tests.MULTI_LINK:
                ParameterReq p = new ParameterReq().set(dtn.DtnLinkParameters.LINK_EXPIRY_TIME, DtnTest.DELAY_TIME)
                ParameterRsp rsp = (ParameterRsp)dtnlink.request(p, 1000)
                p = new ParameterReq().set(dtn.DtnLinkParameters.LINK_PRIORITY, DtnTest.LINK_ORDER)
                rsp = (ParameterRsp)dtnlink.request(p, 1000)

                add(new WakerBehavior(2000*1000) {
                    @Override
                    void onWake() {
                        DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                                ttl: DtnTest.DELAY_TIME,
                                msgID: DtnTest.MESSAGE_ID,
                                protocol: DtnTest.MESSAGE_PROTOCOL,
                                data: DtnTest.MESSAGE_DATA.getBytes())
                        sendDatagram(req)
                    }
                })
                break
            case DtnTest.Tests.PAYLOAD_FRAGEMENTATION:
                byte[] data = new byte[DtnTest.PAYLOAD_SIZE]
                for (int i = 0; i < DtnTest.PAYLOAD_SIZE; i++) {
                    data[i] = 65
                }
                DatagramReq req = new DatagramReq(to: DtnTest.DEST_ADDRESS,
                        ttl: DtnTest.MESSAGE_TTL,
                        msgID: DtnTest.MESSAGE_ID,
                        protocol: DtnTest.MESSAGE_PROTOCOL,
                        data: data)
                sendDatagram(req)
            case DtnTest.Tests.PAYLOAD_REASSEMBLY:
                break
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
                    trivialMessageResult = true
                }
                break
            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == DtnTest.MESSAGE_ID && msg.getTo() == DtnTest.DEST_ADDRESS) {
                        successfulDeliveryResult = true
                    }
                }
                break
            case DtnTest.Tests.ROUTER_MESSAGE:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == DtnTest.MESSAGE_ID && msg.getTo() == DtnTest.DEST_ADDRESS) {
                        routerMessageResult = true
                    }
                }
                break
            case DtnTest.Tests.MAX_RETRIES:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == DtnTest.MESSAGE_ID && msg.getTo() == DtnTest.DEST_ADDRESS) {
                        maxRetryResult = true
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
                        arrivalPriorityResult = true
                    } else {
                        arrivalPriorityResult = false
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
                        expiryPriorityResult = true
                    } else {
                        expiryPriorityResult = false
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
                        randomPriorityResult = true
                    } else {
                        randomPriorityResult = false
                    }
                }
                break
            case DtnTest.Tests.TIMEOUT:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == "1") {
                        timeoutD1Success = true
                    } else {
                        timeoutD1Success = false
                    }
                }
                if (msg instanceof DatagramFailureNtf) {
                    if (msg.getInReplyTo() == "2") {
                        timeoutD2Failed = true
                    } else {
                        timeoutD2Failed = false
                    }
                }
                break
            case DtnTest.Tests.MULTI_LINK:
                if (msg instanceof  DatagramDeliveryNtf) {
                    multiLinkResult = true
                }
                break
            case DtnTest.Tests.PAYLOAD_FRAGEMENTATION:
                if (msg instanceof DatagramDeliveryNtf) {
                    if (msg.getInReplyTo() == DtnTest.MESSAGE_ID && msg.getTo() == DtnTest.DEST_ADDRESS) {
                        fragementationResult = true
                    }
                }
                break
            case DtnTest.Tests.PAYLOAD_REASSEMBLY:
                if (msg instanceof DatagramNtf) {
                    byte[] msgBytes = msg.getData()
                    boolean byteCheck = true
                    for (int i = 0; i < DtnTest.PAYLOAD_SIZE; i++) {
                        if (msgBytes[i] != 65) {
                            byteCheck = false
                        }
                    }
                    if (byteCheck && msg.getProtocol() == DtnTest.MESSAGE_PROTOCOL) {
                        reassemblyResult = true
                    }
                }
                break
        }
    }
}
