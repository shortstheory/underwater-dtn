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