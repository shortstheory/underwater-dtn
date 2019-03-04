package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestAgent extends UnetAgent{
    AgentID dtnlink
    void setup() {
    }

    void startup() {
        dtnlink = agent("dtnlink")
        subscribe(dtnlink)
    }

    void sendDatagram(DatagramReq req) {
        dtnlink.send(req)
    }

    Message processRequest(Message msg) {
        return null
    }

    void processMessage(Message msg) {
    }
}
