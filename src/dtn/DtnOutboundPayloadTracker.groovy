package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnOutboundPayloadTracker implements DtnPayloadTrackerInterface {
    DtnOutboundPayloadTracker(DtnStorage ds) {
        payloadMap = new HashMap<>()
        storage = ds
    }

    @Override
    void insertSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments) {
        if (payloadID != 0) {
            if (payloadMap.get(payloadID) == null) {
                payloadMap.put(payloadID, new PayloadInfo(segments))
                payloadMap.get(payloadID).datagramID = payloadMessageID
            }
            payloadMap.get(payloadID).insertEntry(segmentNumber, segmentID)
        }
    }

    @Override
    boolean payloadTransferred(int payloadID) {
        PayloadInfo payload = payloadMap.get(payloadID)
        if (payload.segmentSet.isEmpty()) {
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

    void removeSegment(Integer payloadID, String segmentID) {
        if (payloadID != 0) {
            payloadMap.get(payloadID).removeEntry(segmentID)
        }
    }
}
