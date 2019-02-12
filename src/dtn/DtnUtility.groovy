package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.fjage.Agent
import org.arl.unet.link.ReliableLinkParam

@CompileStatic
class DtnUtility {
    private int LINK_EXPIRY_TIME = 10*60 // 10 mins before we remove a link's "ALIVE" status
    private DtnLink dtnLink

    // node - link pair; right now we only use one link per node
    private HashMap<Integer, AgentID> nodeLiveLinks
    private HashMap<AgentID, Integer> linkLastTransmission
    private HashMap<AgentID, AgentID> linkPhyMap


    DtnUtility(DtnLink dtnLink) {
        this.dtnLink = dtnLink
        nodeLiveLinks = new HashMap<>()
        linkLastTransmission = new HashMap<>()
        linkPhyMap = new HashMap<>()
    }


    HashMap<AgentID, AgentID> getLinkPhyMap() {
        return linkPhyMap
    }

    HashMap<Integer, AgentID> getNodeLiveLinks() {
        return nodeLiveLinks
    }

    void updateTransmissionTimes(AgentID id) {
//        for (Map.Entry<AgentID, AgentID> entry : linkPhyMap.entrySet()) {
//            if (entry.value() == id) {
//                linkLastTransmission.
//            }
//        }
    }

    int getLastTransmission(AgentID link) {
        return linkLastTransmission.getOrDefault(link, 0)
    }

    void addLink(AgentID link) {
        AgentID phy = dtnLink.agent((String)dtnLink.getProperty(link, ReliableLinkParam.phy))
        if (phy != null) {
            linkPhyMap.put(link, phy)
        }
    }

    void updateLinkMaps(Integer node, AgentID topic) {
        AgentID phy = topic.getOwner().getAgentID()
        for (Map.Entry<AgentID, AgentID> entry : linkPhyMap.entrySet()) {
            AgentID linkID = entry.getKey()
            AgentID phyID = entry.getValue()
            if (phyID == phy) {
                nodeLiveLinks.put(node, linkID)
                linkLastTransmission.put(linkID, dtnLink.currentTimeSeconds())
            }
        }

    }

    void deleteExpiredLinks() {
//        for (Map.Entry<Integer, AgentID> entry : nodeLiveLinks.entrySet()) {
//            int node = entry.getKey()
//            AgentID id = entry.getValue()
//            if (dtnLink.currentTimeSeconds() > linkLastTransmission.get(id) + LINK_EXPIRY_TIME) {
//                nodeLiveLinks.remove(node)
//            }
//        }
    }

}