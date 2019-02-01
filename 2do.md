pending:
* create simulations
* add buffer space checks
* short circuit
* try out 
* have a trigger for the calling the sweep behavior?

done:
* try out PDU classes
* processMessage()/Request()
* generate Ntfs
* fill in methods for Storage


link << new DatagramReq(to: 1)
l = agent("link")
link << new org.arl.unet.DatagramReq(to: 1, data: [1,2,3], reliability: true)

link << new org.arl.unet.DatagramReq(to: 1, data: [1,2,3], protocol: 50, reliability: true)


link << new org.arl.unet.DatagramReq(to: 2, data: [1,2,3], reliability: true)