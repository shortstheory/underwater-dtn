package test

import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.arl.unet.link.ReliableLink
import org.arl.unet.sim.HalfDuplexModem
import org.junit.*
import org.arl.fjage.*
import org.arl.unet.*
import dtn.*
import test.TestApp
import test.TestLink

import java.nio.file.Files

@CompileStatic
class DtnTest {
    String path = "testNode"
    int DELAY_TIME = 3600*1000

    public enum Tests {
        TRIVIAL_MESSAGE_TEST
    }

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
        TestApp app = new TestApp(DtnTest.Tests.TRIVIAL_MESSAGE_TEST)
        TestLink link = new TestLink()
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", app)
        c.add("testlink", link)
        p.start()
        println("Running")
        p.delay(DELAY_TIME)
        println("Done")
        p.shutdown()
        assert(app.TRIVIAL_MESSAGE_TEST == true)
    }

    @After
    public void afterTesting() {
        println "After test"
    }
}