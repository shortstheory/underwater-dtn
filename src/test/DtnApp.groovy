package test

import com.sun.org.apache.xalan.internal.xsltc.cmdline.Compile
import dtn.DtnLink
import dtn.DtnLinkParameters
import groovy.transform.CompileStatic
import org.arl.fjage.Agent
import org.arl.fjage.AgentID
import org.arl.fjage.Message
import org.arl.fjage.PoissonBehavior
import org.arl.fjage.TickerBehavior
import org.arl.unet.DatagramDeliveryNtf
import org.arl.unet.DatagramFailureNtf
import org.arl.unet.DatagramNtf
import org.arl.unet.DatagramReq
import org.arl.unet.ParameterReq
import org.arl.unet.ParameterRsp
import org.arl.unet.Services
import org.arl.unet.UnetAgent
import java.util.UUID

@CompileStatic
class DtnApp extends UnetAgent {
    enum Mode {
        REGULAR,
        RANDOM_TTL,
        PAYLOAD,
        RECEIVER
    }

    AgentID dtnLink
    AgentID link
    AgentID router
    Mode mode

    int[] destNodes
    int msgsent = 0
    int messagePeriod
    int protocolNumber = 22
    int payloadProtocolNumber = 25
    int msgSize
    int msgTtl
    int endTime
    int PAYLOAD_RETRIES = 1000
    boolean randomGeneration
    boolean useRouter = false
    Random random = new Random()
    String payloadPath = "src/test/testPayload"
    String payloadText = new File(payloadPath).text

    ArrayList<String> sentDatagrams = new ArrayList<>()
    ArrayList<String> sentPayloads = new ArrayList<>()
    DtnStats stats

    DtnApp(DtnStats stat) {
        mode = Mode.RECEIVER
        stats = stat
    }

    DtnApp(DtnStats stat, boolean router) {
        mode = Mode.RECEIVER
        stats = stat
        useRouter = router
    }

    DtnApp(int[] destNodes, int period, int size, int ttl, int t, boolean router, Mode m, DtnStats stat) {
        this.destNodes = destNodes
        messagePeriod = period*1000

        msgSize = size
        msgTtl = ttl
        endTime = (t) ? t : Integer.MAX_VALUE
        useRouter = router
        mode = m
        stats = stat
    }

    int currentTimeSeconds() {
        return (currentTimeMillis()/1000).intValue()
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
        router = agentForService(Services.ROUTING)
        subscribe(dtnLink)
        subscribe(link)
        if (router != null) {
            subscribe(router)
        }
        dtnLink.send(new ParameterReq().set(DtnLinkParameters.shortCircuit, false))
        switch (mode) {
        case Mode.RANDOM_TTL:
            add(new PoissonBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {
                        String data = createDataSize(random.nextInt(msgSize))
                        int ttl = 200 + random.nextInt(msgTtl)
                        byte[] bytes = data.getBytes()
                        DatagramReq req
//                        req = new DatagramReq(data: bytes, to: destNode, ttl: ttl, protocol: protocolNumber)
                        if (data.length() > 1992) {
                            req = new DatagramReq(data: bytes, to: destNode, ttl: ttl, protocol: payloadProtocolNumber)
                        } else {
                            req = new DatagramReq(data: bytes, to: destNode, ttl: ttl, protocol: protocolNumber)
                        }
                        sendDatagram(req, false)
                    }
                }
            })
            break
        case Mode.REGULAR:
            add(new TickerBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {

                        DatagramReq req = new DatagramReq(to: destNode, ttl: msgTtl, protocol: protocolNumber)
//                        String data = createDataSize(msgSize)
                        String data = req.getMessageID()
                        byte[] bytes = data.getBytes()
                        req.setData(bytes)
                        sendDatagram(req, false)
                    }
                }
            })
            break
        case Mode.PAYLOAD:
            String data = payloadText
            byte[] bytes = data.getBytes()

            add(new TickerBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {
                        DatagramReq req = new DatagramReq(data: bytes, to: destNode, ttl: msgTtl, protocol: payloadProtocolNumber)
                        sendDatagram(req, true)
                    }
                }
            })
            break
        case Mode.RECEIVER:
            // Nothing to do lah!
            break
        }
    }

    void sendDatagram(Message msg, boolean isPayload) {
        if (currentTimeSeconds() < endTime) {
            if (useRouter) {
                AgentID router = agentForService(Services.ROUTING)
                router.send(msg)
            } else {
                dtnLink.send(msg)
            }
            if (isPayload) {
                stats.payloadsSent++
                sentPayloads.add(msg.getMessageID())
            } else {
                stats.datagramsSent++
                sentDatagrams.add(msg.getMessageID())
            }
        }
    }

    @Override
    protected void processMessage(Message msg) {
        if (msg instanceof DatagramNtf) {
            if (msg.getProtocol() == protocolNumber) {
//                println(msg.toString() + " " + msg.getMessageID())
                stats.uniqueDatagrams.add(new String(msg.getData()))
                stats.msgRecv[msg.getFrom()]++
                stats.datagramsReceived++
            } else if (msg.getProtocol() == payloadProtocolNumber) {
                stats.payloadsReceived++
            }
        } else if (msg instanceof DatagramDeliveryNtf) {
            if (sentDatagrams.contains(msg.getInReplyTo())) {
                stats.datagramsSuccess++
            }
            if (sentPayloads.contains(msg.getInReplyTo())) {
                println(msg.getInReplyTo() + " success!")
                stats.payloadsSuccess++
            }
        } else if (msg instanceof DatagramFailureNtf) {
            if (sentDatagrams.contains(msg.getInReplyTo())) {
                stats.datagramsFailure++
            }
            if (sentPayloads.contains(msg.getInReplyTo())) {
                println(msg.getInReplyTo() + " failed!")
                stats.payloadsFailure++
            }
        }
    }
}
