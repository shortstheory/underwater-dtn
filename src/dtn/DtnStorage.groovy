package dtn

import com.sun.istack.internal.Nullable
import groovy.transform.CompileStatic
import org.arl.unet.DatagramReq
import org.arl.unet.InputPDU
import org.arl.unet.OutputPDU

import java.nio.file.Files

/**
 * Helper class for managing the datagrams saved to the node's non-volatile storage
 * Manages the deletion of expired datagrams and encoding datagrams with PDU headers
 */
@CompileStatic
class DtnStorage {
    private final String directory
    private DtnLink dtnLink
    private HashMap<String, DtnPduMetadata> metadataMap

    // PDU Structure
    // **NEW** - |TTL (24)| PROTOCOL (8)|TBC (1) PID (8) STARTPTR (23)|

    private static final int LOWER_8_BITMASK  = (int)0x000000FF
    private static final int LOWER_24_BITMASK = (int)0x00FFFFFF

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
    }

    @Nullable DtnPduMetadata getMetadata(String messageID) {
        return metadataMap.get(messageID)
    }

    byte[] readPayload(int src, int payloadID) {
        String filename = Integer.toString(src) + "_" + Integer.toString(payloadID)
        return Files.readAllBytes(new File(directory, filename).toPath())
    }

    byte[] getPDUData(byte[] pdu) {
        return Arrays.copyOfRange(pdu, dtnLink.HEADER_SIZE, pdu.length)
    }

    @Nullable byte[] getMessageData(String messageID) {
        byte[] pduBytes = Files.readAllBytes(new File(directory, messageID).toPath())
        if (pduBytes != null) {
            return getPDUData(pduBytes)
        }
        return null
    }

    @Nullable HashMap getPDUInfo(String messageID) {
        try {
            byte[] pduBytes = Files.readAllBytes(new File(directory, messageID).toPath())
            if (pduBytes != null) {
                return decodePdu(pduBytes)
            }
            return null
        } catch(Exception e) {
            println "Message ID " + messageID + " not found " + dtnLink.currentTimeSeconds()
            return null
        }
    }

    ArrayList<String> getNextHopDatagrams(int nextHop) {
        ArrayList<String> data = new ArrayList<>()
        for (Map.Entry<String, DtnPduMetadata> entry : metadataMap.entrySet()) {
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime
                || metadata.delivered
                || metadata.getMessageType() == DtnPduMetadata.MessageType.INBOUND) {
                // we don't delete here, as it will complicate the logic
                // instead, it will be deleted by the next DtnLink sweep
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

    boolean saveDatagram(DatagramReq req) {
        int protocol = req.getProtocol()
        int nextHop = req.getTo()
        // FIXME: only for testing with Router
        int ttl = (Math.round(req.getTtl()))
        String messageID = req.getMessageID()
        byte[] data = req.getData()
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

    void deleteFile(String messageID, DtnPduMetadata metadata) {
        File file = new File(directory, messageID)
        file.delete()
        if (metadata.getMessageType() == DtnPduMetadata.MessageType.OUTBOUND) {
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
                if (metadata.getMessageType() == DtnPduMetadata.MessageType.OUTBOUND) {
                    dtnLink.sendFailureNtf(messageID, metadata.nextHop)
                }
            }
        }
    }

    void deleteFiles() {
        Iterator it = metadataMap.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next()
            String messageID = (String)entry.getKey()
            DtnPduMetadata metadata = (DtnPduMetadata)entry.getValue()
            if (metadata.delivered || dtnLink.currentTimeSeconds() > metadata.expiryTime) { // put the deletion logic here!
                deleteFile(messageID, metadata)
                it.remove()
            }
        }
    }

    static OutputPDU encodePdu(byte[] data, int ttl, int protocol, boolean tbc, int payloadID, int startPtr) {
        int dataLength = (data == null) ? 0 : data.length
        OutputPDU pdu = new OutputPDU(dataLength + DtnLink.HEADER_SIZE)
        pdu.write24(ttl & LOWER_24_BITMASK)
        pdu.write8(protocol)
        int payloadFields
        payloadFields = (tbc) ? (1 << 31) : 0
        payloadFields |= ((payloadID & LOWER_8_BITMASK) << 23)
        payloadFields |= startPtr
        pdu.write32(payloadFields)
        if (data != null) {
            pdu.write(data)
        }
        return pdu
    }

    static HashMap decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)
        HashMap<String, Integer> map = new HashMap<>()
        map.put(TTL_MAP, (int)pdu.read24())
        map.put(PROTOCOL_MAP, (int)pdu.read8())
        int payloadFields = (int)pdu.read32()
        int tbc = ((payloadFields & 0x80000000).toInteger() >>> 31)
        int payloadID = ((payloadFields & 0x7F800000) >>> 23)
        int startPtr = (payloadFields & 0x007FFFFF)
        map.put(TBC_BIT_MAP, tbc)
        map.put(PAYLOAD_ID_MAP, payloadID)
        map.put(START_PTR_MAP, startPtr)
        return map
    }

    int getArrivalTime(String messageID) {
        HashMap<String, Integer> map = getPDUInfo(messageID)
        if (map != null) {
            int ttl = map.get(TTL_MAP)
            int expiryTime = getMetadata(messageID).expiryTime
            return expiryTime - ttl
        }
        return -1
    }
}
