package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*

@CompileStatic
class TestLink extends UnetAgent {
    DtnTest.Tests test

    boolean SUCCESSFUL_DELIVERY_RESULT = false
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
        switch(test) {
            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
                // first we send a (fake) beacon message to the node
                dtnlink.send(new DatagramNtf(from: DtnTest.DEST_ADDRESS, msgID: "mymsg"))
                break
        }
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
                    println("GotDR4test")
                    if (msg.getMessageID() == DtnTest.MESSAGE_ID) {
                        add(new WakerBehavior(100 * 1000) {
                            @Override
                            void onWake() {
                                dtnlink.send(new DatagramDeliveryNtf(to: DtnTest.DEST_ADDRESS,
                                        inReplyTo: DtnTest.MESSAGE_ID))
                            }
                        })
                    }
                    return new Message(msg, Performative.AGREE)
                }
                break
        }
        return null
    }

    void processMessage(Message msg) {
//        switch(test) {
//            case DtnTest.Tests.SUCCESSFUL_DELIVERY:
//                break
//        }
    }

    int getMTU() {
        return 1600
    }
}
