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
import org.arl.fjage.AgentID
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

ArrayList<Tuple2> routesSrc = new ArrayList<>()
ArrayList<Tuple2> routesAUV = new ArrayList<>()
routesSrc.add(new Tuple2(3,2))
routesAUV.add(new Tuple2(3,3))

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
}

def T = 10400.second
def msgSize = 50
def msgFreq = 10
def msgTtl = T
def lastMsg = T

test.DtnStats stat1
test.DtnStats stat2

stat1 = new test.DtnStats()
stat2 = new test.DtnStats()

simulate T, {
    def src = node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routesSrc.toArray(), new AgentID("dtnlink"))
        container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, lastMsg, true, DtnApp.Mode.REGULAR, stat1)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def auv = node '2', address: 2, location: [0, 0, -50.m], mobility: true, shell: 5001, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routesAUV.toArray(), new AgentID("dtnlink"))
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def trajectory = [[duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                      [duration: 2000.seconds, heading: 90.deg, speed: 1.mps],
                      [duration: 600.seconds, heading: 180.deg, speed: 1.mps],
                      [duration: 2000.seconds, heading: 270.deg, speed: 1.mps],
                      [duration: 300.seconds, heading: 0.deg, speed: 1.mps]]//,
    auv.motionModel = trajectory
    auv.motionModel += trajectory
    def dest = node '3', address: 3, location: [nodeDistance*2, 0, -50.m], shell: 5002, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(3))
        container.add 'router', new Router()
        container.add 'testagent', new DtnApp(stat2, true)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
}

stat1.printStats()
stat2.printStats()

