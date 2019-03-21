//!Simulation
/// Output trace file: logs/trace.nam

import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import dtn.*
import org.arl.unet.link.*
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel
import org.arl.unet.net.RouteDiscoveryProtocol
import test.DtnApp

import java.nio.file.Files

platform = DiscreteEventSimulator

channel.model = BasicAcousticChannel

println "Starting AUV simulation!"

def T = 5200.second
int nodeCount = 3

def msgSize = 600
def msgFreq = 100*1000
def dist = 1000.m
def msgTtl = 600

int[] dest1 = [2,3]
int[] dest2 = [1]
int[] dest3 = [1]


for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}
for (int i = 1; i <= 10; i++) {
    println("\n===========\nSize - " + msgSize + " Freq - " + msgFreq + " Dist - " + dist + " TTL - " + msgTtl)
    simulate T, {
        def sensor = node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
//            container.add 'testagent', new test.DtnApp(dest1, msgFreq, msgSize, msgTtl, true)
        }
        def auvR = node '2', address: 2, mobility: true, location: [2400.m, 0, -50.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
//            container.add 'testagent', new test.DtnApp(dest2, msgFreq, msgSize, msgTtl, true)
        }
        auvR.motionModel = [[duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                           [duration: 2000.seconds, heading: 270.deg, speed: 1.mps],
                           [duration: 600.seconds, heading: 180.deg, speed: 1.mps],
                           [duration: 2000.seconds, heading: 90.deg, speed: 1.mps],
                           [duration: 300.seconds, heading: 0.deg, speed: 1.mps]]
        def auvL = node '3', address: 3, mobility: true, location: [-2400.m, 0, -50.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(3))
            container.add 'testagent', new DtnApp(dest3, msgFreq, msgSize, msgTtl, true)
        }
        auvL.motionModel = [[duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                           [duration: 2000.seconds, heading: 90.deg, speed: 1.mps],
                           [duration: 600.seconds, heading: 180.deg, speed: 1.mps],
                           [duration: 2000.seconds, heading: 270.deg, speed: 1.mps],
                           [duration: 300.seconds, heading: 0.deg, speed: 1.mps]]

    }
}
