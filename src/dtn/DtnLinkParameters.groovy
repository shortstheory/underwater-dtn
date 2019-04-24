/***************************************************************************
 *  Copyright (C) 2019 by Arnav Dhamija <arnav.dhamija@gmail.com>          *
 *  Distributed under the MIT License (http://opensource.org/licenses/MIT) *
 ***************************************************************************/

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
