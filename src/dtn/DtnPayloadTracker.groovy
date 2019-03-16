package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnPayloadTracker {
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

        void removePendingEntry(String messageID) {
            segmentSet.remove(messageID)
        }

        boolean outboundPayloadTransferred() {
            if (segmentSet.isEmpty()) {
                status = Status.SUCCESS
                return true
            } else {
                status = Status.PENDING
                return false
            }
        }

        boolean inboundPayloadTransferred() {
            if (segmentSet.size() == segments) {
                status = Status.SUCCESS
                return true
            } else {
                status = Status.PENDING
                return false
            }
        }

        byte[] reassemblePayloadData() {
            int minMtu = 1500
            byte[] payloadData = new byte[segments*minMtu]
            for (String id : segmentSet) {
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

    // PAYLOAD-ID - and its DATAGRAMS
    HashMap<Integer, PayloadInfo> payloadMap

    DtnPayloadTracker(DtnStorage ds) {
        payloadMap = new HashMap<>()
        storage = ds
    }

    void insertOutboundPayloadSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments) {
        if (payloadID != 0) {
            if (payloadMap.get(payloadID) == null) {
                payloadMap.put(payloadID, new PayloadInfo(segments))
                payloadMap.get(payloadID).datagramID = payloadMessageID
            }
            payloadMap.get(payloadID).insertEntry(segmentNumber, segmentID)
        }
    }

    void insertInboundPayloadSegment(Integer payloadID, String messageID, int segmentNumber, int segments) {
        if (payloadID != 0) {
            if (payloadMap.get(payloadID) == null) {
                payloadMap.put(payloadID, new PayloadInfo(segments))
            }
            payloadMap.get(payloadID).insertEntry(segmentNumber, messageID)
            payloadMap.get(payloadID).inboundPayloadTransferred()
        }
    }

    void removePendingSegment(Integer payloadID, String segmentID) {
        if (payloadID != 0) {
            payloadMap.get(payloadID).removePendingEntry(segmentID)
        }
    }

    boolean payloadTransferred(Integer payloadID) {
        return (payloadID == 0) ? false : payloadMap.get(payloadID).outboundPayloadTransferred()
    }
}