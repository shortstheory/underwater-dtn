//!Simulation
/// Output trace file: logs/trace.nam

import com.google.gson.Gson
import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import dtn.*
import org.arl.fjage.RealTimePlatform
import org.arl.unet.link.*
import org.arl.unet.net.Router
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.channels.ProtocolChannelModel

import java.nio.file.Files

//platform = RealTimePlatform
platform = DiscreteEventSimulator

channel.model = BasicAcousticChannel
//channel.model = ProtocolChannelModel
//
//channel.soundSpeed = 1500.mps           // c
//channel.communicationRange = 2000.m     // Rc
//channel.detectionRange = 2500.m         // Rd
//channel.interferenceRange = 3000.m      // Ri
//channel.pDetection = 1                  // pd
//channel.pDecoding = 1                   // pc

println "Starting simulation!"

def T = 1.hour

//int[] dest1 = [2,3]
//int[] dest2 = [3,1]
//int[] dest3 = [1,2]

int[] dest1 = [2]
int[] dest2 = [3]
int[] dest3 = [1]

int nodeCount = 3

def msgSize = 400
def msgFreq = 100*1000
def dist = 1000.m
def msgTtl = 1000

for (def i = 0; i < 10; i++) {
    // add housekeeping here
    println("\n===========\nSize - " + msgSize + " Freq - " + msgFreq + " Dist - " + dist + " TTL - " + msgTtl)

    for (int f = 0; f < nodeCount; f++) {
        FileUtils.deleteDirectory(new File(Integer.toString(f)))
        Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
    }

    simulate T, {
        node 'a', address: 1, location: [0, 0, 0], shell: 5000, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
            container.add 'testagent', new DatagramGenerator(dest1, msgFreq, msgSize, msgTtl, true)
        }
        node 'b', address: 2, location: [dist, 0, 0], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
            container.add 'testagent', new DatagramGenerator(dest2, msgFreq, msgSize, msgTtl, true)
        }
        node 'c', address: 3, location: [0, dist, 0], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(3))
            container.add 'testagent', new DatagramGenerator(dest3, msgFreq, msgSize, msgTtl, true)
        }
    }
    DtnStats.printAllStats(nodeCount)
}

