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
//
def range = 1000.m
def nodeDistance = 800.m
//
channel.model = BasicAcousticChannel
//channel.model = ProtocolChannelModel
//channel.pDetection = 1.0
//channel.pDecoding = 1.0
//channel.communicationRange = range
//channel.detectionRange = range
//channel.interferenceRange = range

int nodeCount = 5

int m = 1
int s = 2
int a = 3
int s0 = 4
int s1 = 5

int[] destm = [s0,s1]
int[] destauv = [m]
int[] dests0 = [m]
int[] dests1 = [m]

println "Starting Complex AUV simulation!"

ArrayList<Tuple2> routesM = new ArrayList<>()
ArrayList<Tuple2> routesS = new ArrayList<>()
ArrayList<Tuple2> routesA = new ArrayList<>()
ArrayList<Tuple2> routesS0 = new ArrayList<>()
ArrayList<Tuple2> routesS1 = new ArrayList<>()
// first - finalDest, second - nextHop
routesM.add(new Tuple2(s0,s))
routesM.add(new Tuple2(s1,s))
routesS.add(new Tuple2(s0,a))
routesS.add(new Tuple2(s1,a))
routesA.add(new Tuple2(m,s))
routesA.add(new Tuple2(s0,s0))
routesA.add(new Tuple2(s1,s1))
routesS0.add(new Tuple2(m,a))
routesS1.add(new Tuple2(m,a))

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
}

test.DtnStats statM = new test.DtnStats()
//test.DtnStats statS = new test.DtnStats()
test.DtnStats statA = new test.DtnStats()
test.DtnStats statS0 = new test.DtnStats()
test.DtnStats statS1 = new test.DtnStats()

def msgSize = 300
def msgFreq = 10*1000
def msgTtl = 10400

def T = 10400.second
simulate T, {
    // 10 DGs to s0,s1
    def master = node '1', address: m, location: [-1*nodeDistance, 0, -50.m], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(m))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routesM.toArray())
        container.add 'testagent', new DtnApp(destm, 10, 200, T, 200, true, DtnApp.Mode.REGULAR, statM)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def slave = node '2', address: s, location: [0, 0, -50.m], shell: 5001, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(s))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routesS.toArray())
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    // 2 to m
    def auv = node '3', address: a, location: [nodeDistance/2, 0, -50.m], mobility: true, shell: 5002, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(a))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routesA.toArray())
        container.add 'testagent', new DtnApp(destauv, 1800, 600, 3600, 0, true, DtnApp.Mode.REGULAR, statA)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def trajectory = [[duration: 2600.seconds, heading: 0.deg, speed: 0.mps],
                      [duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                      [duration: 2000.seconds, heading: 90.deg, speed: 1.mps],
                      [duration: 600.seconds, heading: 180.deg, speed: 1.mps],
                      [duration: 2000.seconds, heading: 270.deg, speed: 1.mps],
                      [duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                      [duration: 2600.seconds, heading: 0.deg, speed: 0.mps]]
    auv.motionModel = trajectory
    // 1 to m
    def sensor0 = node '4', address: s0, location: [nodeDistance + nodeDistance/2, nodeDistance/2, -50.m], shell: 5003, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(s0))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routesS0.toArray())
        container.add 'testagent', new DtnApp(dests0, 300, 600, 3600, 0, true, DtnApp.Mode.REGULAR, statS0)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    // 6 to m
    def sensor1 = node '5', address: s1, location: [nodeDistance*3+nodeDistance/2, 0, -50.m], shell: 5004, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(s1))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routesS1.toArray())
        container.add 'testagent', new DtnApp(dests1, 300, 600, T, 0, true, DtnApp.Mode.REGULAR, statS1)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
}

statM.printStats()
statA.printStats()
statS0.printStats()
statS1.printStats()
