//!Simulation
/// Output trace file: logs/trace.nam

import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import dtn.*
import org.arl.unet.link.*
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel

import java.nio.file.Files

platform = DiscreteEventSimulator

channel.model = BasicAcousticChannel

println "Starting AUV simulation!"

def T = 5200.second
int nodeCount = 2

def msgSize = 100
def msgFreq = 10*1000
def dist = 1000.m
def msgTtl = 600

int[] dest1 = [2]


for (int f = 0; f < nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}
for (int i = 1; i < 10; i++) {
    msgTtl = 300
    println("\n===========\nSize - " + msgSize + " Freq - " + msgFreq + " Dist - " + dist + " TTL - " + msgTtl)
    simulate T, {
        def sensor = node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink()
            container.add 'testagent', new DatagramGenerator(dest1, msgFreq, msgSize, msgTtl)
        }
        def auv = node '2', address: 2, mobility: true, location: [2400.m, 0, -50.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink()
        }
        auv.motionModel = [[duration: 300.second, heading: 0.deg, speed: 1.mps],
                           [duration: 2000.seconds, heading: 270.deg, speed: 1.mps],
                           [duration: 600.seconds, heading: 180.deg, speed: 1.mps],
                           [duration: 2000.seconds, heading: 90.deg, speed: 1.mps],
                           [duration: 300.seconds, heading: 0.deg, speed: 1.mps]]
    }
    DtnStats.printAllStats(nodeCount)
}
