package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked;

@TypeChecked
@CompileStatic
class DtnPDUMetadata {
    int nextHop
    int expiryTime
}
