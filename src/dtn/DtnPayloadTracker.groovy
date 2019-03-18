package dtn

import groovy.transform.CompileStatic

class PayloadInfo {
    String datagramID
    int segments
    HashSet<String> segmentSet
    int MTU

    enum Status {
        PENDING, SUCCESS, FAILURE
    }

    Status status

    PayloadInfo(int seg) {
        segmentSet = new HashSet<>()
        segments = seg
        status = Status.PENDING
    }

    void setMTU(int mtu) {
        MTU = mtu
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

    abstract boolean payloadTransferred(int payloadID)
    abstract PayloadInfo.Status getStatus(int payloadID)
    boolean exists(Integer payloadID) {
        if (payloadMap.get(payloadID) != null) {
            return true
        }
        return false
    }
    PayloadInfo removePayload(Integer payloadID) {
        return payloadMap.remove(payloadID)
    }
}