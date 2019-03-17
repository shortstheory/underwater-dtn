package dtn

import groovy.transform.CompileStatic

class PayloadInfo {
    String datagramID
    int segments
    HashSet<String> segmentSet

    enum Status {
        PENDING, SUCCESS, FAILURE
    }

    Status status

    PayloadInfo(int seg) {
        segmentSet = new HashSet<>()
        segments = seg
        status = Status.PENDING
    }

    void insertEntry(Integer segmentNumber, String messageID) {
        segmentSet.add(messageID)
    }

    void removeEntry(String messageID) {
        segmentSet.remove(messageID)
    }

    Status getStatus() {
        return status
    }
}

@CompileStatic
abstract class DtnPayloadTracker {
    HashMap<Integer, PayloadInfo> payloadMap
    DtnStorage storage

    DtnPayloadTracker(DtnStorage ds) {
        payloadMap = new HashMap<>()
        storage = ds
    }

    abstract void insertSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments)
    abstract boolean payloadTransferred(int payloadID)
    abstract PayloadInfo.Status getStatus(int payloadID)
}