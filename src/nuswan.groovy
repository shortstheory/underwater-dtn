//!Simulation
/// Output trace file: logs/trace.nam

import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import dtn.*
import org.arl.unet.link.*
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel
import org.arl.unet.sim.channels.ProtocolChannelModel

import java.nio.file.Files

platform = DiscreteEventSimulator

channel.model = BasicAcousticChannel

println "Starting Swan simulation!"

def T = 600.second
int nodeCount = 2

def msgSize = 600
def msgFreq = 5*1000
def dist = 1000.m
def msgTtl = 3600

int[] dest1 = [2]
int[] dest2 = [1]

channel.model = ProtocolChannelModel

channel.soundSpeed = 300*1000*1000.mps           // c
channel.communicationRange = 5000.m     // Rc
channel.detectionRange = 5000.m         // Rd
channel.interferenceRange = 5000.m      // Ri
channel.pDetection = 0.8                  // pd
//channel.pDecoding = 0.9                // pc

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

for (int i = 1; i <= 10; i++) {
    println("\n===========\nSize - " + msgSize + " Freq - " + msgFreq + " Dist - " + dist + " TTL - " + msgTtl)
    simulate T, {
        def baseStation = node '1', address: 1, location: [dist, 0, 0.m], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
        }
        def swan = node '2', address: 2, location: [0.m, 0.m, 0.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
            container.add 'testagent', new DatagramGenerator(dest2, msgFreq, 600, msgTtl, false)
            container.add 'testagent2', new DatagramGenerator(dest2, msgFreq, 100, msgTtl, false)

        }
    }
    DtnStats.printAllStats(nodeCount)
}
