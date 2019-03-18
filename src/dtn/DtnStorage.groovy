package dtn

import groovy.transform.CompileStatic
import org.arl.unet.DatagramReq
import org.arl.unet.InputPDU
import org.arl.unet.OutputPDU

import java.nio.file.Files

@CompileStatic
class DtnStorage {
    private final String directory
    private DtnLink dtnLink
    private HashMap<String, DtnPduMetadata> metadataMap

    enum MessageType {
        DATAGRAM,
        PAYLOAD_SEGMENT,
        PAYLOAD_TRANSFERRED
    }

    enum PayloadType {
        INBOUND,
        OUTBOUND
    }
//
    private DtnOutboundPayloadTracker outboundPayloads
    private DtnInboundPayloadTracker inboundPayloads
    // PDU Structure
    // |TTL (32)| PAYLOAD (16)| PROTOCOL (8)|TOTAL_SEG (16) - SEGMENT_NUM (16)|
    // no payload ID for messages which fit in the MTU

    /**
     * Pair of the new MessageID and old MessageID
     */
    private HashMap<String, String> datagramMap

    private static final int LOWER_16_BITMASK = (int)0x0000FFFF
    private static final int LOWER_24_BITMASK = (int)0x00FFFFFF
    private static final int UPPER_16_BITMASK = (int)0xFFFF0000

    public static final String TTL_MAP            = "ttl"
    public static final String PROTOCOL_MAP       = "protocol"
    public static final String PAYLOAD_ID_MAP     = "pid"
    public static final String SEGMENT_NUM_MAP    = "segnum"
    public static final String TOTAL_SEGMENTS_MAP = "totseg"

    DtnStorage(DtnLink link, String dir) {
        directory = dir
        dtnLink = link

        File file = new File(directory)
        if (!file.exists()) {
            file.mkdir()
        }

        metadataMap = new HashMap<>()
        datagramMap = new HashMap<>()
        outboundPayloads = new DtnOutboundPayloadTracker(this)
        inboundPayloads = new DtnInboundPayloadTracker(this)
        // FIXME: this causes a compiler bug!!!
//        outboundPayloads = new DtnPayloadTracker<>()
    }

    void trackDatagram(String newMessageID, String oldMessageID) {
        getMetadata(oldMessageID).attempts++
        datagramMap.put(newMessageID, oldMessageID)
    }

    DtnPduMetadata getDatagramMetadata(String messageID) {
        return metadataMap.get(messageID)
    }

    String getOriginalMessageID(String newMessageID) {
        return datagramMap.get(newMessageID)
    }

