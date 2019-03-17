package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnInboundPayloadTracker extends DtnPayloadTracker {
    DtnInboundPayloadTracker(DtnStorage ds) {
        super(ds)
    }

    void setPayloadMTU(int payloadID, int mtu) {
        payloadMap.get(payloadID).setMTU(mtu)
    }

    void insertSegment(Integer payloadID, String segmentID, int segmentNumber, int segments) {
        if (payloadID != 0) {
            if (payloadMap.get(payloadID) == null) {
                payloadMap.put(payloadID, new PayloadInfo(segments))
            }
            payloadMap.get(payloadID).insertEntry(segmentNumber, segmentID)
            payloadTransferred(payloadID)
        }
    }

    @Override
    boolean payloadTransferred(int payloadID) {
        PayloadInfo payload = payloadMap.get(payloadID)
        if (payload.segmentSet.size() == payload.segments) {
            payload.status = PayloadInfo.Status.SUCCESS
            return true
        }
        payload.status = PayloadInfo.Status.PENDING
        return false
    }

    @Override
    PayloadInfo.Status getStatus(int payloadID) {
        return payloadMap.get(payloadID).getStatus()
    }

    byte[] reassemblePayloadData(int payloadID) {
        PayloadInfo payload = payloadMap.get(payloadID)
        int mtu = payload.MTU
        byte[] payloadData = new byte[payload.segments*mtu]
        for (String id : payload.segmentSet) {
            int segmentNumber = storage.getMetadata(id).segmentNumber
            int base = (segmentNumber-1)*mtu
            byte[] segmentData = storage.getPDUData(id)
            for (int i = 0; i < segmentData.length; i++) {
                payloadData[i+base] = segmentData[i]
            }
        }
        return payloadData
    }
}
