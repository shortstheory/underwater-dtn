package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked;

//@TypeChecked
@CompileStatic
class DtnPduMetadata {
    int nextHop
    int expiryTime
    int attempts
    boolean delivered
}
