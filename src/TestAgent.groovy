import org.arl.fjage.Agent
import org.arl.fjage.AgentID
import org.arl.fjage.TickerBehavior
import org.arl.unet.DatagramReq
import org.arl.unet.UnetAgent

class TestAgent extends UnetAgent{
    AgentID dtnLink
    AgentID link

    int destNode

    TestAgent(int destNode) {
        this.destNode = destNode
    }

    private static String createDataSize(int msgSize) {
        StringBuilder sb = new StringBuilder(msgSize);
        for (int i = 0; i < msgSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Override
    protected void startup() {
        dtnLink = agent("dtnlink")
        link = agent("link")
        String data = createDataSize(100)
        byte[] bytes = data.getBytes()
        add(new TickerBehavior(100.second, {
//            dtnLink.send(new DatagramReq(data: bytes, to: destNode, ttl: 10000, protocol: 10))
            link.send(new DatagramReq(data: bytes, to: destNode, ttl: 10000, protocol: 99))
        }))
    }
}
