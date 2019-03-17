package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked;

@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    int attempts
    boolean delivered
    int payloadID
    int segmentNumber

    MessageType getMessageType() {
        return (payloadID) ? MessageType.PAYLOAD_SEGMENT : MessageType.DATAGRAM
    }
}

enum MessageType {
    PAYLOAD_SEGMENT,
    DATAGRAM
}