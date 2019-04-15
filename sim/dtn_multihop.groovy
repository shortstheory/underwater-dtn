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

def range = 2000.m
def nodeDistance = 1500.m

channel.model = ProtocolChannelModel
channel.communicationRange = range
channel.detectionRange = range
channel.interferenceRange = range

int nodeCount = 3
int[] dest1 = [3]
def T = 3.hour

def msgSize = 40
def msgFreq = 10
def msgTtl = T
def msgEndTime = 2000


println "Starting Routing simulation!"

ArrayList<Tuple2> routes1 = new ArrayList<>()
ArrayList<Tuple2> routes2 = new ArrayList<>()

routes1.add(new Tuple2(3,2))
routes2.add(new Tuple2(3,3))

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
}

test.DtnStats stat1
test.DtnStats stat2

for (int i = 1; i <= 10; i++) {
    channel.pDetection = 0.1 * i
    channel.pDecoding = 1.0

    println("\nChannel Config - " + channel.pDetection + " / " + channel.pDecoding)
    println("\nRunning sim for " + T + " seconds!\n==============\n")

    stat1 = new test.DtnStats()
    stat2 = new test.DtnStats()

    stat2.pDecode = channel.pDecoding
    stat2.pDetect = channel.pDetection
    stat2.simTime = T
    stat2.agentName = "dtnlink"
    stat2.msgSize = msgSize

    simulate T, {
        node '1', address: 1, location: [0, 0, -50.m], shell: 5000, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
            container.add 'router', new Router()
            container.add 'router_init', new RouteInitialiser((Tuple2[]) routes1.toArray(), "dtnlink")
            container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, msgEndTime, true, DtnApp.Mode.REGULAR, stat1)
        }
        node '2', address: 2, location: [nodeDistance, 0, -50.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
            container.add 'router_init', new RouteInitialiser((Tuple2[]) routes2.toArray(), "dtnlink")
            container.add 'router', new Router()
        }
        node '3', address: 3, location: [nodeDistance * 2, 0, -50.m], shell: 5002, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(3))
            container.add 'router', new Router()
            container.add 'testagent', new DtnApp(stat2, true)
        }
    }
    println("DtnLink Results")
    stat2.printStats()

    String filename1 = "results/" + "dtnlink_" + channel.pDetection + "_" + msgSize + ".json"
    stat2.saveResults(filename1)

    stat1 = new test.DtnStats()
    stat2 = new test.DtnStats()

    stat2.pDecode = channel.pDecoding
    stat2.pDetect = channel.pDetection
    stat2.simTime = T
    stat2.agentName = "reliablelink"
    stat2.msgSize = msgSize

    simulate T, {
        node '1', address: 1, location: [0, 0, -50.m], shell: 5000, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'router', new Router()
            container.add 'router_init', new RouteInitialiser((Tuple2[]) routes1.toArray(), "link")
            container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, msgEndTime, true, DtnApp.Mode.REGULAR, stat1)
        }
        node '2', address: 2, location: [nodeDistance, 0, -50.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'router_init', new RouteInitialiser((Tuple2[]) routes2.toArray(), "link")
            container.add 'router', new Router()
        }
        node '3', address: 3, location: [nodeDistance * 2, 0, -50.m], shell: 5002, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'router', new Router()
            container.add 'testagent', new DtnApp(stat2, true)
        }
    }
    println("ReliableLink Results")
    stat2.printStats()
    String filename2 = "results/" + "link_" + channel.pDetection + "_" + msgSize + ".json"
    stat2.saveResults(filename2)
    println("XXXXXXXXXXXXXXXXXXXX\n")
}
