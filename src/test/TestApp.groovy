package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestApp extends UnetAgent {
    AgentID dtnlink

    enum Tests {
        TRIVIAL_MESSAGE_TEST
    }

    Tests test

    TestApp(Tests t) {
        test = t
    }

    void setup() {
    }

    void startup() {
        dtnlink = agent("dtnlink")
        subscribe(topic(dtnlink))

        if (test == Tests.TRIVIAL_MESSAGE_TEST) {
            String id = "my_id"
            DatagramReq req = new DatagramReq(to: 2, ttl: 200, msgID: id)
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
        if (msg) {

        }
    }
}
