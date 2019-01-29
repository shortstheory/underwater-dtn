package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
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
    }

    ArrayList<DtnPDUMetadata> getNextHopMetadata(int nextHop) {
        ArrayList data = new ArrayList<DtnPDUMetadata>()

        for (Map.Entry<String, DtnPDUMetadata> entry : db.entrySet()) {
            DtnPDUMetadata metadata = entry.getValue()
            if (System.currentTimeMillis() > metadata.expiryTime) {
                deleteFile(entry.getKey())
                continue
            }
            if (metadata.nextHop == nextHop) {
                data.add(metadata)
            }
        }
        return data
    }

    boolean saveDatagram(DatagramReq req) {
        int protocol = req.getProtocol()
        int nextHop = req.getTo()
        String messageID = req.getMessageID()
        byte[] data = req.getData()

        try {
            File file = new File(directory, messageID)
            Files.write(file.toPath(), data)
            db.put(messageID, new DtnPDUMetadata(nextHop: nextHop, protocol: protocol))
            return true
        } catch (IOException e) {
            println "Could not save file for " + messageID
            return false
        }
    }

    void deleteFile(String messageID) {
        File file = new File(directory, messageID)
        try {
            file.delete()
            db.remove(messageID)
            // add corresponding method for DatagramMap
        } catch (IOException e) {
            println "Could not delete file for " + messageID
        }
    }

    ArrayList<String> deleteExpiredDatagrams() {

    }
}
