import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.junit.*
import org.arl.fjage.*
import org.arl.unet.*
import dtn.*

import java.nio.file.Files

@CompileStatic
class DtnTest {
    String path = "testNode"

    @Before
    public void beforeTesting() {
        FileUtils.deleteDirectory(new File(path))
        Files.deleteIfExists((new File(path)).toPath())
    }

    @Test
    public void testPriority() {
        Platform platform = new DiscreteEventSimulator()
        Container container = new Container(platform)

    }

    @After
    public void afterTesting() {
        println "After test"
    }
}