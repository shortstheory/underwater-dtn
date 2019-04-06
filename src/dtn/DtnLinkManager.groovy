package dtn

import com.sun.istack.internal.Nullable
import groovy.transform.CompileStatic
import org.arl.fjage.AgentID
import org.arl.unet.DatagramParam
import org.arl.unet.link.ReliableLink
import org.arl.unet.link.ReliableLinkParam
import org.arl.unet.phy.Physical
import org.arl.unet.phy.PhysicalChannelParam
import org.arl.unet.phy.PhysicalParam
import org.arl.unet.sim.HalfDuplexModem

/**
 * Helper class for managing the underlying links used by DtnLink
 * Sets priorities for links and records the time of last transmission of a particular link
 */
@CompileStatic
class DtnLinkManager {
    private DtnLink dtnLink

    class LinkMetadata {
        AgentID phyID
        int lastTransmission
        int linkMTU
        int dataRate
    }

    private HashMap<AgentID, LinkMetadata> linkInfo
    private HashMap<Integer, HashSet<AgentID>> nodeLinks

    DtnLinkManager(DtnLink dtnLink) {
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
            for (int i = 0; i < dtnLink.linkPriority.size(); i++) {
                if (aid == dtnLink.linkPriority[i] && i < bestLinkPriority) {
                    bestLinkPriority = i
                    bestLink = aid
                }
            }
        }
        return bestLink
    }

    void addLink(AgentID link) {
        dtnLink.subscribe(dtnLink.topic(link))
        // If phy for a link doesn't exist, we can't SNOOP to listen for other transmissions
        String phyName = dtnLink.getProperty(link, ReliableLinkParam.phy)
        // FIXME: agent(null) returns "null" and not null!
        AgentID phy = (phyName != null) ? dtnLink.agent(phyName) : null
        int mtu = (int)dtnLink.getProperty(link, DatagramParam.MTU)
        int[] dataRateArray
        int dataRate = 0
        if (phy != null) {
            // FIXME: how do I get ethe data rate directly?
            dataRateArray = (int[])dtnLink.getProperty(phy, PhysicalChannelParam.dataRate)
            dataRate = dataRateArray[Physical.DATA-1]
            dtnLink.subscribe(dtnLink.topic(phy))
            dtnLink.subscribe(dtnLink.topic(phy, Physical.SNOOP))
        } else {
            println "PHY not provided for link"
        }
        linkInfo.put(link, new LinkMetadata(phyID: phy, lastTransmission: dtnLink.currentTimeSeconds(), linkMTU: mtu, dataRate: dataRate))
    }

    List<Integer> getDestinationNodes() {
        return nodeLinks.keySet().asList()
    }

    Set<AgentID> getLinksForNode(int node) {
        Set<AgentID> liveLinks = new HashSet<>()
        Set<AgentID> links = nodeLinks.get(node)
        if (links != null) {
            for (AgentID link : links) {
                LinkMetadata metadata = getLinkMetadata(link)
                if (metadata != null) {
                    if (metadata.lastTransmission + dtnLink.linkExpiryTime > dtnLink.currentTimeSeconds()) {
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

    @Nullable AgentID getLink(AgentID l) {
        AgentID link = (l.isTopic()) ? l.getOwner().getAgentID() : l
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

    boolean linkExists(AgentID link) {
        return (linkInfo.get(link) != null)
    }
}
