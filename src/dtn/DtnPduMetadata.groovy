package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    int attempts
    boolean delivered
    int bytesSent
    int size

    MessageType getMessageType() {
        if (nextHop != -1) {
            return MessageType.OUTBOUND
        } else {
            return MessageType.INBOUND
        }
    }

        // FIXME: Change the names of these message types
    enum MessageType {
        INBOUND,
        OUTBOUND
    }
}

