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
        STRESS
    }

    public static final String MESSAGE_ID = "testmessage"
    public static final String MESSAGE_DATA = "testdata"
    public static final int DEST_ADDRESS = 2
    public static final int MESSAGE_TTL = 3600
    public static final int MESSAGE_PROTOCOL = Protocol.USER
    public static final int PRIORITY_MESSAGES = 100
    public static final int DELAY_TIME = 3600*1000

    public static final int DTN_MAX_RETRIES = 5
    public static final int DTN_LINK_EXPIRY = 600

    @Before
    public void beforeTesting() {
        println("Cleaning Dirs")
        FileUtils.deleteDirectory(new File(path))
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
        assert(app.TRIVIAL_MESSAGE_RESULT)
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
        assert(app.SUCCESSFUL_DELIVERY_RESULT)
        assert(link.SUCCESSFUL_DELIVERY_RESULT)
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
        assert(app.ROUTER_MESSAGE_RESULT)
        assert(link.ROUTER_MESSAGE_RESULT)
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
        assert(app.MAX_RETRY_RESULT)
        assert(link.MAX_RETRY_RESULT)
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
        assert(app.ARRIVAL_PRIORITY_RESULT)
        assert(link.ARRIVAL_PRIORITY_RESULT)
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
        assert(app.EXPIRY_PRIORITY_RESULT)
        assert(link.EXPIRY_PRIORITY_RESULT)
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
        assert(app.RANDOM_PRIORITY_RESULT)
        assert(link.RANDOM_PRIORITY_RESULT)
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
        assert(app.TIMEOUT_D1_SUCCESS)
        assert(app.TIMEOUT_D2_FAILED)
        assert(link.TIMEOUT_D1_SUCCESS)
        assert(link.TIMEOUT_D2_FAILED)
    }

    @After
    public void afterTesting() {
    }
}