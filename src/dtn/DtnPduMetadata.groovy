package dtn

import groovy.transform.CompileStatic

/**
 * Used for storing the TTL, bytes sent, next hop, and delivery status of each datagram in the node's volatile memory
 */
@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    int attempts // only used for statistics
    boolean delivered
    int bytesSent
    int size

    public static final int INBOUND_HOP = -1

    DtnPduMetadata(int hop, int expiry) {
        attempts = 1
        bytesSent = 0
        delivered = false
        nextHop = hop
        expiryTime = expiry
    }

    /**
     * INBOUND messages are in-progress downloads of Payloads which are sent from other nodes
     * OUTBOUND messages are datagrams other agents request the DtnLink to send
     */
    enum MessageType {
        INBOUND,
        OUTBOUND
    }

    MessageType getMessageType() {
        if (nextHop == INBOUND_HOP) {
            return MessageType.INBOUND
        }
        return MessageType.OUTBOUND
    }

    void setDelivered() {
        delivered = true
    }
}

