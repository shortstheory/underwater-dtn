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

    /**
     * Pair of the new MessageID and old MessageID
     */
    private HashMap<String, String> datagramMap

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
        int ttl = (req.getTtl().isNaN()) ? 2000 : Math.round(req.getTtl())
        String messageID = req.getMessageID()
        byte[] data = req.getData()
        OutputPDU outputPDU = encodePdu(data, ttl, protocol)

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
                                                 delivered: false))
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        } finally {
            fos.close()
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

    OutputPDU encodePdu(byte[] data, int ttl, int protocol) {
        int dataLength = (data == null) ? 0 : data.length
        OutputPDU pdu = new OutputPDU(dataLength + dtnLink.HEADER_SIZE)
        pdu.write32(ttl)
        pdu.write8(protocol)
        pdu.write(data)
        return pdu
    }

    // FIXME: make Public Static?
    Tuple decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)

        int ttl = (int)pdu.read32()
        int protocol = (int)pdu.read8()
        // the data follows the 8 byte header
        byte[] data = Arrays.copyOfRange(pduBytes, dtnLink.HEADER_SIZE, pduBytes.length)
        return new Tuple(ttl, protocol, data)
    }

    OutputPDU getPDU(String messageID, boolean adjustTtl) {
        try {
            byte[] pduBytes = Files.readAllBytes(new File(directory, messageID).toPath())
            if (pduBytes != null) {
                Tuple pduTuple = decodePdu(pduBytes)
                int ttl = (adjustTtl) ? getMetadata(messageID).expiryTime - dtnLink.currentTimeSeconds() : (int)pduTuple.get(0)
                int protocol = (int)pduTuple.get(1)
                byte[] data = (byte[])pduTuple.get(2)
                if (ttl > 0) {
                    return encodePdu(data, ttl, protocol)
                }
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

    int getTimeSinceArrival(String messageID) {
        OutputPDU pdu = getPDU(messageID, false)
        if (pdu != null) {
            Tuple pduInfo = decodePdu(pdu.toByteArray())
            int ttl = (int)pduInfo.get(0)
            int expiryTime = getMetadata(messageID).expiryTime
            int T = dtnLink.currentTimeSeconds()
            return T - (expiryTime - ttl)
        }
        return -1 // this happens when we deleted the PDU before the DDN reached us!
    }
}
