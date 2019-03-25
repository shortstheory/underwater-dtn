package dtn

import groovy.transform.CompileStatic
import org.arl.unet.Parameter

@CompileStatic
enum DtnLinkParameters implements Parameter {
    beaconPeriod,
    sweepPeriod,
    datagramPeriod,
    linkExpiryTime,
    datagramPriority,
    linkPriority,
    discoveredNodes
}
