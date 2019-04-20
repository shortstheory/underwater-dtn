issues:
* PDU byte arrangement could be better
* remove AB and use the payloadID - which can be random instead
* discard OoO payloads
* random will disrupt the hashCode logic! - so either drop it or so it won't work

* changed datagramCycle -> datagramCycle()
* should AltBit -> payload byte first

router.defaultLink = dtnlink
router << new RouteDiscoveryNtf(to: 3, nextHop: 2, reliability: true, link: dtnlink)

pending:
    * how about a TTL for position updates from a GPS node?
    
later:

in progress:
* inbound fragments will be lost on power disruption
* backup datagrams metadata to internal storage, this only has to be written n deleted once
* remove println->log.info
* startPtr+TBC==isPayload()?

router << new DatagramReq(to: 2, ttl: 5000, protocol: 22, data: [1,2,3])
* should DtnLink be allowed to do RReqs?
* ALL NODES NEED RDP for it to work correctly!
* Which link does RDP use by default? Won't be good for it to use DtnLink :P
* Router will use DtnLink if auto1hop is == true
* does default link have to be symmetric?
* Router does short circuiting!!!

done:
* multihop DTN complicated :P
Alternating Bit
* issue is that if we go out of sync on which bit to expect/receive, then we WILL lose all the messages
* But! DtnLink doesn't touch most messages, they go straight to App!!!
* Then what do we do?
    * leave things as they are
    * make all messages travel through DtnLink
    * may or may not be a good idea depending on how lossy the medium is
* When I use dtnlink as default, it gives the wrong results for the number of DGs which should have been received!!
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



====++COMPLEXAUV+++++====

/usr/lib/jvm/java-8-openjdk-amd64/bin/java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:37035,suspend=y,server=n -javaagent:/snap/intellij-idea-community/138/plugins/Groovy/lib/agent/gragent.jar -javaagent:/snap/intellij-idea-community/138/lib/rt/debugger-agent.jar -Dfile.encoding=UTF-8 -classpath /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/charsets.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/dnsns.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/icedtea-sound.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/jaccess.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/java-atk-wrapper.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/localedata.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/nashorn.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunec.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/zipfs.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jsse.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/management-agent.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/resources.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/home/nic/nus/dtn/out/production/dtn:/home/nic/nus/UnetStack3-prerelease-20190128/lib/cloning-1.9.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-fileupload-1.3.3.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-io-2.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-lang3-3.6.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/groovy-all-2.4.15.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/gson-2.8.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/javax.servlet-api-3.1.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jcommon-1.0.23.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-client-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-http-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-io-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-security-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-server-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-servlet-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-util-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-xml-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jfreechart-1.0.19.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jline-3.9.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jSerialComm-2.4.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jtransforms-2.4.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/junit-4.8.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/objenesis-1.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/openrq-3.3.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-framework-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-simulator-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-stack-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-api-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-client-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-common-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-server-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-servlet-9.4.12.v20180830.jar:/home/nic/nus/commons-math3-3.6.1/commons-math3-3.6.1.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/fjage-1.5.2-SNAPSHOT.jar:/snap/intellij-idea-community/138/lib/idea_rt.jar org.arl.fjage.shell.GroovyBoot cls://org.arl.unet.sim.initrc sim/complex_auv.groovy
Connected to the target VM, address: '127.0.0.1:37035', transport: 'socket'
Starting Complex AUV simulation!
java.lang.reflect.InvocationTargetException
> 
Stats: 
Datagrams: TX: 18 RX: 12 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->0
2->12
3->5
4->1
5->6

Stats: 
Datagrams: TX: 5 RX: 0 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->0
2->40
3->0
4->2
5->12

Stats: 
Datagrams: TX: 1 RX: 16 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->16
2->0
3->32
4->0
5->0

Stats: 
Datagrams: TX: 6 RX: 10 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->10
2->0
3->20
4->0
5->0


