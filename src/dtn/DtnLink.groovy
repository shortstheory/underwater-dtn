package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.arl.fjage.AgentID
import org.arl.fjage.Message
import org.arl.fjage.TickerBehavior
import org.arl.unet.DatagramCapability
import org.arl.unet.Services
import org.arl.unet.UnetAgent

@TypeChecked
@CompileStatic
class DtnLink extends UnetAgent {
    private DtnStorage storage
    private int nodeAddress
    private long linkLastSeen

    private AgentID link
    private AgentID notify

    int BEACON_DURATION = 100
    int SWEEP_DURATION = 100

    TickerBehavior beacon
    TickerBehavior sweepStorage

    @Override
    protected void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)

        addCapability(DatagramCapability.RELIABILITY)
    }

    @Override
    protected void startup() {
    }

    @Override
    protected Message processRequest(Message msg) {
    }

    @Override
    protected void processMessage(Message msg) {
    }
}
