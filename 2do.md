
issues:
assert linkState == LinkState.ACK_WAIT
       |                      |
       IDLE                   ACK_WAIT

Stack trace: ...
   org.arl.unet.link.ReliableLink$7.onWake(ReliableLink.groovy:362)
   org.arl.fjage.WakerBehavior.action(WakerBehavior.java:86)
   org.arl.fjage.Agent.run(Agent.java:782) ...

1554207380013|SEVERE|org.arl.unet.link.ReliableLink@25:die|Agent link died: Assertion failed: 
	
	assert linkState == LinkState.ACK_WAIT
	       |                      |
	       IDLE                   ACK_WAIT
==========================================	
Stack trace: ...
   org.arl.unet.link.ReliableLink.endTxFrag(ReliableLink.groovy:355)
   org.arl.unet.link.ReliableLink.endTxFrag(ReliableLink.groovy)
   org.arl.unet.link.ReliableLink.processMessage(ReliableLink.groovy:205)
   org.arl.unet.UnetAgent$2.onReceive(UnetAgent.java:74)
   org.arl.fjage.MessageBehavior.action(MessageBehavior.java:82)
   org.arl.fjage.Agent.run(Agent.java:782) ...
pending:


later:

in progress:

done:
* does @Nullable do anything?
* publish progress use ProgressNtfs?
* is my PDU ID random enough? - maybe use pearson hashing?
* improve the RFC, make it more neutral and stick with RFC style guide
* check if you can replace the datagramMap with a queue or other DS
* datagramMap can be avoided entirely if we use the same msgID for reTx-ing messages
* try deleting datagra-ms immediately - not much point
* check for amount of coupling in DtnLink/Storage and remove the useless methods
* removed more useless methods
* Reply structure
* Use req instead of send
* add a payload test
* wrote fragmentation test
* added check for rsp when sending on sendDatagram
* fragmentation
* lots of duplications may be needed in tests
    * can be circumvented by using the same cases and just sending a BAM and checking that it behaves well
* replace it with the generator and the testApp for collecting stats which can be displayed
* add a new SIMS folder :P
* change the place where the retry count & all is incremented
* delete stats tracker NOW
* prof's idea is better than mine
* multihop router tests
**create issue for the bitrate of the link**
* start midsem report
* ask how to write tests
* configurable containers

* what works
    * splitting  & reassembly
* what doesn't
    * probably a lot of other things too, but they need to be worked out
        * deletion of expired payloads
    * write tests
    * refactor and redo v. v. messy deletion logic!!
!!* clean code?
!!* study some 3B1B
* do docu

* what MTU to use?
* using debugger in router causes different outputs in the processRequest function of DtnLink
* udplink crash
* rxframentf & dntf problems

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


// random commands:

new ArrayList<AgentID>(Arrays.asList(phy,linkX,link))
* addroute 2,1,dtnlink - > this makes the regenerated DatagramReq go to node 1 when I do router << new DatagramReq(to: 2) ???

addroute 3,2,dtnlink

dtnlink.send(new ParameterReq().set(dtn.DtnLinkParameters.MAX_RETRIES, 5))

dtnlink << new DatagramReq(to: 3, ttl: 5000, protocol: 5, data: [0,1,2,3,4,5,6,7,8,9])
dtnlink << new DatagramReq(to: 2, ttl: 5000, protocol: 5)

router << new DatagramReq(to: 2, ttl: 5000, protocol: 5)
router << new DatagramReq(to: 2, ttl: 5000, protocol: 5)
do two modems in same container cause problems?
UdpLink ignores distance!
TTL/size in random simulations is the largest size of the TTL/size that can be selected for these values

router << new org.arl.unet.net.RouteDiscoveryNtf(nextHop: 2, to: 3, link: dtnlink, reliability: true)
router << new org.arl.unet.DatagramReq(to: 3, reliability: true)

addroute 3,2,dtnlink
router << new DatagramReq(to: 3, ttl: 5000, protocol: 23, data: [0,1,2,3,4,5,6,7,8,9])
dtnlink << new DatagramReq(to: 3, ttl: 5000, protocol: 23, data: [0,1,2,3,4,5,6,7,8,9])
dtnlink << new DatagramReq(to: 2, ttl: 5000, protocol: 23, data: [0,1,2,3,4,5,6,7,8,9])


router << new DatagramReq(to: 3, ttl: 5000, protocol: 29, data: {0,1,2,3,4,5,6,7,8,9})

link << new org.arl.unet.DatagramReq(to: 3, reliability: true)
link << new org.arl.unet.DatagramReq(to: 0)


    ArrayList<AgentID> getLinks() {
        ArrayList<AgentID> links = new ArrayList<>()
        for (AgentID aid : linkInfo.keySet()) {
            links.add(aid)
        }
        return links
    }

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

Node	Tx  	Rx  	Fail	Suc 	Req 	Stor	Rsnt	Expr	Beac	Coll	BadF	F%  	Tx%		Mean	SD

1 & 165 & 0 & 73 & 91 & 103 & 0 &	 64 & 8 & 156 & 0 & 17  & 0.442 & 0.883 & 68.32 & 147.63
2 & 0 & 91 & 0 & 0 & 0 & 0 & 0 & 0 & 391 & 0 & 600 & NaN & NaN & NaN & NaN
2 & 0 & 1 & 0 & 0 & 0 & 0 & 0 & 0 & 1 & 0 & 0 & N & N & N & N
{c | c | c | c | c | c | c | c | c | c | c | c | c | c | c | c}

    Node & Tx & Rx & Fail & Suc & Req & Stor & Rsnt & Expr & Beac & Coll & BadF & F% & Tx% & Mean & SD

