import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.nodeinfo.*
import org.arl.fjage.Behavior
import groovy.transform.*;
import org.arl.unet.phy.*
import java.util.logging.FileHandler;

//@TypeChecked
@CompileStatic
class DummyAgent extends UnetAgent {
  final static int PING_PROTOCOL = Protocol.USER
  def myntf;
  int addr;
  int dummyValue = 500;

  List<Parameter> getParameterList() {
    allOf(MyAgentParameters)
  }

    class DebugReq extends org.arl.fjage.Message {
      DebugReq(String x) {
        super(Performative.REQUEST);
        msg = x;
      }
      String msg;
    }

    AgentID shellAgent;
    void startup() {
    def phy = agentForService Services.PHYSICAL
    def link = agentForService Services.LINK
    def nodeInfo = agentForService Services.NODE_INFO
    shellAgent = agent("shell");
    
    addr = (int)get(nodeInfo, NodeInfoParam.address)
    
    //subscribe(link)
    subscribe(phy)
    //subscribe(topic(phy, Physical.SNOOP))

    add(new TickerBehavior(1000) {
        @Override
        void onTick() {
            super.onTick()
            if (addr == 1) {
                byte[] mydata = "Hello World".getBytes()
                link.send(new DatagramReq(to: 2, data: mydata, protocol: 15));
            }
        }
       // shellAgent << new DebugReq(msg: "HI WORLD");
    });
      //log.warning(addr+'Hello earth!!!');
  }

  Message processRequest(Message msg) {
    return msg
  }

  void processMessage(Message msg) {
    //if (msg instanceof DatagramNtf) {
    //  shellAgent.send(new DebugReq("Received msg: " + msg.toString()));
    //}
    if (msg instanceof TxFrameNtf) {
      println(addr+" ReceivedTFNMsg "+msg.getRecipient());  
    }
/*    if (msg instanceof DatagramNtf && msg.protocol == PING_PROTOCOL) {
      request new DatagramReq(recipient: msg.sender, to: msg.from, protocol: Protocol.DATA);
    } else if (msg instanceof DatagramDeliveryNtf) {
      myntf = msg;
      println("RECEIVEDDDNMSG");
    }*/
  }
}
