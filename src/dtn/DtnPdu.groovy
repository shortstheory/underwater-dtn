package dtn

import org.arl.unet.PDU

class DtnPdu extends PDU {
    @Override
    protected void format() {
        length(11)

    }
}
