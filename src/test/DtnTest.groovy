package test

import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.arl.unet.Protocol
import org.junit.*
import org.arl.fjage.*
import dtn.*

@CompileStatic
class DtnTest {
    // FIXME: add multilink tests once the DataRate parameter is added
    String path = "testNode"

    public enum Tests {
        TRIVIAL_MESSAGE,
        SUCCESSFUL_DELIVERY,
        ROUTER_MESSAGE,
        MAX_RETRIES,
        EXPIRY_PRIORITY, // just check order @DtnLink
        ARRIVAL_PRIORITY,
        RANDOM_PRIORITY, // count if all messages have been sent
        TIMEOUT, // i.e., is our link still active? - add link, delay
        MULTI_LINK
    }

    public static final String MESSAGE_ID = "testmessage"
    public static final String BIG_MESSAGE_ID = "bigmessage"
    public static final String MESSAGE_DATA = "testdata"
    public static final String storagePath = "testStorage"
    public static final String payloadPath = "testPayload"

    public static final int DEST_ADDRESS = 2
    public static final int MESSAGE_TTL = 3600
    public static final int MESSAGE_PROTOCOL = Protocol.USER
    public static final int PRIORITY_MESSAGES = 100
    public static final int DELAY_TIME = 3600*1000

    public static final int DTN_MAX_RETRIES = 5
    public static final int DTN_LINK_EXPIRY = 600
    public static final int PAYLOAD_SIZE = 10000
    public String payloadText
    public static ArrayList<AgentID> LINK_ORDER = new ArrayList<>()

    @Before
    public void beforeTesting() {
        println("Cleaning Dirs")
        FileUtils.deleteDirectory(new File(path))
        payloadText = new File(payloadPath).text
    }

    @Test
    public void testTrivialMessage() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.TRIVIAL_MESSAGE)
        TestLink link = new TestLink(DtnTest.Tests.TRIVIAL_MESSAGE)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME)
        println("Done")
        p.shutdown()
        assert(app.trivialMessageResult)
    }

    @Test
    public void testSuccessfulDelivery() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.SUCCESSFUL_DELIVERY)
        TestLink link = new TestLink(DtnTest.Tests.SUCCESSFUL_DELIVERY)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME)
        println("Done")
        p.shutdown()
        assert(app.successfulDeliveryResult)
        assert(link.successfulDeliveryResult)
    }

    @Test
    public void testRouterDelivery() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.ROUTER_MESSAGE)
        TestLink link = new TestLink(DtnTest.Tests.ROUTER_MESSAGE)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME)
        println("Done")
        p.shutdown()
        assert(app.routerMessageResult)
        assert(link.routerMessageResult)
    }

    @Test
    public void testMaxRetries() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.MAX_RETRIES)
        TestLink link = new TestLink(DtnTest.Tests.MAX_RETRIES)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME)
        println("Done")
        p.shutdown()
        assert(app.maxRetryResult)
        assert(link.maxRetryResult)
    }

    @Test
    public void testArrivalPriority() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.ARRIVAL_PRIORITY)
        TestLink link = new TestLink(DtnTest.Tests.ARRIVAL_PRIORITY)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME*PRIORITY_MESSAGES) // extra long, but that's OK
        println("Done")
        p.shutdown()
        assert(app.arrivalPriorityResult)
        assert(link.arrivalPriorityResult)
    }

    @Test
    public void testExpiryPriority() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.EXPIRY_PRIORITY)
        TestLink link = new TestLink(DtnTest.Tests.EXPIRY_PRIORITY)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME*PRIORITY_MESSAGES) // extra long, but that's OK
        println("Done")
        p.shutdown()
        assert(app.expiryPriorityResult)
        assert(link.expiryPriorityResult)
    }

    @Test
    public void testRandomPriority() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.RANDOM_PRIORITY)
        TestLink link = new TestLink(DtnTest.Tests.RANDOM_PRIORITY)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME*PRIORITY_MESSAGES) // extra long, but that's OK
        println("Done")
        p.shutdown()
        assert(app.randomPriorityResult)
        assert(link.randomPriorityResult)
    }

    @Test
    public void testTimeout() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.TIMEOUT)
        TestLink link = new TestLink(DtnTest.Tests.TIMEOUT)
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME*PRIORITY_MESSAGES) // extra long, but that's OK
        println("Done")
        p.shutdown()
        assert(app.timeoutD1Success)
        assert(app.timeoutD2Failed)
        assert(link.timeoutD1Success)
//        assert(link.timeoutD2Failed)
    }

    @Test
    public void testMultilink() {
        Platform p = new DiscreteEventSimulator()
        Container c = new Container(p)
        TestApp app = new TestApp(DtnTest.Tests.MULTI_LINK)
        TestLink link1 = new TestLink(DtnTest.Tests.MULTI_LINK)
        TestLink link2 = new TestLink(DtnTest.Tests.MULTI_LINK)
        TestLink link3 = new TestLink(DtnTest.Tests.MULTI_LINK)
        link1.linkPriorityExpectMessage = false
        link2.linkPriorityExpectMessage = true
        link3.linkPriorityExpectMessage = false

        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("link1", link1)
        c.add("link2", link2)
        c.add("link3", link3)
        LINK_ORDER.add(link2.getAgentID())
        LINK_ORDER.add(link3.getAgentID())
        LINK_ORDER.add(link1.getAgentID())
        p.start()
        println("Running")
        p.delay(DELAY_TIME) // extra long, but that's OK
        println("Done")
        p.shutdown()
        assert(app.multiLinkResult)
        assert(link1.beaconReceived)
        assert(link2.beaconReceived)
        assert(link3.beaconReceived)
        assert(link1.linkPriorityExpectMessage == link1.linkPriorityReceivedMessage)
        assert(link2.linkPriorityExpectMessage == link2.linkPriorityReceivedMessage)
        assert(link3.linkPriorityExpectMessage == link3.linkPriorityReceivedMessage)
    }

    @After
    public void afterTesting() {
    }
}