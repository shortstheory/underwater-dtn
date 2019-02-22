package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked;

@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    int attempts
    boolean delivered
    boolean inTransit
}
