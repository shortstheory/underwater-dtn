package dtn

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
        return (payloadID == 0) ? false : payloadMap.get(payloadID).outboundPayloadTransferred()
    }

    void removeSegment(Integer payloadID, String segmentID) {
        if (payloadID != 0) {
            payloadMap.get(payloadID).removePendingEntry(segmentID)
        }
    }
}
