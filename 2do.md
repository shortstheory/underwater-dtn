issues:
* SAME DG keeps failing!!!
    * probably because they all fail at the same time?!
* waiting for a DFN blocks the rest of the queue
* NPE occasionally in getLinkWithReliability // might be fixed with 1sec timeout
* CSMA MAC isn't working
* I cannot change the defaultLink property of Router. I get the message: "org.arl.unet.UnetException: Parameter defaultLink could not be set [empty response]" when I try to change 
* some unet3 telnet commands do not work
* collect trace.nam stats and use them
* setBEACON_BEHAVIOR() looks bad
* agent.stop() is called several times?

router << new DatagramReq(to: 3, data: [1,2,3])
link << new DatagramReq(to: 2, data: [1,2,3])
link << new DatagramReq(to: 2, data: [1,2,3])

pending:
* increase timeout
* simulations

* delete old links
* should I send a DFN if I don't have a route to node?
* note about fxns in CyclicBehavior
* multiple links per node
* multiple links for Cyclic
* multihop router tests
* code to run on sim start and sim end!!
* sorting of messages
* have a trigger for the calling the sweep behavior?
* configurable containers
* make MTU readonly
* short circuit

done:
* improve stats collection
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