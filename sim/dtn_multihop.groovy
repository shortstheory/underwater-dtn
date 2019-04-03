//!Simulation
/// Output trace file: logs/trace.nam

import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import dtn.*
import org.arl.unet.link.*
import org.arl.unet.net.RouteDiscoveryNtf
import org.arl.unet.net.Router
import org.arl.unet.sim.channels.ProtocolChannelModel
import org.arl.unet.transport.SWTransport
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel
import org.arl.unet.shell.*
import test.DtnApp
import test.RouteInitialiser

import java.nio.file.Files

platform = DiscreteEventSimulator

def range = 1000.m
def nodeDistance = 900.m

channel.model = ProtocolChannelModel
channel.pDetection = 1.0
channel.pDecoding = 1.0
channel.communicationRange = range
channel.detectionRange = range
channel.interferenceRange = range

int nodeCount = 3
int[] dest1 = [3]

println "Starting Routing simulation!"

ArrayList<Tuple2> routes1 = new ArrayList<>()
routes1.add(new Tuple2(3,2))

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

test.DtnStats stat1 = new test.DtnStats()
test.DtnStats stat2 = new test.DtnStats()

def msgSize = 300
def msgFreq = 1000*1000
def msgTtl = 3600

def T = 1.hour

DtnApp app = new DtnApp(dest1, msgFreq, msgSize, msgTtl, DtnApp.Mode.REGULAR, stat1)
app.useRouter = true

simulate T, {
    node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routes1.toArray())
        container.add 'testagent', app
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '2', address: 2, location: [nodeDistance, 0, -50.m], shell: 5001, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'router', new Router()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '3', address: 3, location: [nodeDistance*2, 0, -50.m], shell: 5002, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(3))
        container.add 'router', new Router()
        container.add 'testagent', new DtnApp(stat2)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
}

stat1.printStats()
stat2.printStats()