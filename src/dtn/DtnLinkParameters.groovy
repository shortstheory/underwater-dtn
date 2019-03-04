package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.arl.unet.Parameter;

@com.google.gson.annotations.JsonAdapter(org.arl.unet.JsonTypeAdapter.class)
@CompileStatic
enum DtnLinkParameters implements Parameter {
    BEACON_PERIOD,
    SWEEP_PERIOD,
    DATAGRAM_PERIOD,
    MAX_RETRIES,
    LINK_EXPIRY_TIME
}
