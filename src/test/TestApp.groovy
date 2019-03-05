package test

import groovy.transform.CompileStatic
import net.fec.openrq.util.rq.Rand
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

    int ARRIVAL_NEXT_DATAGRAM = 0
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
                ParameterReq parameterReq = new ParameterReq().set(dtn.DtnLinkParameters.DATAGRAM_PRIORITY,
                                                                    dtn.DtnLink.DatagramPriority.ARRIVAL)
                ParameterRsp rsp = (ParameterRsp)dtnlink.request(parameterReq, 1000)
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
                    if (msgCount == ARRIVAL_NEXT_DATAGRAM) {
                        ARRIVAL_NEXT_DATAGRAM++ // if a datagram is OoO it will never pass
                    }
                    if (ARRIVAL_NEXT_DATAGRAM == DtnTest.PRIORITY_MESSAGES - 1) {
                        ARRIVAL_PRIORITY_RESULT = true
                    }
                }
                break
        }
    }
}
