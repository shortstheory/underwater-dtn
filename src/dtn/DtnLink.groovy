package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.arl.fjage.AgentID
import org.arl.fjage.Message
import org.arl.fjage.TickerBehavior
import org.arl.unet.UnetAgent
import sun.management.Agent

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
