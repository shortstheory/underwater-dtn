package dtn

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

    HashMap<Integer, PayloadInfo> payloadMap
    void insertSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments)
    boolean payloadTransferred(int payloadID)
}