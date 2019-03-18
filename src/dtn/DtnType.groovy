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

    static enum MessageType {
        PAYLOAD_SEGMENT,
        DATAGRAM
    }
}
