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
def nodeDistance = 800.m

channel.model = ProtocolChannelModel
channel.pDetection = 1.0
channel.pDecoding = 1.0
channel.communicationRange = range
channel.detectionRange = range
channel.interferenceRange = range

int nodeCount = 3
int[] dest1 = [3]

println "Starting Complex AUV simulation!"

ArrayList<Tuple2> routes1 = new ArrayList<>()
routes1.add(new Tuple2(3,2))

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
}

test.DtnStats stat1 = new test.DtnStats()
test.DtnStats stat2 = new test.DtnStats()

def msgSize = 300
def msgFreq = 10*1000
def msgTtl = 10400

def T = 10400.second
simulate T, {
    def master = node '1', address: 1, location: [-1*nodeDistance, 0, -50.m], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routes1.toArray())
        container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, 0, true, DtnApp.Mode.REGULAR, stat1)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def slave = node '2', address: 2, location: [0, 0, -50.m], shell: 5001, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routes1.toArray())
        container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, 0, true, DtnApp.Mode.REGULAR, stat1)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def auv = node '3', address: 3, location: [nodeDistance/2, 0, -50.m], mobility: true, shell: 5002, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'router', new Router()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def trajectory = [[duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                      [duration: 2000.seconds, heading: 270.deg, speed: 1.mps],
                      [duration: 600.seconds, heading: 180.deg, speed: 1.mps],
                      [duration: 2000.seconds, heading: 90.deg, speed: 1.mps],
                      [duration: 300.seconds, heading: 0.deg, speed: 1.mps]]//,
    auv.motionModel = trajectory
    def sensor0 = node '4', address: 4, location: [nodeDistance, nodeDistance/2, -50.m], shell: 5003, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(3))
        container.add 'router', new Router()
        container.add 'testagent', new DtnApp(stat2)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    def sensor1 = node '5', address: 5, location: [nodeDistance*2, 0, -50.m], shell: 5004, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(3))
        container.add 'router', new Router()
        container.add 'testagent', new DtnApp(stat2)
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }

}

stat1.printStats()
stat2.printStats()