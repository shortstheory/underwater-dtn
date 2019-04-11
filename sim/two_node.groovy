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
channel.pDecoding = 0.5
channel.communicationRange = range
channel.detectionRange = range
channel.interferenceRange = range

int nodeCount = 2
int[] dest1 = [2]
def T = 2.hour

def msgSize = 300
def msgFreq = 10
def msgTtl = T
def msgEndTime = 1000

println "Starting 2-hop simulation!"

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
}

test.DtnStats stat1
test.DtnStats stat2
//for (int i = 1; i <= 5; i++) {
    def i = 1
    channel.pDetection = 0.2 * i
    channel.pDecoding = 1.0
    println("Channel Config - " + channel.pDetection + " / " + channel.pDecoding)
    stat1 = new test.DtnStats()
    stat2 = new test.DtnStats()

    simulate T, {
        node '1', address: 1, location: [0, 0, -50.m], shell: 5000, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
            container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, msgEndTime, false, DtnApp.Mode.REGULAR, stat1)
        }
        node '2', address: 2, location: [nodeDistance, 0, -50.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
            container.add 'testagent', new DtnApp(stat2, false)
        }
    }

println("DtnLink Results")
stat2.printStats()
//}