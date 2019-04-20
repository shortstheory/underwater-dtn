package dtn

import com.sun.istack.internal.Nullable
import groovy.transform.CompileStatic
import org.arl.fjage.AgentID
import org.arl.unet.DatagramParam
import org.arl.unet.link.ReliableLinkParam
import org.arl.unet.phy.Physical
import org.arl.unet.phy.PhysicalChannelParam

import java.util.logging.Logger

/**
 * Helper class for managing the underlying links used by DtnLink
 * Sets priorities for links and records the time of last transmission of a particular link
 */
@CompileStatic
class DtnLinkManager {
    private DtnLink dtnlink
    protected Logger log = Logger.getLogger(getClass().getName());

    class LinkMetadata {
        AgentID phyID
        int lastTransmission
        int lastReception
        int linkMTU
        int dataRate
    }

    private HashMap<AgentID, LinkMetadata> linkInfo       // Maintains the properties of each Link
    private HashMap<Integer, HashSet<AgentID>> nodeLinks  // Maintains the available Links available for each node

    DtnLinkManager(DtnLink dtnLink) {
        dtnlink = dtnLink
        linkInfo = new HashMap<>()
        nodeLinks = new HashMap<>()
    }

    LinkMetadata getLinkMetadata(AgentID link) {
        return linkInfo.get(link)
    }

    HashMap<AgentID, LinkMetadata> getLinkInfo() {
        return linkInfo
    }

    // Returns the highest priority link which is available in the LinkPriority list
    // FIXME: iterate through links on FailureNtf
    AgentID getBestLink(int node) {
        int bestLinkPriority = Integer.MAX_VALUE
        AgentID bestLink = null
        Set<AgentID> nodeLinks = getNodeLinks(node)
        for (AgentID aid : nodeLinks) {
            for (int i = 0; i < dtnlink.linkPriority.size(); i++) {
                if (aid == dtnlink.linkPriority[i] && i < bestLinkPriority) {
                    bestLinkPriority = i
                    bestLink = aid
                }
            }
        }
        return bestLink
    }

    void addLink(AgentID link) {
        dtnlink.subscribe(dtnlink.topic(link))
        // If phy for a link doesn't exist, we can't SNOOP to listen for other transmissions
        String phyName = dtnlink.getProperty(link, ReliableLinkParam.phy)
        AgentID phy = (phyName != null) ? dtnlink.agent(phyName) : null
        int mtu = (int)dtnlink.getProperty(link, DatagramParam.MTU)
        int[] dataRateArray
        int dataRate = 0
        if (phy != null) {
            // FIXME: how do I get the data rate directly? This looks clumsy!
            dataRateArray = (int[])dtnlink.getProperty(phy, PhysicalChannelParam.dataRate)
            dataRate = dataRateArray[Physical.DATA-1]
            dtnlink.subscribe(dtnlink.topic(phy))
            dtnlink.subscribe(dtnlink.topic(phy, Physical.SNOOP))
        } else {
            log.fine("PHY not provided for link")
        }
        linkInfo.put(link, new LinkMetadata(phyID: phy,
                            lastTransmission: dtnlink.currentTimeSeconds(),
                            lastReception: dtnlink.currentTimeSeconds(),
                            linkMTU: mtu,
                            dataRate: dataRate))
    }

    List<Integer> getDestinationNodes() {
        return nodeLinks.keySet().asList()
    }

    /**
     * Returns a set of Link AgentIDs which have not yet timed out for sending messages
     */
    Set<AgentID> getNodeLinks(int node) {
        Set<AgentID> liveLinks = new HashSet<>()
        Set<AgentID> links = nodeLinks.get(node)
        if (links != null) {
            for (AgentID link : links) {
                LinkMetadata metadata = getLinkMetadata(link)
                if (metadata != null) {
                    if (metadata.lastReception + dtnlink.linkExpiryTime > dtnlink.currentTimeSeconds()) {
                        liveLinks.add(link)
                    }
                }
            }
        }
        return liveLinks
    }

    void addNodeLink(int node, AgentID link) {
        if (nodeLinks.get(node) == null) {
            nodeLinks.put(node, new HashSet<AgentID>())
        }
        nodeLinks.get(node).add(link)
    }

    void removeNodeLink(int node, AgentID link) {
        // FIXME: only if we have options it makes sense to delete our current one, right?
        if (getNodeLinks(node).size() > 1) {
            nodeLinks.remove(node, link)
        }
    }

    AgentID getLinkForPhy(AgentID phy) {
        for (Map.Entry<AgentID, LinkMetadata> entry : linkInfo) {
            if (entry.getValue().phyID == phy) {
                return entry.getKey()
            }
        }
        return null
    }

    @Nullable AgentID getLink(AgentID agentID) {
        AgentID link = (agentID.isTopic()) ? agentID.getOwner().getAgentID() : agentID
        for (Map.Entry<AgentID, LinkMetadata> entry : linkInfo) {
            if (entry.getKey().getName() == link.getName()) {
                return entry.getKey()
            }
        }
        return null
    }

    void updateLastTransmission(AgentID linkID) {
        if (linkInfo.get(linkID) != null) {
            linkInfo.get(linkID).lastTransmission = dtnlink.currentTimeSeconds()
        }
    }

    void updateLastReception(AgentID linkID) {
        if (linkInfo.get(linkID) != null) {
            linkInfo.get(linkID).lastReception = dtnlink.currentTimeSeconds()
        }
    }

    boolean linkExists(AgentID link) {
        return (linkInfo.get(link) != null)
    }
}