    ArrayList<String> getNextHopDatagrams(int nextHop) {
        ArrayList<String> data = new ArrayList<>()
        for (Map.Entry<String, DtnPduMetadata> entry : metadataMap.entrySet()) {
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime
                || metadata.delivered) {
                // we don't delete here, as it will complicate the logic
                // instead, it will be deleted by the next DtnLink sweep
                continue
            } else if (metadata.attempts > dtnLink.MAX_RETRIES) {
                // one hack for cleaning MAX_RETRIES being exceeded.
                // this won't cause a DFN because it will be caught in the
                // check for message delivery
                metadata.expiryTime = 0
                continue
            }
            if (metadata.nextHop == nextHop) {
                data.add(messageID)
            }
        }
        return data
    }

    boolean saveIncomingPayloadSegment(byte[] incomingSegment, int payloadID, int segmentNum, int ttl, int segments) {
        String messageID = Integer.toString(payloadID) + "_" + Integer.toString(segmentNum)
        FileOutputStream fos
        try {
            File dir = new File(directory)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            File file = new File(dir, messageID)
            fos = new FileOutputStream(file)
            fos.write(incomingSegment)
            // this is just for holding its metadata and deleting it on TTL,
            // it could have it's own too I guess
            metadataMap.put(messageID, new DtnPduMetadata(nextHop: -1,
                                                            expiryTime: (int)ttl, // this will not include transit time!!
                                                            attempts: 0,
                                                            delivered: false,
                                                            payloadID: payloadID,
                                                            segmentNumber: segmentNum))
//            void insertOutboundPayloadSegment(String payloadMessageID, Integer payloadID, String segmentID, int segmentNumber, int segments) {

            inboundPayloads.insertSegment(payloadID, messageID, segmentNum, segments)
            inboundPayloads.setPayloadMTU(payloadID, dtnLink.getMTU())
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        } finally {
            fos.close()
        }
    }

    PayloadInfo.Status getPayloadStatus(int payloadID, PayloadType type) {
        if (type == PayloadType.INBOUND) {
            return inboundPayloads.getStatus(payloadID)
        } else {
            return outboundPayloads.getStatus(payloadID)
        }
    }

    byte[] getPayloadData(int payloadID) {
        return inboundPayloads.reassemblePayloadData(payloadID)
    }

    // Fragment PDUs can't be tracked normally lah!!
    // dumb mistake for sure
    boolean saveDatagram(DatagramReq req) {
        int protocol = req.getProtocol()
        int nextHop = req.getTo()
        // FIXME: only for testing with Router
        int ttl = (Math.round(req.getTtl()) & LOWER_24_BITMASK)
        String messageID = req.getMessageID()
        byte[] data = req.getData()
        int minMTU = dtnLink.getMTU()
        int segments = (data == null) ? 1 : (int)Math.ceil((double)data.length/minMTU)

        if (segments > 0xFFFF) {
//          too many segments lah! can't send message!
            return false
        }

        if (segments == 1) {
            OutputPDU outputPDU = encodePdu(data, ttl, protocol, 0, 0, 0)
            FileOutputStream fos
            try {
                File dir = new File(directory)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                File file = new File(dir, messageID)
                fos = new FileOutputStream(file)
                outputPDU.writeTo(fos)
                metadataMap.put(messageID, new DtnPduMetadata(nextHop: nextHop,
                        expiryTime: (int)ttl + dtnLink.currentTimeSeconds(),
                        attempts: 0,
                        delivered: false,
                        payloadID: 0))
                return true
            } catch (IOException e) {
                println "Could not save file for " + messageID
                return false
            } finally {
                fos.close()
            }
        } else {
            int payloadID = dtnLink.random.nextInt() & LOWER_16_BITMASK

            for (int i = 0; i < segments; i++) {
                byte[] segmentData
                int segmentNumber = i + 1
                int startPtr = i * minMTU
                int endPtr = (minMTU < (data.length - startPtr)) ? (segmentNumber) * minMTU : data.length
                segmentData = Arrays.copyOfRange(data, startPtr, endPtr)

                FileOutputStream fos
                try {
                    String segmentID = Integer.toString(payloadID) + "_" + Integer.toString(i) // nice and simple scheme
                    OutputPDU outputPDU = encodePdu(segmentData, ttl, protocol, payloadID, segmentNumber, segments)
                    File dir = new File(directory)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    File file = new File(dir, segmentID)
                    fos = new FileOutputStream(file)
                    outputPDU.writeTo(fos)
                    metadataMap.put(segmentID, new DtnPduMetadata(nextHop: nextHop,
                            expiryTime: (int) ttl + dtnLink.currentTimeSeconds(),
                            attempts: 0,
                            delivered: false,
                            payloadID: payloadID))
                    outboundPayloads.insertSegment(messageID, payloadID, segmentID, segmentNumber, segments)
                } catch (IOException e) {
                    println "Could not payload file for " + messageID + " / " + payloadID
                    return false
                } finally {
                    fos.close()
                }
            }
            println("done")
            return true
        }
    }

    MessageType updateMaps(String messageID) {
        DtnPduMetadata metadata = getMetadata(messageID)
        if (metadata != null) {
            metadata.delivered = true
            outboundPayloads.removeSegment(metadata.payloadID, messageID)
            if (metadata.payloadID) {
                if (outboundPayloads.payloadTransferred(metadata.payloadID)) {
                    return MessageType.PAYLOAD_TRANSFERRED
                }
                return MessageType.PAYLOAD_SEGMENT
            }
        }
        return MessageType.DATAGRAM
    }

    String getPayloadDatagramID(int payloadID) {
        return outboundPayloads.payloadMap.get(payloadID).datagramID
    }

    void payloadDelivered(int payloadID) {
        for (String id : inboundPayloads.payloadMap.get(payloadID).segmentSet) {
            metadataMap.get(id).delivered = true
        }
    }

    void deleteFile(String messageID, DtnPduMetadata metadata) {
        try {
            File file = new File(directory, messageID)
            file.delete()
            int nextHop = metadata.nextHop
            Iterator it = datagramMap.entrySet().iterator()

            if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
                if (metadata.getMessageType() == dtn.MessageType.DATAGRAM) {
                    dtnLink.sendFailureNtf(messageID, nextHop)
                } else if (metadata.getMessageType() == dtn.MessageType.PAYLOAD_SEGMENT) {
                    if (inboundPayloads.exists(metadata.payloadID)) {
                        PayloadInfo info = removePayload(metadata.payloadID, PayloadType.INBOUND)
                    } else if (outboundPayloads.exists(metadata.payloadID)) {
                        PayloadInfo info = removePayload(metadata.payloadID, PayloadType.OUTBOUND)
                        dtnLink.sendFailureNtf(info.datagramID, metadata.nextHop)
                    }
                }
            }

            // removes from tracking map
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next()
                if (entry.getValue() == messageID) {
                    it.remove()
                    return
                }
            }
        } catch (Exception e) {
            println "Could not delete file for " + messageID + " files " + datagramMap.size() + "/" + metadataMap.size()
        }
    }

    void deleteFiles() {
        Iterator it = metadataMap.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next()
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (metadata.delivered || dtnLink.currentTimeSeconds() > metadata.expiryTime) { // put the deletion logic here!
                deleteFile(messageID, metadata)
                it.remove()
            }
        }
    }

    OutputPDU encodePdu(byte[] data, int ttl, int protocol, int payloadId, int segmentNumber, int totalSegments) {
        int dataLength = (data == null) ? 0 : data.length
        OutputPDU pdu = new OutputPDU(dataLength + dtnLink.HEADER_SIZE)
        pdu.write24(ttl)
        pdu.write8(protocol)
        pdu.write16(payloadId)
        pdu.write16(segmentNumber)
        pdu.write16(totalSegments)
        if (data != null) {
            pdu.write(data)
        }
        return pdu
    }

    HashMap decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)
        HashMap<String, Integer> map = new HashMap<>()
        map.put(TTL_MAP, (int)pdu.read24())
        map.put(PROTOCOL_MAP, (int)pdu.read8())
        map.put(PAYLOAD_ID_MAP, (int)pdu.read16() & 0x0000FFFF)
        map.put(SEGMENT_NUM_MAP, (int)pdu.read16())
        map.put(TOTAL_SEGMENTS_MAP, (int)pdu.read16())
        return map
    }

    byte[] getPDUData(String messageID) {
        byte[] pduBytes = Files.readAllBytes(new File(directory, messageID).toPath())
        if (pduBytes != null) {
            return Arrays.copyOfRange(pduBytes, dtnLink.HEADER_SIZE, pduBytes.length)
        }
    }

    HashMap getParsedPDU(String messageID) {
        try {
            byte[] pduBytes = Files.readAllBytes(new File(directory, messageID).toPath())
            if (pduBytes != null) {
                return decodePdu(pduBytes)
            }
            return null
        } catch(Exception e) {
            println "Message ID " + messageID + " not found"
            return null
        }
    }

    DtnPduMetadata getMetadata(String messageID) {
        return metadataMap.get(messageID)
    }

    void removeFailedEntry(String newMessageID) {
        datagramMap.remove(newMessageID)
    }

    PayloadInfo removePayload(int payloadID, PayloadType type) {
        if (type == PayloadType.INBOUND) {
            return inboundPayloads.removePayload(payloadID)
        } else {
            return outboundPayloads.removePayload(payloadID)
        }
    }

    int getArrivalTime(String messageID) {
        HashMap<String, Integer> map = getParsedPDU(messageID)
        if (map != null) {
            int ttl = map.get(TTL_MAP)
            int expiryTime = getMetadata(messageID).expiryTime
            return expiryTime - ttl
        }
        return -1
    }
}
