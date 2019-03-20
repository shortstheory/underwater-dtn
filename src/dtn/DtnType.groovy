package dtn

class DtnType {
    static enum MessageResult {
        DATAGRAM,
        PAYLOAD_SEGMENT,
        PAYLOAD_TRANSFERRED
    }

    static enum PayloadType {
        INBOUND,
        OUTBOUND
    }

    // FIXME: Change the names of these message types
    static enum MessageType {
        PAYLOAD,
        DATAGRAM
    }
}
