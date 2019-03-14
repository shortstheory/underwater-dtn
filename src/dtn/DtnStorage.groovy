package dtn

import groovy.transform.CompileStatic
import org.arl.unet.DatagramReq
import org.arl.unet.InputPDU
import org.arl.unet.OutputPDU
import org.arl.unet.PDU

import java.lang.reflect.Array
import java.nio.file.Files

@CompileStatic
class DtnStorage {
    private final String directory
    private DtnLink dtnLink
    private HashMap<String, DtnPduMetadata> metadataMap
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

    boolean saveDatagram(DatagramReq req) {
        int protocol = req.getProtocol()
        int nextHop = req.getTo()
        // FIXME: only for testing with Router
        int ttl = (Math.round(req.getTtl()) & LOWER_24_BITMASK)
        String messageID = req.getMessageID()
        byte[] data = req.getData()
        int minMTU = dtnLink.getMinMTU()
        int segments = (data == null) ? 1 : (int)Math.ceil((double)data.length/minMTU)
        int payloadId = (segments > 1) ? dtnLink.random.nextInt() & LOWER_16_BITMASK : 0

        if (segments > 0xFFFF) {
//          too many segments lah! can't send message!
            return false
        }

        for (int i = 0; i < segments; i++) {
            byte[] segmentData = null
            int startPtr = i*minMTU
            if (data != null) {
                int endPtr = (minMTU < (data.length - startPtr)) ? (i * 1) * minMTU : data.length
                segmentData = (data == null) ? null : Arrays.copyOfRange(data, startPtr, endPtr)
            }
            OutputPDU outputPDU = encodePdu(segmentData, ttl, protocol, payloadId, i+1, segments)

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
                        expiryTime: (int) ttl + dtnLink.currentTimeSeconds(),
                        attempts: 0,
                        delivered: false))
                return true
            } catch (IOException e) {
                println "Could not save file for " + messageID
                return false
            } finally {
                fos.close()
            }
        }
    }

    Tuple2 deleteFile(String messageID) {
        int nextHop
        try {
            File file = new File(directory, messageID)
            file.delete()
            nextHop = getMetadata(messageID).nextHop
            String key

            // Can be done in O(1) with bi-map? But not a big deal
            for (Map.Entry<String, String> entry : datagramMap.entrySet()) {
                if (entry.getValue() == messageID) {
                    key = entry.getKey()
                    break
                }
            }
            datagramMap.remove(key)
        } catch (Exception e) {
            println "Could not delete file for " + messageID + " files " + datagramMap.size() + "/" + metadataMap.size()
        }
        return new Tuple2(messageID, nextHop)
    }

    ArrayList<Tuple2> deleteExpiredDatagrams() {
        ArrayList<Tuple2> expiredDatagrams = new ArrayList<>()

        Iterator it = metadataMap.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next()
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (metadata.delivered) {
                deleteFile(messageID)
                it.remove()
            } else if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
                expiredDatagrams.add(deleteFile(messageID))
                it.remove()
            }
        }
        return expiredDatagrams
    }

    void setDelivered(String messageID) {
        if (getMetadata(messageID) != null) {
            getMetadata(messageID).delivered = true
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
        map.put(PAYLOAD_ID_MAP, (int)pdu.read16())
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

    int getArrivalTime(String messageID) {
        HashMap<String, Integer> map = getParsedPDU(messageID)
        if (map != null) {
            int ttl = map.get(TTL_MAP)
            int expiryTime = getMetadata(messageID).expiryTime
            return expiryTime - ttl
        }
        return -1
    }

    int getTimeSinceArrival(String messageID) {
        int arrivalTime
        return ((arrivalTime = getArrivalTime(messageID)) > 0) ? dtnLink.currentTimeSeconds() - arrivalTime : -1
    }
}
