package dtn

import groovy.transform.CompileStatic
import org.arl.unet.DatagramReq
import org.arl.unet.InputPDU
import org.arl.unet.OutputPDU

import java.nio.file.Files;

//@TypeChecked
@CompileStatic
class DtnStorage {
    private final String directory
    HashMap<String, DtnPduMetadata> db

    // New DatagramReqID - Old DatagramReqID
    HashMap<String, String> datagramMap

    DtnLink dtnLink

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
        db.get(oldMessageID).attempts++
        datagramMap.put(newMessageID, oldMessageID)
    }

    String getOriginalMessageID(String newMessageID) {
        return datagramMap.get(newMessageID)
    }

    ArrayList<String> getNextHopDatagrams(int nextHop) {
        ArrayList<String> data = new ArrayList<>()

        for (Map.Entry<String, DtnPduMetadata> entry : db.entrySet()) {
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
                // we don't delete here, as it will complicate the logic
                // instead, it will be deleted by the next sweep
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
            File file = new File(directory, messageID)
            Files.write(file.toPath(), pduBytes)
            db.put(messageID, new DtnPduMetadata(nextHop: nextHop,
                                                 expiryTime: (int)ttl + dtnLink.currentTimeSeconds(),
                                                 attempts: 0))
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
            nextHop = db.get(messageID).nextHop
            String key
            for (Map.Entry<String, String> entry : datagramMap.entrySet()) {
                if (entry.getValue() == messageID) {
                    key = entry.getKey()
                    break
                }
            }
            datagramMap.remove(key)
            db.remove(messageID)
        } catch (Exception e) {
            println "Could not delete file for " + messageID + " files " + datagramMap.size() + "/" + db.size()
        }
        // first & second
        return new Tuple2(messageID, nextHop)
    }

    ArrayList<Tuple2> deleteExpiredDatagrams() {
        // FIXME: This is very very broken CME
        ArrayList<Tuple2> expiredDatagrams = new ArrayList<>()

        Iterator it = db.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next()
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
//                expiredDatagrams.add(deleteFile(messageID))
            }
        }
        return expiredDatagrams
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
        byte[] data = Arrays.copyOfRange(pduBytes, 8, pduBytes.length)

        Tuple decodedPDU = new Tuple(ttl, protocol, data)
        return decodedPDU
    }

    // this method will automatically set the TTL correctly for sending the PDU
    byte[] getPDU(String messageID, boolean adjustTtl) {
        byte[] pduBytes = new File(directory, messageID).text.getBytes()
        Tuple pduTuple = decodePdu(pduBytes)

        int protocol = (int)pduTuple.get(1)
        byte[] data = (byte[])pduTuple.get(2)
        int ttl = (adjustTtl) ? db.get(messageID).expiryTime - dtnLink.currentTimeSeconds() : db.get(messageID).expiryTime
        if (ttl > 0) {
            return encodePdu(data, ttl, protocol)
        }
        return null
    }

    void removeFailedEntry(String newMessageID) {
        datagramMap.remove(newMessageID)
    }

    int getTimeSinceArrival(String messageID) {
        Tuple pduInfo = decodePdu(getPDU(messageID, false))
        int ttl = (int)pduInfo.get(0)
        int expiryTime = db.get(messageID).expiryTime
        return dtnLink.currentTimeSeconds() - (expiryTime - ttl)
    }
}
