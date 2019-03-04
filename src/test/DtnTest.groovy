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
        c.add("dtnlink", new DtnLink(path))
        c.add("testapp", new TestApp(TestApp.Tests.TRIVIAL_MESSAGE_TEST))
        c.add("testlink", new TestLink())

        p.start()
        println("Running")
        p.delay(DELAY_TIME)
        println("Done")
        p.shutdown()
    }

    @After
    public void afterTesting() {
        println "After test"
    }
}