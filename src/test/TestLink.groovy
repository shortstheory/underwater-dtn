package test

import groovy.transform.CompileStatic
import org.arl.unet.*

@CompileStatic
class TestLink extends UnetAgent {
    void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)
        addCapability(DatagramCapability.RELIABILITY)
    }
}
