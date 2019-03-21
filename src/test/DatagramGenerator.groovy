package test

import dtn.DtnLink
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
import org.arl.unet.UnetAgent

class DatagramGenerator extends UnetAgent{
    int payloadsSent      = 0
    int payloadsSuccess   = 0
    int payloadsFailure   = 0
    int payloadsReceived  = 0

    int datagramsSent     = 0
    int datagramsSuccess  = 0
    int datagramsFailure  = 0
    int datagramsReceived = 0

    enum Mode {
        REGULAR,
        RANDOM_TTL,
        PAYLOAD,
        RECEIVER
    }

    AgentID dtnLink
    AgentID link
    Mode mode

    int[] destNodes
    int msgsent = 0
    int messagePeriod
    int protocolNumber = 22
    int payloadProtocolNumber = 25
    int msgSize
    int msgTtl
    int PAYLOAD_RETRIES = 1000
    boolean randomGeneration
    Random random = new Random()
    String payloadPath = "testPayload"
    String payloadText = new File(payloadPath).text

    ArrayList<String> sentDatagrams
    ArrayList<String> sentPayloads

    DatagramGenerator() {
        mode = Mode.RECEIVER
    }

    DatagramGenerator(int[] destNodes, int period, int size, int ttl, Mode mode) {
        this.destNodes = destNodes
        messagePeriod = period

        msgSize = size
        messagePeriod = period
        msgTtl = ttl
        this.mode = mode
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
        subscribe(topic(dtnLink))

        switch (mode) {
        case Mode.RANDOM_TTL:
            add(new PoissonBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {
                        String data = createDataSize(random.nextInt(msgSize))
                        int ttl = 200 + random.nextInt(msgTtl)
                        byte[] bytes = data.getBytes()
                        DatagramReq req = new DatagramReq(data: bytes, to: destNode, ttl: ttl, protocol: protocolNumber)
                        dtnLink.send(req)
                        datagramsSent++
                        sentDatagrams.add(req.getMessageID())
                    }
                }
            })
            break
        case Mode.REGULAR:
            String data = createDataSize(msgSize)
            byte[] bytes = data.getBytes()
            add(new TickerBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {
                        DatagramReq req = new DatagramReq(data: bytes, to: destNode, ttl: msgTtl, protocol: protocolNumber)
                        dtnLink.send(req)
                        datagramsSent++
                        sentDatagrams.add(req.getMessageID())
                    }
                }
            })
            break
        case Mode.PAYLOAD:
            String data = payloadText
            byte[] bytes = data.getBytes()
                ParameterReq parameterReq = new ParameterReq().set(dtn.DtnLinkParameters.MAX_RETRIES, PAYLOAD_RETRIES)
                ParameterRsp rsp = (ParameterRsp)dtnLink.request(parameterReq, 1000)

                add(new TickerBehavior(messagePeriod) {
                @Override
                void onTick() {
                    for (int destNode : destNodes) {
                        DatagramReq req = new DatagramReq(data: bytes, to: destNode, ttl: msgTtl, protocol: payloadProtocolNumber)
                        dtnLink.send(req)
                        payloadsSent++
                        sentPayloads.add(req.getMessageID())
                    }
                }
            })
            break
        case Mode.RECEIVER:
            // Nothing to do lah!
            break
        }
    }

    @Override
    protected void processMessage(Message msg) {
        if (msg instanceof DatagramNtf) {
            if (msg.getProtocol() == protocolNumber) {
                datagramsReceived++
            } else if (msg.getProtocol() == payloadProtocolNumber) {
                def x = msg
                String s = new String(msg.getData())
                if (s == payloadText) {
                    println "Successful Tx!"
                } else {
                    println "Fail"
                }
                payloadsReceived++
            }
        } else if (msg instanceof DatagramDeliveryNtf) {
            if (sentDatagrams.contains(msg.getInReplyTo())) {
                datagramsSuccess++
            }
            if (sentPayloads.contains(msg.getInReplyTo())) {
                datagramsSuccess++
            }
        } else if (msg instanceof DatagramFailureNtf) {
            if (sentDatagrams.contains(msg.getInReplyTo())) {
                datagramsFailure++
            }
            if (sentPayloads.contains(msg.getInReplyTo())) {
                payloadsFailure++
            }
        }
    }
}
