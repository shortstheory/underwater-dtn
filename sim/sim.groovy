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
import test.DatagramGenerator

import java.nio.file.Files

//platform = RealTimePlatform
platform = DiscreteEventSimulator

channel.model = BasicAcousticChannel
//channel.model = ProtocolChannelModel
//
//channel.soundSpeed = 300*1000*1000.mps           // c
//channel.communicationRange = 5000.m     // Rc
//channel.detectionRange = 5000.m         // Rd
//channel.interferenceRange = 5000.m      // Ri
//channel.pDetection = 1                  // pd
//channel.pDecoding = 1                   // pc
//
//modem.dataRate = [80*1000.bps, 240*1000.bps]
//modem.frameLength = [1500.bytes, 1500.bytes]
//modem.powerLevel = [0.dB, -10.dB]

println "Starting 3-simulation!"

def T = 1.hour

//int[] dest1 = [2,3]
//int[] dest2 = [3,1]
//int[] dest3 = [1,2]

int[] dest1 = [2]
int[] dest2 = [3]
int[] dest3 = [1]

int nodeCount = 3

def msgSize = 100
def msgFreq = 100*1000
def dist = 200.m
def msgTtl = 100000

for (def i = 0; i < 10; i++) {
    // add housekeeping here
    println("\n===========\nSize - " + msgSize + " Freq - " + msgFreq + " Dist - " + dist + " TTL - " + msgTtl)
    msgSize = (i+1)*100
    for (int f = 0; f < nodeCount; f++) {
        FileUtils.deleteDirectory(new File(Integer.toString(f)))
        Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
    }

    simulate T, {
        node 'a', address: 1, location: [0, 0, 0], shell: 5000, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
            container.add 'testagent', new DatagramGenerator(dest1, msgFreq, msgSize, msgTtl, false)
        }
        node 'b', address: 2, location: [dist, 0, 0], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
            container.add 'testagent', new DatagramGenerator(dest2, msgFreq, msgSize, msgTtl, false)
        }
        node 'c', address: 3, location: [0, dist, 0], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(3))
            container.add 'testagent', new DatagramGenerator(dest3, msgFreq, msgSize, msgTtl, false)
        }
    }
    DtnStats.printAllStats(nodeCount)
}

