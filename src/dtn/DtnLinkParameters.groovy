package dtn

import groovy.transform.CompileStatic
import org.arl.unet.Parameter

@CompileStatic
enum DtnLinkParameters implements Parameter {
    beaconTimeout,
    GCPeriod,
    datagramPeriod,
    linkExpiryTime,
    datagramPriority,
    linkPriority,
    discoveredNodes
}
