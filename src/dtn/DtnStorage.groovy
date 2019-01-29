package dtn

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked;

@TypeChecked
@CompileStatic
public class DtnStorage {
    HashMap<String, DtnPDUMetadata> db
    HashMap<String, String> datagramMap

    ArrayList<DtnPDUMetadata> getNextHopMetadata(int nextHop) {
        ArrayList data = new ArrayList<DtnPDUMetadata>()

        for (Map.Entry<String, DtnPDUMetadata> entry : db.entrySet()) {
            DtnPDUMetadata metadata = entry.getValue()
            if (System.currentTimeMillis() > metadata.expiryTime) {
                // delete
                continue
            }
            if (metadata.nextHop == nextHop) {
                data.add(metadata)
            }
        }

        return data;
    }
}
