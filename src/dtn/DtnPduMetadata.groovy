package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    int attempts
    boolean delivered
    int bytesSent
    int payloadID

    DtnType.MessageType getMessageType() {
        return (payloadID) ? DtnType.MessageType.PAYLOAD_SEGMENT : DtnType.MessageType.DATAGRAM
    }
}

