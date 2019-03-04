package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestApp extends UnetAgent {
    AgentID dtnlink

    String TRIVIAL_MESSAGE_ID = "testmessage"

    public boolean TRIVIAL_MESSAGE_RESULT = false
    public boolean SUCCESSFUL_DELIVERY_RESULT = false

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
                DatagramReq req = new DatagramReq(to: 2, ttl: 200, msgID: TRIVIAL_MESSAGE_ID)
                sendDatagram(req)
                break
            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
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
                break
        }
    }
}
