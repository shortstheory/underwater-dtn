package test

import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.arl.unet.Protocol
import org.junit.*
import org.arl.fjage.*
import dtn.*

import java.nio.file.Files

@CompileStatic
class DtnTest {
    String path = "testNode"
    int DELAY_TIME = 3600*1000

    public enum Tests {
        TRIVIAL_MESSAGE,
        SUCCESSFUL_DELIVERY,
        ROUTER_MESSAGE
    }

    public static final String MESSAGE_ID = "testmessage"
    public static final String MESSAGE_DATA = "testdata"
    public static final int DEST_ADDRESS = 2
    public static final int MESSAGE_TTL = 1000
    public static final int MESSAGE_PROTOCOL = Protocol.USER

    @Before
    public void beforeTesting() {
        println("Cleaning Dirs")
        FileUtils.deleteDirectory(new File(path))
        Files.deleteIfExists((new File(path)).toPath())
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

    @After
    public void afterTesting() {
        println "After test"
    }
}