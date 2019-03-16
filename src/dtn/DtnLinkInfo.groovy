package dtn

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.fjage.Agent
import org.arl.unet.link.ReliableLinkParam
import org.arl.unet.phy.Physical
import org.arl.unet.*

@CompileStatic
class DtnLinkInfo {
    private DtnLink dtnLink

    class LinkMetadata {
        AgentID phyID
        int lastTransmission
        int linkMTU
        int priority
    }

    private HashMap<AgentID, LinkMetadata> linkInfo
    private HashMap<Integer, HashSet<AgentID>> nodeLinks

    DtnLinkInfo(DtnLink dtnLink) {
        this.dtnLink = dtnLink
        linkInfo = new HashMap<>()
        nodeLinks = new HashMap<>()
    }

    LinkMetadata getLinkMetadata(AgentID link) {
        return linkInfo.get(link)
    }

    HashMap<AgentID, LinkMetadata> getLinkInfo() {
        return linkInfo
    }

    AgentID getBestLink(int node) {
        int bestLinkPriority = Integer.MAX_VALUE
        AgentID bestLink = null
        Set<AgentID> nodeLinks = getLinksForNode(node)
        for (AgentID aid : nodeLinks) {
            for (int i = 0; i < dtnLink.LINK_PRIORITY.size(); i++) {
                if (aid == dtnLink.LINK_PRIORITY[i] && i < bestLinkPriority) {
                    bestLinkPriority = i
                    bestLink = aid
                }
            }
        }
        return bestLink
    }

    void addLink(AgentID link) {
        dtnLink.subscribe(dtnLink.topic(link))
        AgentID phy = dtnLink.agent((String)dtnLink.getProperty(link, ReliableLinkParam.phy))
        // it's OK if phy is null, we just won't have SNOOP


        int mtu = (int)dtnLink.getProperty(link, DatagramParam.MTU)
        linkInfo.put(link, new LinkMetadata(phyID: phy, lastTransmission: dtnLink.currentTimeSeconds(), linkMTU: mtu))
        if (phy != null) {
            dtnLink.subscribe(dtnLink.topic(phy))
            dtnLink.subscribe(dtnLink.topic(phy, Physical.SNOOP))
        } else {
            println "PHY not provided for link"
        }
    }

    Set<Integer> getDestinationNodes() {
        return nodeLinks.keySet()
    }

    Set<AgentID> getLinksForNode(int node) {
        Set<AgentID> liveLinks = new HashSet<>()
        Set<AgentID> links = nodeLinks.get(node)
        if (links != null) {
            for (AgentID link : links) {
                LinkMetadata metadata = getLinkMetadata(link)
                if (metadata != null) {
                    int currentTime = dtnLink.currentTimeSeconds()
                    if (metadata.lastTransmission + dtnLink.LINK_EXPIRY_TIME > currentTime) {
                        liveLinks.add(link)
                    }
                }
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

    AgentID getLinkForPhy(AgentID phy) {
        for (Map.Entry<AgentID, LinkMetadata> entry : linkInfo) {
            if (entry.getValue().phyID == phy) {
                return entry.getKey()
            }
        }
        return null
    }

    AgentID getLinkForTopic(AgentID topic) {
        AgentID link = topic.getOwner().getAgentID()
        for (Map.Entry<AgentID, LinkMetadata> entry : linkInfo) {
            if (entry.getKey().getName() == link.getName()) {
                return entry.getKey()
            }
        }
        return null
    }

    void updateLastTransmission(AgentID linkID) {
        if (linkInfo.get(linkID) != null) {
            linkInfo.get(linkID).lastTransmission = dtnLink.currentTimeSeconds()
        }
    }
}
