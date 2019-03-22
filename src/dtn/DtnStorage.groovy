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
    // **NEW** - |TTL (24)| PROTOCOL (8)|TBC (1) PID (8) STARTPTR (23)|
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
                || metadata.delivered
                || metadata.getMessageType() == DtnPduMetadata.MessageType.OUTBOUND) {
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
            fileBytes = new byte[Math.max(payload.length, startPtr + data.length + dtnLink.HEADER_SIZE)]
            // copy the data
            int i = 0
            for (byte b : payload) {
                fileBytes[i++] = b
            }
            i = startPtr + dtnLink.HEADER_SIZE
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
            // Only thing the tracking map is doing here is maintaining TTL and delivered status
            metadataMap.put(filename, new DtnPduMetadata(-1, ttl + dtnLink.currentTimeSeconds()))
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

    void deletePayload(int src, int payloadID) {
        String filename = Integer.toString(src) + "_" + Integer.toString(payloadID)
        File file = new File(directory, filename)
        file.delete()
        // FIXME: on TTL expiry we have to make sure that this method is called
        // No DDN/DFN required if this is deleted
        // so behave the same as regular message delivery as well
    }

    // dumb mistake for sure
    boolean saveDatagram(DatagramReq req) {
        int protocol = req.getProtocol()
        int nextHop = req.getTo()
        // FIXME: only for testing with Router
        int ttl = (Math.round(req.getTtl()) & LOWER_24_BITMASK)
        String messageID = req.getMessageID()
        byte[] data = req.getData()
        int mtu = dtnLink.getMTU()
        if (data != null && dtnLink.getMTU() < data.length) {
            return false
        }

        OutputPDU outputPDU = encodePdu(data, ttl, protocol, false, 0, 0)
        File file = new File(directory, messageID)
        FileOutputStream fos = new FileOutputStream(file)
        try {
            outputPDU.writeTo(fos)
            metadataMap.put(messageID, new DtnPduMetadata(nextHop, ttl + dtnLink.currentTimeSeconds()))
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        } finally {
            fos.close()
        }
    }

    void setDelivered(String messageID) {
        metadataMap.get(messageID).delivered = true
    }

    void deleteFile(String messageID, DtnPduMetadata metadata) {
        File file = new File(directory, messageID)
        file.delete()
        // remove from tracking map
        Iterator it = datagramMap.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next()
            if (entry.getValue() == messageID) {
                it.remove()
                break
            }
        }
        // If TTL'ed send the appropriate ntf
        if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
            if (metadata.getMessageType() == DtnPduMetadata.MessageType.OUTBOUND) {
                dtnLink.sendFailureNtf(messageID, metadata.nextHop)
            }
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
        OutputPDU pdu = new OutputPDU(dataLength + DtnLink.HEADER_SIZE)
        pdu.write24(ttl)
        pdu.write8(protocol)
        int payloadFields
        payloadFields = (tbc) ? (1 << 31) : 0
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
                return Integer.valueOf((String)pair.getKey())
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
        map.put(DtnStorage.TTL_MAP, (int)pdu.read24())
        map.put(DtnStorage.PROTOCOL_MAP, (int)pdu.read8())
        int payloadFields = (int)pdu.read32()
        int tbc = ((payloadFields & 0x80000000).toInteger() >>> 31)
        int payloadID = ((payloadFields & 0x7F800000) >>> 23)
        int startPtr = (payloadFields & 0x007FFFFF)
        map.put(DtnStorage.TBC_BIT_MAP, tbc)
        map.put(DtnStorage.PAYLOAD_ID_MAP, payloadID)
        map.put(DtnStorage.START_PTR_MAP, startPtr)
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

    void removeTracking(String newMessageID) {
        datagramMap.remove(newMessageID)
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
