package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    int attempts
    boolean delivered
    int payloadID
    int segmentNumber

    DtnType.MessageType getMessageType() {
        return (payloadID) ? DtnType.MessageType.PAYLOAD_SEGMENT : DtnType.MessageType.DATAGRAM
    }
}

