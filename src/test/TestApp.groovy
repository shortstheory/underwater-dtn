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

    DtnTest.Tests test

    TestApp(DtnTest.Tests t) {
        test = t
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
        }
    }
}