/usr/lib/jvm/java-8-openjdk-amd64/bin/java -javaagent:/snap/intellij-idea-community/138/lib/idea_rt.jar=35267:/snap/intellij-idea-community/138/bin -Dfile.encoding=UTF-8 -classpath /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/charsets.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/dnsns.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/icedtea-sound.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/jaccess.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/java-atk-wrapper.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/localedata.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/nashorn.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunec.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/zipfs.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jsse.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/management-agent.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/resources.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/home/nic/nus/dtn/out/production/dtn:/home/nic/nus/UnetStack3-prerelease-20190128/lib/cloning-1.9.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-fileupload-1.3.3.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-io-2.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-lang3-3.6.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/groovy-all-2.4.15.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/gson-2.8.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/javax.servlet-api-3.1.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jcommon-1.0.23.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-client-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-http-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-io-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-security-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-server-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-servlet-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-util-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-xml-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jfreechart-1.0.19.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jline-3.9.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jSerialComm-2.4.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jtransforms-2.4.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/junit-4.8.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/objenesis-1.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/openrq-3.3.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-framework-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-simulator-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-stack-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-api-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-client-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-common-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-server-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-servlet-9.4.12.v20180830.jar:/home/nic/nus/commons-math3-3.6.1/commons-math3-3.6.1.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/fjage-1.5.2-SNAPSHOT.jar org.arl.fjage.shell.GroovyBoot cls://org.arl.unet.sim.initrc sim/complex_auv.groovy
Starting Complex AUV simulation!
java.lang.reflect.InvocationTargetException
> 
Stats: 
Datagrams: TX: 38 RX: 61 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

3->5
4->34
5->22

Stats: 
Datagrams: TX: 5 RX: 0 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0


Stats: 
Datagrams: TX: 34 RX: 36 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->36

Stats: 
Datagrams: TX: 34 RX: 23 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->23

1 simulation completed in 435.505 seconds


Process finished with exit code 0

/usr/lib/jvm/java-8-openjdk-amd64/bin/java -javaagent:/snap/intellij-idea-community/138/lib/idea_rt.jar=35371:/snap/intellij-idea-community/138/bin -Dfile.encoding=UTF-8 -classpath /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/charsets.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/dnsns.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/icedtea-sound.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/jaccess.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/java-atk-wrapper.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/localedata.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/nashorn.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunec.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/zipfs.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jsse.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/management-agent.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/resources.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/home/nic/nus/dtn/out/production/dtn:/home/nic/nus/UnetStack3-prerelease-20190128/lib/cloning-1.9.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-fileupload-1.3.3.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-io-2.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/commons-lang3-3.6.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/groovy-all-2.4.15.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/gson-2.8.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/javax.servlet-api-3.1.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jcommon-1.0.23.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-client-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-http-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-io-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-security-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-server-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-servlet-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-util-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jetty-xml-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jfreechart-1.0.19.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jline-3.9.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jSerialComm-2.4.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/jtransforms-2.4.0.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/junit-4.8.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/objenesis-1.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/openrq-3.3.2.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-framework-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-simulator-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/unet-stack-1.4.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-api-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-client-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-common-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-server-9.4.12.v20180830.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/websocket-servlet-9.4.12.v20180830.jar:/home/nic/nus/commons-math3-3.6.1/commons-math3-3.6.1.jar:/home/nic/nus/UnetStack3-prerelease-20190128/lib/fjage-1.5.2-SNAPSHOT.jar org.arl.fjage.shell.GroovyBoot cls://org.arl.unet.sim.initrc sim/complex_auv.groovy
Starting Complex AUV simulation!
java.lang.reflect.InvocationTargetException
> Message ID 75cde4b5-2fe9-4b8c-9455-ceec44ad913b not found 6303

Stats: 
Datagrams: TX: 38 RX: 1 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

4->1

Stats: 
Datagrams: TX: 5 RX: 0 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0


Stats: 
Datagrams: TX: 34 RX: 13 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->13

Stats: 
Datagrams: TX: 34 RX: 4 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->4

1 simulation completed in 10486.590 seconds

BUG=======
Process finished with exit code 0
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






   Stats: 
Datagrams: TX: 38 RX: 107 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

3->17
4->60
5->30

Stats: 
Datagrams: TX: 5 RX: 0 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0


Stats: 
Datagrams: TX: 34 RX: 3 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->3

Stats: 
Datagrams: TX: 34 RX: 0 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0


1 simulation completed in 1289.365 seconds











NEW::::
with new
Stats: 
Datagrams: TX: 38 RX: 78 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

3->10
4->40
5->28

Stats: 
Datagrams: TX: 5 RX: 0 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0


Stats: 
Datagrams: TX: 34 RX: 24 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->24

Stats: 
Datagrams: TX: 34 RX: 24 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->24

1 simulation completed in 489.583 seconds

OLD:::::
Stats: 
Datagrams: TX: 38 RX: 59 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

3->6
4->32
5->21

Stats: 
Datagrams: TX: 5 RX: 0 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0


Stats: 
Datagrams: TX: 34 RX: 35 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->35

Stats: 
Datagrams: TX: 34 RX: 24 S: 0 F: 0
Payloads:  TX: 0 RX: 0 S: 0 F: 0

1->24

1 simulation completed in 472.615 seconds

