# Ideas for DTNTransport

* Main idea: use DtnLink as much as possible, make as few changes as possible
    * Consult ROUTING for the tables, let's not redo that work
    * But send the payload by DtnT

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