\begin{table}
\caption{The effects of treatments X and Y on the four groups studied.}
\label{tab:treatments}
\centering
\begin{tabular}{l l l}
\toprule
\tabhead{Groups} & \tabhead{Treatment X} & \tabhead{Treatment Y} \\
\midrule
1 & 0.2 & 0.8\\
2 & 0.17 & 0.7\\
3 & 0.24 & 0.75\\
4 & 0.68 & 0.3\\
\bottomrule\\
\end{tabular}
\end{table}


MessageType updateMaps(String messageID) {
    DtnPduMetadata metadata = getMetadata(messageID)
    if (metadata != null) {
        metadata.delivered = true
        outboundPayloads.removePendingSegment(metadata.payloadID, messageID)
        if (metadata.payloadID) {
            if (outboundPayloads.payloadTransferred(metadata.payloadID)) {
                return MessageType.PAYLOAD_TRANSFERRED
            }
            return MessageType.PAYLOAD_SEGMENT
        }
    }
    return MessageType.DATAGRAM
}
ROUTER tests:

1 -> 2 -> 3 -> 4 (all 1km apart)

for 1:
1: to 2 via link/2 [reliable, hops: 1, metric: 6.0]
2: to 3 via link/2 [reliable, hops: 2, metric: 2.5500002]
3: to 4 via link/2 [reliable, hops: 3, metric: 2.4]

for 2:
1: to 1 via link/3 [reliable, hops: 3, metric: 2.4]
2: to 1 via link/1 [reliable, hops: 1, metric: 6.0]
3: to 4 via link/3 [reliable, hops: 2, metric: 2.5500002]
4: to 3 via link/3 [reliable, hops: 1, metric: 6.0]

for 3:
1: to 2 via link/2 [reliable, hops: 1, metric: 3.0]
2: to 1 via link/2 [reliable, hops: 2, metric: 5.1]
3: to 4 via link/4 [reliable, hops: 1, metric: 3.0]

for 4:
1: to 3 via link/3 [reliable, hops: 1, metric: 3.0]
2: to 1 via link/3 [reliable, hops: 3, metric: 2.4]


++++++++++++++++++++++++++++++++++++++++++++++++DtnTransport+++++++++++++++++++++++++++++++++++++++++++
# Ideas for DTNTransport

* Main idea: use DtnLink as much as possible, make as few changes as possible
    * Consult ROUTING for the tables, let's not redo that work
    * But send the payload by DtnT
    * support fragmentation,datagram,transport

* multicopy routing looks wasteful, lots of msg duplication, stresses the MAC
    * choose LCP instead via DV?

* PDU:
    * TTL
    * protocol number
    * final destination
    * PayloadID
    * PayloadSeqNo
    * TotalSeqNo // can we remove this somehow?
    * Data (upto MTU of DtnLink, which is min of connected Links)

* most of these are same as DtnPdu, so we could probably just reuse it with extensions, just keeping the first bytes constant for compatibility

* How should rreqs be initiated - takes some time to update during which we will have to wait a bit
    * no path means we can just go to next hop

* each payload gets segmented with unique ID, each segment gets a seq number
    * total number of segments saved in PDU
    * Q: Should the entire payload be transferred before moving it onto the next hop?
* Failed messages not needed, let it just TTL on all the nodes
* But we need ACKs to go back to the destination - send it like a DDN
    * simple format, just need the payload ID and result
    * maybe we can throw in arrivalTime/TTL remaining for stats though that won't really be relevant because multiple segments
    * send it through least cost path?
* How to track segments?
    * new data structure & listen to dtnlink for the ACKs
    * fail only on TTL, so all the segments will fail at the same time
    * Look at SWTransport RxStatus/TxStatus

* How does routing metric work? What does it mean?
* What routing algo should I use? - AODV?

++++++++++++++++++++++++++++++++++++TEST_Cases+++++++++++++++++++++++++
Check for:
* DatagramReqs are correct for short-circuit
* PDUs are correctly created for Routing Headers
* TTL reduction is correct
* PDUs who are expired are deleted with DFNs
* messages are received correctly to the dest
* multi-link test works
* beacon discovery works
* use platform send


Create new agents
* TestSender
    * subscribes to the dtnlink for DDNs/DFNs
* TestReceiver
    * subscribes to dtnlink for messages
    * checks if all is OK
    * how does the testagent know that the result is OK?
        * i have no idea?!
        
* TestLink aka FarziLink
    * does reliability and Link
    * on sending a messsage it should send it using platformSend

+++++++++++++++++++++++++++++++++++++CHANGE_LOG++++++++++++++++++++++++++++++++++++++++===
Week 10:
* added tests
* fixed owner bug for Link

Week 9:
* thinking about tests
* writing TS
* saved 3 bytes in the PDU
* added multi-link support
* fixed broken pdu logic with output/inputstreams

Week 8:
* running simulations and collecting stats
* added random generation of Datagrams
* added new sims
* added short-circuit

Week 7:
* made the beacon behavior execute regardless
* stability
* auv simulation
* changed behaviors to poisson
* added different examples
* added stats
* switched to basicchannelmodel
* added a LinkState variable
* added a sent field to DtnPduMetadata
* moved all deletion logic to a single place in the code