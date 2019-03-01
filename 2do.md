issues:
**create issue for the bitrate of the link**

* what MTU to use?
* using debugger in router causes different outputs in the processRequest function of DtnLink
* udplink crash
* rxframentf & dntf problems
* addroute 2,1,dtnlink - > this makes the regenerated DatagramReq go to node 1 when I do router << new DatagramReq(to: 2) ???

addroute 3,2,dtnlink

dtnlink << new DatagramReq(to: 2, ttl: 5000, protocol: 5, data: {0,1,2,3,4,5,6,7,8,9})
dtnlink << new DatagramReq(to: 2, ttl: 5000, protocol: 5)

router << new DatagramReq(to: 2, ttl: 5000, protocol: 5)
router << new DatagramReq(to: 2, ttl: 5000, protocol: 5)
do two modems in same container cause problems?
UdpLink ignores distance!
TTL/size in random simulations is the largest size of the TTL/size that can be selected for these values

router << new org.arl.unet.net.RouteDiscoveryNtf(nextHop: 2, to: 3, link: dtnlink, reliability: true)
router << new org.arl.unet.DatagramReq(to: 3, reliability: true)
link << new org.arl.unet.DatagramReq(to: 3, reliability: true)
link << new org.arl.unet.DatagramReq(to: 0)

pending:
!!* clean code?
!!* study some 3B1B
* start midsem report
* do docu
* ask how to write tests

* multihop router tests
* configurable containers

later:

done:
* need to see how UdpLink is handled by multilink - we won't receive RxFrameNtfs so think of another way
* simulations
* multiple links per node
* multiple links for Cyclic
!!* read SAUVC rules
!!* have max retry before FAIL
* short circuit
* I cannot change the defaultLink property of Router. I get the message: "org.arl.unet.UnetException: Parameter defaultLink could not be set [empty response]" when I try to change 
* make MTU readonly
* next DG selecting logic
* change to poissonbehavior for beacon
* should I send a DFN if I don't have a route to node?
* note about fxns in CyclicBehavior
* code to run on sim start and sim end!!
* sorting of messages
* have a trigger for the calling the sweep behavior?
* some datagrams in AUV scenario are not getting transferred in their window of opportunity
2 recipients for node == much longer delivery times
* random NPEs/FNF
* waiting for a DFN blocks the rest of the queue
* NPE occasionally in getLinkWithReliability // might be fixed with 1sec timeout
* some unet3 telnet commands do not work
* collect trace.nam stats and use them
* setBEACON_BEHAVIOR() looks bad
* agent.stop() is called several times?

* CSMA MAC isn't working
* SAME DG keeps failing!!!
    * probably because they all fail at the same time?!
* delete old links
* improve stats collection
* increase timeout
* TickerBehavior doesn't have a configurable period
* without reliaiblity, reliableLink does not send any messages!!
* write32/48 but not write64 for floats?
* passing ref of Agent to DtnStorage is bad
* ttls are floats, but I want int/long
* create message triggered behaviors
    * cyclic behavior
    * mark link as alive and on getting DDN, send a message on that link on DDN
    * tickerbehavior for periodic wakeups of that
* CyclicBehavior not blocking and resending same message many times
* How do I get the instanceof an agentID to make sure I don't subscribe to DTNLink?
* @TypeChecked breaks TickerBehavior
* create the document
* file github issue with code snippets for @TypeChecked and CSMA
* create simulations
* add buffer space checks
* try out PDU classes
* processMessage()/Request()
* generate Ntfs
* fill in methods for Storage
* telnet doesn't work for me
* sending beacon at same causes X_X

experimental:
        Container[] containers = platform.getContainers()
//        for (Container container : containers) {
//            println "gotContainer"
//        }
        DtnLink l
        for (AgentID agentID : containers[0].getAgents()) {
            if (agentID.getName() == "dtnlink") {
                AgentID agent = agentID

                l = containers[0].getAgent(agent)
                stats = l.getStats()
                println "Ref2Dtn"
            }
        }


link << new DatagramReq(to: 1)
l = agent("link")
link << new org.arl.unet.DatagramReq(to: 1, data: [1,2,3], reliability: true)

link << new org.arl.unet.DatagramReq(to: 1, data: [1,2,3], protocol: 50, reliability: true)


link << new org.arl.unet.DatagramReq(to: 2, data: [1,2,3], reliability: true)

router << new DatagramReq(to: 3, data: [1,2,3])
link << new DatagramReq(to: 2, data: [1,2,3])
link << new DatagramReq(to: 2, data: [1,2,3])
BasicChannelMode is better!


You can do it.remove! But not directly from the Map!!

'%4d\t\t%4d\t\t%4d\t\t%4d\t\t%4d\t\t%4d\t\t%4d\t\t%4d\t\t%4d\t\t%5.3f\t%5.3f\t%5.2f\t%5.2f',

UdpLink bug:

Exception in thread "link [monitor]" org.arl.fjage.FjageError: request() should only be called from agent thread 44, but called from 47
	at org.arl.fjage.Agent.request(Agent.java:528)
	at org.arl.unet.NodeAddressCache.update(NodeAddressCache.java:79)
	at org.arl.unet.NodeAddressCache.getAddress(NodeAddressCache.java:42)
	at org.arl.unet.link.UdpLink.receiveDatagram(UdpLink.java:353)
	at org.arl.unet.link.UdpLink.access$700(UdpLink.java:17)
	at org.arl.unet.link.UdpLink$RxMonitor.run(UdpLink.java:449)
Exception in thread "link [monitor]" org.arl.fjage.FjageError: request() should only be called from agent thread 35, but called from 38
	at org.arl.fjage.Agent.request(Agent.java:528)
	at org.arl.unet.NodeAddressCache.update(NodeAddressCache.java:79)
	at org.arl.unet.NodeAddressCache.getAddress(NodeAddressCache.java:42)
	at org.arl.unet.link.UdpLink.receiveDatagram(UdpLink.java:353)
	at org.arl.unet.link.UdpLink.access$700(UdpLink.java:17)
	at org.arl.unet.link.UdpLink$RxMonitor.run(UdpLink.java:449)
AGREE