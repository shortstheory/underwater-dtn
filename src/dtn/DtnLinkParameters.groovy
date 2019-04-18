package dtn

import groovy.transform.CompileStatic
import org.arl.unet.Parameter

/**
 * Parameters used by the DtnLink agent
 */
@CompileStatic
enum DtnLinkParameters implements Parameter {
    beaconTimeout,
    GCPeriod,
    datagramPeriod,
    linkExpiryTime,
    datagramPriority,
    linkPriority,
    discoveredNodes,
    shortCircuit
}
