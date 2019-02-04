issues:
* @TypeChecked breaks TickerBehavior
* How do I get the instanceof an agentID to make sure I don't subscribe to DTNLink?
* ttls are floats, but I want int/long
* write32/48 but not write64 for floats?
* CSMA MAC isn't working
* without reliaiblity, reliableLink does not send any messages!!
* passing ref of Agent to DtnStorage is bad

pending:
* create message triggered behaviors
    * cyclic behavior
    * mark link as alive and on getting DDN, send a message on that link on DDN
    * tickerbehavior for periodic wakeups of that
* short circuit
* have a trigger for the calling the sweep behavior?

done:
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


link << new DatagramReq(to: 1)
l = agent("link")
link << new org.arl.unet.DatagramReq(to: 1, data: [1,2,3], reliability: true)

link << new org.arl.unet.DatagramReq(to: 1, data: [1,2,3], protocol: 50, reliability: true)


link << new org.arl.unet.DatagramReq(to: 2, data: [1,2,3], reliability: true)