package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*
import test.DtnTest

@CompileStatic
class TestApp extends UnetAgent {
    AgentID dtnlink

    String TRIVIAL_MESSAGE_ID = "testmessage"

    public boolean TRIVIAL_MESSAGE_TEST = false

    DtnTest.Tests test

    TestApp(DtnTest.Tests t) {
        test = t
    }

    void setup() {
    }

    void startup() {
        dtnlink = agent("dtnlink")
        subscribe(topic(dtnlink))

        if (test == DtnTest.Tests.TRIVIAL_MESSAGE_TEST) {
            DatagramReq req = new DatagramReq(to: 2, ttl: 200, msgID: TRIVIAL_MESSAGE_ID)
            sendDatagram(req)
        }
    }

    void sendDatagram(DatagramReq req) {
        dtnlink.send(req)
    }

    Message processRequest(Message msg) {
        return null
    }

    void processMessage(Message msg) {
        if (test == DtnTest.Tests.TRIVIAL_MESSAGE_TEST) {
            if (msg.getPerformative() == Performative.AGREE) {
                TRIVIAL_MESSAGE_TEST = true
            }
        }
    }
}
