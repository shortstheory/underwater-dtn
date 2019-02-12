package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.AgentID

@CompileStatic
class DtnUtility {
    // node - link pair; right now we only use one link per node
    private HashMap<Integer, AgentID> nodeLiveLinks
    private HashMap<AgentID, Integer> linkLastTransmission
    private HashMap<AgentID, AgentID> linkPhyMap

    DtnUtility() {
        nodeLiveLinks = new HashMap<>()
        linkLastTransmission = new HashMap<>()
        linkPhyMap = new HashMap<>()
    }

}
