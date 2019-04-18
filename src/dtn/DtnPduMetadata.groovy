package dtn

import groovy.transform.CompileStatic

/**
 * Used for storing the TTL, bytes sent, next hop, and delivery status of each datagram in the node's volatile memory
 */
@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    boolean delivered
    int bytesSent
    int size

    public static final int INBOUND_HOP = -1

    /**
     * INBOUND messages are in-progress fragments of Payloads which are sent from other nodes
     * OUTBOUND messages are datagrams other agents request the DtnLink to send
     */
    enum MessageType {
        INBOUND,
        OUTBOUND
    }

    DtnPduMetadata(int hop, int expiry) {
        bytesSent = 0
        delivered = false
        nextHop = hop
        expiryTime = expiry
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

