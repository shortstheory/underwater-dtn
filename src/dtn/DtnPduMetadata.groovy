package dtn

import groovy.transform.CompileStatic

/**
 * Used for storing the TTL, bytes sent, next hop, and delivery status of each datagram in the node's volatile memory
 */
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

    enum MessageType {
        INBOUND,
        OUTBOUND
    }

    MessageType getMessageType() {
        if (nextHop == -1) {
            return MessageType.INBOUND
        }
        return MessageType.OUTBOUND
    }

    void setDelivered() {
        delivered = true
    }
}

