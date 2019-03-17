package dtn

class DtnInboundPayloadTracker implements DtnPayloadTrackerInterface {
    DtnInboundPayloadTracker(DtnStorage ds) {
        payloadMap = new HashMap<>()
        storage = ds
    }

    @Override
    void insertSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments) {
        if (payloadID != 0) {
            if (payloadMap.get(payloadID) == null) {
                payloadMap.put(payloadID, new PayloadInfo(segments))
            }
            payloadMap.get(payloadID).insertEntry(segmentNumber, messageID)
            payloadMap.get(payloadID).inboundPayloadTransferred()
        }
    }

    @Override
    boolean payloadTransferred(int payloadID) {
        return false
    }
}
