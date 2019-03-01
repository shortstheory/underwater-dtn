import groovy.transform.CompileStatic
import org.junit.*

@CompileStatic
class DtnTest {
    @Before
    public void beforeTesting() {
        println "Starting test"
    }

    @Test
    public void testNothing() {
        println "doing nothing"
    }

    @After
    public void afterTesting() {
        println "After test"
    }
}