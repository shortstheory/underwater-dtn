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

    DtnType.MessageType getMessageType() {
        if (nextHop != -1) {
            return DtnType.MessageType.OUTBOUND
        } else {
            return DtnType.MessageType.INBOUND
        }
    }
}

