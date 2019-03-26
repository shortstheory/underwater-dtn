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

    DtnPduMetadata(int hop, int expiry) {
        attempts = 1
        bytesSent = 0
        delivered = false
        nextHop = hop
        expiryTime = expiry
    }

    MessageType getMessageType() {
        if (nextHop != -1) {
            return MessageType.OUTBOUND
        } else {
            return MessageType.INBOUND
        }
    }

    void setDelivered() {
        delivered = true
    }

    // FIXME: Change the names of these message types
    enum MessageType {
        INBOUND,
        OUTBOUND
    }
}

