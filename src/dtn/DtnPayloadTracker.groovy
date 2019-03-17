package dtn

import groovy.transform.CompileStatic

@CompileStatic
class DtnPayloadTracker {

    // PAYLOAD-ID - and its DATAGRAMS

    DtnPayloadTracker(DtnStorage ds) {
        payloadMap = new HashMap<>()
        storage = ds
    }

    void insertOutboundPayloadSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments) {

    }

    void insertInboundPayloadSegment(Integer payloadID, String messageID, int segmentNumber, int segments) {

    }


    boolean payloadTransferred(Integer payloadID) {
    }
}