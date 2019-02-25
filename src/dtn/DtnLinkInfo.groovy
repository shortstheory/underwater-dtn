package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.fjage.Agent
import org.arl.unet.link.ReliableLinkParam

@CompileStatic
class DtnLinkInfo {
    private int LINK_EXPIRY_TIME = 10*60 // 10 mins before we remove a link's "ALIVE" status
    private DtnLink dtnLink

    class LinkMetadata {
        AgentID phyID
        int lastTransmission
    }

    HashMap<AgentID, LinkMetadata> linkInfo
    private HashMap<Integer, HashSet<AgentID>> nodeLinks

    DtnLinkInfo(DtnLink dtnLink) {
        this.dtnLink = dtnLink
        linkInfo = new HashMap<>()
        nodeLinks = new HashMap<>()
    }

    LinkMetadata getLinkMetadata(AgentID link) {
        return linkInfo.get(link)
    }

    void addLink(AgentID link) {
        AgentID phy = dtnLink.agent((String)dtnLink.getProperty(link, ReliableLinkParam.phy))
        linkInfo.put(link, new LinkMetadata(phyID: phy, lastTransmission: 0))
    }

    Set<Integer> getNodes() {
        return nodeLinks.keySet()
    }

    Set<AgentID> getLinksForNode(int node) {
        Set<AgentID> liveLinks = new HashSet<>()
        Set<AgentID> links = nodeLinks.get(node)
        if (links != null) {
            for (AgentID link : links) {
                LinkMetadata metadata = linkInfo.get(link)

//                if (metadata.lastTransmission + LINK_EXPIRY_TIME < dtnLink.currentTimeSeconds()) {
                    liveLinks.add(link)
//                }
            }
        }
        return liveLinks
    }

    void addLinkForNode(int node, AgentID link) {
        if (nodeLinks.get(node) == null) {
            nodeLinks.put(node, new HashSet<AgentID>())
        }
        nodeLinks.get(node).add(link)
    }

    AgentID getLink(AgentID phy) {
        for (Map.Entry<AgentID, LinkMetadata> entry : linkInfo) {
            if (entry.getValue().phyID == phy) {
                return entry.getKey()
            }
        }
        return null
    }

    void updateLastTransmission(AgentID topic) {
        AgentID phy = topic.getOwner().getAgentID()
        for (Map.Entry<AgentID, LinkMetadata> entry : linkInfo.entrySet()) {
            AgentID linkID = entry.getKey()
            AgentID phyID = entry.getValue().phyID
            if (phyID == phy) {
                linkInfo.get(linkID).lastTransmission = dtnLink.currentTimeSeconds()
            }
        }
    }
}
