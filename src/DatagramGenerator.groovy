import dtn.DtnLink
import org.arl.fjage.Agent
import org.arl.fjage.AgentID
import org.arl.fjage.TickerBehavior
import org.arl.unet.DatagramReq
import org.arl.unet.UnetAgent

class DatagramGenerator extends UnetAgent{
    AgentID dtnLink
    AgentID link

    int[] destNodes
    int msgsent = 0
    int messagePeriod

    int msgSize
    int msgTtl

    DatagramGenerator(int[] destNodes, int period, int size, int ttl) {
        this.destNodes = destNodes
        messagePeriod = period

        msgSize = size
        messagePeriod = period
        msgTtl = ttl
    }

    private static String createDataSize(int msgSize) {
        StringBuilder sb
        sb = new StringBuilder(msgSize);
        for (int i = 0; i < msgSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Override
    protected void startup() {
        dtnLink = agent("dtnlink")
        link = agent("link")
        String data = createDataSize(msgSize)
        byte[] bytes = data.getBytes()
        add(new TickerBehavior(messagePeriod, {
            for (int destNode : destNodes) {
//                println "Messages Sent to " + destNode + ": " + ++msgsent
//                link.send(new DatagramReq(data: bytes, to: destNode, ttl: 10000, protocol: DtnLink.DTN_PROTOCOL))
                dtnLink.send(new DatagramReq(data: bytes, to: destNode, ttl: msgTtl, protocol: DtnLink.DTN_PROTOCOL))
            }
        }))
    }
}
