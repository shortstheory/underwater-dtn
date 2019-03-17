package dtn

import groovy.transform.CompileStatic

@CompileStatic
interface DtnPayloadTrackerInterface {
    DtnStorage storage
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
    }

    HashMap<Integer, PayloadInfo> payloadMap
    void insertSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments)
    boolean payloadTransferred(int payloadID)
}