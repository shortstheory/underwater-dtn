package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.arl.unet.Parameter;

@com.google.gson.annotations.JsonAdapter(org.arl.unet.JsonTypeAdapter.class)
//@TypeChecked
@CompileStatic
enum DtnLinkParameters implements Parameter {
    BEACON_DURATION,
    SWEEP_DURATION,
    MTU
}
