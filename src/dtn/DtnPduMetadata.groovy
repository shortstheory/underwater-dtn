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
    DtnType.MessageType messageType
}

