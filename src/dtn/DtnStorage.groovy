package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.apache.commons.lang3.tuple.Pair
import org.arl.unet.DatagramReq
import org.arl.unet.Protocol

import java.nio.file.Files;

@TypeChecked
@CompileStatic
public class DtnStorage {
    private final String directory
    HashMap<String, DtnPDUMetadata> db

    // New DatagramReqID - Old DatagramReqID
    HashMap<String, String> datagramMap

    DtnStorage(String dir) {
        directory = dir

        File file = new File(directory)
        if (!file.exists()) {
            file.mkdir()
        }

        db = new HashMap<>()
        datagramMap = new HashMap<>()
    }

    ArrayList<String> getNextHopDatagrams(int nextHop) {
        ArrayList data = new ArrayList<String>()

        for (Map.Entry<String, DtnPDUMetadata> entry : db.entrySet()) {
            String messageID = entry.getKey()
            DtnPDUMetadata metadata = entry.getValue()
            if (System.currentTimeSeconds() > metadata.expiryTime) {
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
        float ttl = req.getTtl()
        String messageID = req.getMessageID()
        byte[] data = req.getData()

        try {
            File file = new File(directory, messageID)
            Files.write(file.toPath(), data)
            db.put(messageID, new DtnPDUMetadata(nextHop: nextHop,
                    expiryTime: (int)ttl + System.currentTimeSeconds()))
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        }
    }

    Tuple2 deleteFile(String messageID) {
        File file = new File(directory, messageID)
        int nextHop
        try {
            file.delete()
            nextHop = db.get(messageID).nextHop
            // add corresponding method for DatagramMap
        } catch (IOException e) {
            println "Could not delete file for " + messageID
        }
        return new Tuple2(messageID, nextHop)
    }

    ArrayList<Tuple2> deleteExpiredDatagrams() {
        ArrayList expiredDatagrams = new ArrayList<Tuple2>()

        for (Map.Entry<String, DtnPDUMetadata> entry : db.entrySet()) {
            String messageID = entry.getKey()
            DtnPDUMetadata metadata = entry.getValue()
            if (System.currentTimeSeconds() > metadata.expiryTime) {
                expiredDatagrams.add(deleteFile(entry.getKey()))
            }
        }
        return expiredDatagrams
    }

    byte[] encodePdu(String messageID) {

    }

    void decodePdu(byte[] pdu) {

    }

    

}
