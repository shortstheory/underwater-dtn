package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestLink extends UnetAgent {
    DtnTest.Tests test

    TestLink(DtnTest.Tests t) {
        test = t
    }

    void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)
        addCapability(DatagramCapability.RELIABILITY)
    }

    List<Parameter> getParameterList() {
        return allOf(DatagramParam.class)
    }

    Message processRequest(Message msg) {
        switch (test) {
        case DtnTest.Tests.TRIVIAL_MESSAGE:
            if (msg instanceof DatagramReq) {
                return new Message(msg, Performative.AGREE)
            }
            break
        }
        return null
    }

    void processMessage(Message msg) {

    }

    int getMTU() {
        return 1600
    }
}
