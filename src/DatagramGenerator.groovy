import dtn.DtnLink
import org.arl.fjage.Agent
import org.arl.fjage.AgentID
import org.arl.fjage.PoissonBehavior
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
    boolean randomGeneration
    Random random = new Random()

    DatagramGenerator(int[] destNodes, int period, int size, int ttl, boolean rnd) {
        this.destNodes = destNodes
        messagePeriod = period

        msgSize = size
        messagePeriod = period
        msgTtl = ttl
        randomGeneration = rnd
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


        if (randomGeneration) {
            add(new PoissonBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {
                        String data = createDataSize(random.nextInt(msgSize))
                        int ttl = 200 + random.nextInt(msgTtl)
                        byte[] bytes = data.getBytes()
                        dtnLink.send(new DatagramReq(data: bytes, to: destNode, ttl: ttl, protocol: 22))
                    }
                }
            })
        } else {
            String data = createDataSize(msgSize)
            byte[] bytes = data.getBytes()
            add(new TickerBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {
//                        dtnLink.send(new DatagramReq(data: bytes, to: destNode, ttl: 5000, protocol: 22))
//                        dtnLink.send(new DatagramReq(to: destNode, ttl: 5000, protocol: 5))

                        dtnLink.send(new DatagramReq(data: bytes, to: destNode, ttl: msgTtl, protocol: 22))
                    }
                }
            })
        }
    }
}
