package dtn

import groovy.transform.CompileStatic
import org.arl.unet.DatagramReq
import org.arl.unet.InputPDU
import org.arl.unet.OutputPDU

import java.nio.file.Files;

@CompileStatic
class DtnStorage {
    private final String directory
    private DtnLink dtnLink
    private HashMap<String, DtnPduMetadata> db
    // New DatagramReqID - Old DatagramReqID
    private HashMap<String, String> datagramMap

    DtnStorage(DtnLink link, String dir) {
        directory = dir
        dtnLink = link

        File file = new File(directory)
        if (!file.exists()) {
            file.mkdir()
        }

        db = new HashMap<>()
        datagramMap = new HashMap<>()
    }

    void trackDatagram(String newMessageID, String oldMessageID) {
        getMetadata(oldMessageID).attempts++
        datagramMap.put(newMessageID, oldMessageID)
    }

    DtnPduMetadata getDatagramMetadata(String messageID) {
        return db.get(messageID)
    }

    String getOriginalMessageID(String newMessageID) {
        return datagramMap.get(newMessageID)
    }

    ArrayList<String> getNextHopDatagrams(int nextHop) {
        ArrayList<String> data = new ArrayList<>()
        for (Map.Entry<String, DtnPduMetadata> entry : db.entrySet()) {
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime
                || metadata.attempts >= dtnLink.MAX_RETRIES
                || metadata.delivered) {
                // we don't delete here, as it will complicate the logic
                // instead, it will be deleted by the next DtnLink sweep

                // one hack for cleaning MAX_RETRIES being exceeded.
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
        int ttl = Math.round(req.getTtl())
        String messageID = req.getMessageID()
        byte[] data = req.getData()
        byte[] pduBytes = encodePdu(data, ttl, protocol)

        try {
            File dir = new File(directory)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            File file = new File(dir, messageID)
            Files.write(file.toPath(), pduBytes)
            db.put(messageID, new DtnPduMetadata(nextHop: nextHop,
                                                 expiryTime: (int)ttl + dtnLink.currentTimeSeconds(),
                                                 attempts: 0,
                                                 delivered: false))
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        }
    }

    Tuple2 deleteFile(String messageID) {
        int nextHop
        try {
            File file = new File(directory, messageID)

            file.delete()
            nextHop = getMetadata(messageID).nextHop
            String key
            for (Map.Entry<String, String> entry : datagramMap.entrySet()) {
                if (entry.getValue() == messageID) {
                    key = entry.getKey()
                    break
                }
            }
            datagramMap.remove(key)
//            db.remove(messageID) removing it this way will cause a CME!!, must remove through it
        } catch (Exception e) {
            println "Could not delete file for " + messageID + " files " + datagramMap.size() + "/" + db.size()
        }
        // first & second
        return new Tuple2(messageID, nextHop)
    }

    ArrayList<Tuple2> deleteExpiredDatagrams() {
        ArrayList<Tuple2> expiredDatagrams = new ArrayList<>()

        Iterator it = db.entrySet().iterator()
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

    byte[] encodePdu(byte[] data, int ttl, int protocol) {
        // ttl + protocol = 8 bytes?
        OutputPDU pdu = new OutputPDU(data.length + 8)
        pdu.write32(ttl)
        pdu.write32(protocol)
        pdu.write(data)
        return pdu.toByteArray()
    }

    Tuple decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)

        int ttl = pdu.read32()
        int protocol = pdu.read32()
        // the data follows the 8 byte header
        byte[] data = Arrays.copyOfRange(pduBytes, 8, pduBytes.length)
        return new Tuple(ttl, protocol, data)
    }

    byte[] getPDU(String messageID, boolean adjustTtl) {
        // This occasionally throws NPEs. Who knows why?
        try {
            byte[] pduBytes = new File(directory, messageID).text.getBytes()
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
            return  null
        }
    }

    DtnPduMetadata getMetadata(String messageID) {
        return db.get(messageID)
    }

    void removeFailedEntry(String newMessageID) {
        datagramMap.remove(newMessageID)
    }

    int getTimeSinceArrival(String messageID) {
        byte[] pdu = getPDU(messageID, false)
        if (pdu != null) {
            Tuple pduInfo = decodePdu(pdu)
            int ttl = (int)pduInfo.get(0)
            int expiryTime = getMetadata(messageID).expiryTime
            return dtnLink.currentTimeSeconds() - (expiryTime - ttl)
        }
        return -1 // this happens when we deleted the PDU before the DDN reached us!
    }
}
