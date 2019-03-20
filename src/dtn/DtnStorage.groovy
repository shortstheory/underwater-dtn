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

    // PDU Structure
    // |TTL (24)| PAYLOAD (16)| PROTOCOL (8)|TOTAL_SEG (16) - SEGMENT_NUM (16)|
    // |TTL (24)| PROTOCOL (8)|TBC (1) PID (8) STARTPTR (31)|
    // no payload ID for messages which fit in the MTU

    /**
     * Pair of the new MessageID and old MessageID
     */
    private HashMap<String, String> datagramMap

    private static final int LOWER_8_BITMASK  = (int)0x000000FF
    private static final int LOWER_16_BITMASK = (int)0x0000FFFF
    private static final int LOWER_24_BITMASK = (int)0x00FFFFFF
    private static final int UPPER_16_BITMASK = (int)0xFFFF0000

    public static final String TTL_MAP            = "ttl"
    public static final String PROTOCOL_MAP       = "protocol"
    public static final String TBC_BIT_MAP        = "tbc"
    public static final String PAYLOAD_ID_MAP     = "pid"
    public static final String START_PTR_MAP      = "startptr"

    DtnStorage(DtnLink link, String dir) {
        directory = dir
        dtnLink = link

        File file = new File(directory)
        if (!file.exists()) {
            file.mkdir()
        }

        metadataMap = new HashMap<>()
        datagramMap = new HashMap<>()
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
                // one hack for cleaning MAX_RETRIES being exceeded
                metadata.expiryTime = 0
                continue
            }
            if (metadata.nextHop == nextHop) {
                data.add(messageID)
            }
        }
        return data
    }

    boolean saveFragment(int src, int payloadID, int protocol, int startPtr, int ttl, byte[] data) {
        String filename = Integer.toString(src) + "_" + Integer.toString(payloadID)
        File file = new File(directory, filename)

        byte[] fileBytes
        if (file.exists()) {
            byte[] payload = readPayload(src, payloadID)
            fileBytes = new byte[Math.min(payload.length, startPtr + data.length)]
            // copy the data
            int i = 0
            for (byte b : payload) {
                fileBytes[i++] = b
            }
            i = startPtr
            for (byte b : data) {
                fileBytes[i++] = b
            }
            FileOutputStream fos = new FileOutputStream(file)
            try {
                fos.write(fileBytes)
                return true
            } catch (IOException e) {
                return false
            } finally {
                fos.close()
            }
        } else {
            // FIXME: this isn't really the best structure for files, but it does what we need
            OutputPDU pdu = encodePdu(data, ttl, protocol, false, payloadID, 0)
            FileOutputStream fos = new FileOutputStream(file)
            // FIXME: we might have to add to payload tracking map here
            // Only thing the tracking map is doing here is maintaining TTL and delivered status
            metadataMap.put(filename, new DtnPduMetadata(nextHop: -1,
                                    expiryTime: (int)ttl, // this will not include transit time!!
                                    attempts: 0,
                                    delivered: false,
                                    payloadID: payloadID,
                                    bytesSent: 0))
            try {
                pdu.writeTo(fos)
                return true
            } catch (IOException e) {
                return false
            } finally {
                fos.close()
            }
        }


    }

    byte[] readPayload(int src, int payloadID) {
        String filename = Integer.toString(src) + "_" + Integer.toString(payloadID)
        return Files.readAllBytes(new File(directory, filename).toPath())
    }

    void deletePayload(int payloadID) {

    }

    boolean saveIncomingPayloadSegment(byte[] incomingSegment, int payloadID, int startPtr, int ttl, int segments) {
        try {
            String messageID = Integer.toString(payloadID) + "_" + Integer.toString(segmentNum)
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        } finally {
            fos.close()
        }
    }

    PayloadInfo.Status getPayloadStatus(int payloadID, DtnType.PayloadType type) {
        if (type == DtnType.PayloadType.INBOUND) {
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
        if (dtnLink.getMTU() < data.length) {
            return false
        }

        OutputPDU outputPDU = encodePdu(data, ttl, protocol, false, 0, 0)
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
                    bytesSent: 0))
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        } finally {
            fos.close()
        }
    }

    DtnType.MessageResult updateMaps(String messageID) {
        DtnPduMetadata metadata = getMetadata(messageID)
        if (metadata != null) {
            metadata.delivered = true
            outboundPayloads.removeSegment(metadata.payloadID, messageID)
            if (metadata.payloadID) {
                if (outboundPayloads.payloadTransferred(metadata.payloadID)) {
                    return DtnType.MessageResult.PAYLOAD_TRANSFERRED
                }
                return DtnType.MessageResult.PAYLOAD_SEGMENT
            }
        }
        return DtnType.MessageResult.DATAGRAM
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
                if (metadata.getMessageType() == DtnType.MessageType.DATAGRAM) {
                    dtnLink.sendFailureNtf(messageID, nextHop)
                } else if (metadata.getMessageType() == DtnType.MessageType.PAYLOAD_SEGMENT) {
                    if (inboundPayloads.exists(metadata.payloadID)) {
                        PayloadInfo info = removePayload(metadata.payloadID, DtnType.PayloadType.INBOUND)
                    } else if (outboundPayloads.exists(metadata.payloadID)) {
                        PayloadInfo info = removePayload(metadata.payloadID, DtnType.PayloadType.OUTBOUND)
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

    OutputPDU encodePdu(byte[] data, int ttl, int protocol, boolean tbc, int payloadID, int startPtr) {
        int dataLength = (data == null) ? 0 : data.length
        OutputPDU pdu = new OutputPDU(dataLength + dtnLink.HEADER_SIZE)
        pdu.write24(ttl)
        pdu.write8(protocol)
        int payloadFields
        payloadFields = (tbc) ? (1 << 32) : 0
        payloadFields |= (payloadID << 23)
        payloadFields |= startPtr
        pdu.write32(payloadFields)
        if (data != null) {
            pdu.write(data)
        }
        return pdu
    }

    int getPayloadID(String messageID) {
        Iterator it = datagramMap.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next()
            if (pair.getValue() == messageID) {
                return pair.getKey()
            }
        }
        int randomID = dtnLink.random.nextInt() & LOWER_8_BITMASK
        datagramMap.put(Integer.toString(randomID), messageID)
        return randomID
    }

    HashMap decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)
        HashMap<String, Integer> map = new HashMap<>()
        map.put(TTL_MAP, (int)pdu.read24())
        map.put(PROTOCOL_MAP, (int)pdu.read8())
        int payloadFields = (int)pdu.read32()
        int tbc = (payloadFields & 0x80000000) >> 31
        int payloadID = (payloadFields & 0x7F800000) >> 19
        int startPtr = (payloadFields & 0xFFFFFF)
        map.put(TBC_BIT_MAP, tbc)
        map.put(PAYLOAD_ID_MAP, payloadID)
        map.put(START_PTR_MAP, startPtr)
        return map
    }

    byte[] getDataFromPDU(byte[] pdu) {
        return Arrays.copyOfRange(pdu, dtnLink.HEADER_SIZE, pdu.length)
    }

    byte[] getPDUData(String messageID) {
        byte[] pduBytes = Files.readAllBytes(new File(directory, messageID).toPath())
        if (pduBytes != null) {
            return getDataFromPDU(pduBytes)
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

    PayloadInfo removePayload(int payloadID, DtnType.PayloadType type) {
        if (type == DtnType.PayloadType.INBOUND) {
            return inboundPayloads.removePayload(payloadID)
        } else if (type == DtnType.PayloadType.OUTBOUND) {
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
