package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.fjage.Agent
import org.arl.unet.link.ReliableLinkParam

@CompileStatic
class DtnLinkInfo {
    private int LINK_EXPIRY_TIME = 10*60 // 10 mins before we remove a link's "ALIVE" status
    private DtnLink dtnLink

    // node - link pair; right now we only use one link per node
    private HashMap<Integer, AgentID> nodeLiveLinks
    private HashMap<AgentID, Integer> linkLastTransmission
    private HashMap<AgentID, AgentID> linkPhyMap

    DtnLinkInfo(DtnLink dtnLink) {
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

    int getLastTransmission(AgentID link) {
        return linkLastTransmission.getOrDefault(link, 0)
    }

    void addLink(AgentID link) {
        AgentID phy = dtnLink.agent((String)dtnLink.getProperty(link, ReliableLinkParam.phy))
        if (phy != null) {
            linkPhyMap.put(link, phy)
        }
    }

    void updateLiveLinks(Integer node, AgentID phy_topic) {
        AgentID phy = phy_topic.getOwner().getAgentID()
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
        Iterator it = nodeLiveLinks.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next()
            int node = entry.getKey()
            AgentID id = entry.getValue()
            if (dtnLink.currentTimeSeconds() > linkLastTransmission.get(id) + LINK_EXPIRY_TIME) {
                it.remove()
            }
        }
    }
}
