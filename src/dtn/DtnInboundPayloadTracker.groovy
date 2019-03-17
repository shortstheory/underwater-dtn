package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnInboundPayloadTracker extends DtnPayloadTracker {
    HashMap<Integer, PayloadInfo> payloadMap
    DtnStorage storage

    DtnInboundPayloadTracker(DtnStorage ds) {
        super(ds)
    }

    @Override
    void insertSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments) {
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
        int minMtu = 1500
        PayloadInfo payload = payloadMap.get(payloadID)
        byte[] payloadData = new byte[payload.segments*minMtu]
        for (String id : payload.segmentSet) {
            int segmentNumber = storage.getMetadata(id).segmentNumber
            int base = (segmentNumber-1)*minMtu
            byte[] segmentData = storage.getPDUData(id)
            for (int i = 0; i < segmentData.length; i++) {
                payloadData[i+base] = segmentData[i]
            }
        }
        return payloadData
    }
}
