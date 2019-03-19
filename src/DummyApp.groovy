import groovy.transform.CompileStatic
import org.arl.fjage.AgentID
import org.arl.fjage.Message
import org.arl.unet.DatagramNtf
import org.arl.unet.Services
import org.arl.unet.UnetAgent
import org.arl.unet.nodeinfo.NodeInfoParam

@CompileStatic
class DummyApp extends UnetAgent {
    AgentID dtnlink
    AgentID link
    void startup() {
        dtnlink = agent("dtnlink")
        link = agent("link")
        subscribe(dtnlink)
        subscribe(link)
    }

    void processMessage(Message msg) {
        if (msg instanceof DatagramNtf) {
            if (msg.getProtocol() == 22) {
                def x = msg
            }
        }
    }
}
