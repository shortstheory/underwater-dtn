package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.arl.fjage.AgentID
import org.arl.fjage.Message
import org.arl.fjage.Performative
import org.arl.fjage.TickerBehavior
import org.arl.unet.Address
import org.arl.unet.CapabilityReq
import org.arl.unet.DatagramCapability
import org.arl.unet.DatagramReq
import org.arl.unet.Services
import org.arl.unet.UnetAgent
import org.arl.unet.nodeinfo.NodeInfoParam
import org.arl.unet.phy.Physical

//@TypeChecked
@CompileStatic
class DtnLink extends UnetAgent {
    private DtnStorage storage
    private int nodeAddress
    private long lastReceivedTime = 0

    private AgentID link
    private AgentID notify
    private AgentID nodeInfo
    private AgentID phy

    int BEACON_DURATION = 100*1000
    int SWEEP_DURATION = 100*1000

    @Override
    protected void setup() {
        register(Services.LINK)
        register(Services.DATAGRAM)

        addCapability(DatagramCapability.RELIABILITY)
    }

    @Override
    protected void startup() {
        nodeInfo = agentForService(Services.NODE_INFO)
        nodeAddress = (int)get(nodeInfo, NodeInfoParam.address)
        notify = topic()

        storage = new DtnStorage(Integer.toString(nodeAddress))

        link = getLinkWithReliability()
        if (link != null) {
            subscribe(link)
            phy = agent((String)get(link, org.arl.unet.link.ReliableLinkParam.phy))
            if (phy != null) {
                subscribe(phy)
                subscribe(topic(phy, Physical.SNOOP))
            } else {
                println "PHY not provided for link"
            }
        }
        add(new TickerBehavior(BEACON_DURATION) {
            @Override
            void onTick() {
                super.onTick()
                if (System.currentTimeSeconds() - lastReceivedTime >= BEACON_DURATION) {
                    link.send(new DatagramReq(to: Address.BROADCAST))
                    lastReceivedTime = System.currentTimeSeconds()
                }
            }
        })

        add(new TickerBehavior(SWEEP_DURATION) {
            @Override
            void onTick() {
                super.onTick()
                ArrayList<Tuple2> expiredDatagrams = storage.deleteExpiredDatagrams()
            }
        })
    }

    AgentID getLinkWithReliability() {
        AgentID[] links = agentsForService(Services.LINK)

        for (AgentID link : links) {
            CapabilityReq req = new CapabilityReq(link, DatagramCapability.RELIABILITY)
            Message rsp = request(req, 500)
            if (rsp.getPerformative() == Performative.CONFIRM) {
                return link
            }
        }
        return null
    }

    @Override
    protected Message processRequest(Message msg) {
    }

    @Override
    protected void processMessage(Message msg) {
    }
}
