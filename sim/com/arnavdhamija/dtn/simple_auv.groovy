//!Simulation
/// Output trace file: logs/trace.nam
package com.arnavdhamija.dtn
import com.arnavdhamija.dtn.DtnApp
import com.arnavdhamija.dtn.DtnLink
import com.arnavdhamija.dtn.DtnStats
import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import org.arl.unet.link.*
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel
import org.arl.unet.sim.channels.ProtocolChannelModel

import java.nio.file.Files

platform = DiscreteEventSimulator
channel.model = BasicAcousticChannel
//channel.model = ProtocolChannelModel
//channel.pDetection = 0.3                  // pd
//channel.pDecoding = 0.8                   // pc


println "Starting Simple AUV simulation!"

def T = 10400.second
int nodeCount = 2

def msgSize = 300
def msgFreq = 10*1000
def dist = 2500.m
def msgTtl = 300

int[] dest1 = [2]
int[] dest2 = [1]


for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

DtnStats stat1 = new DtnStats()
DtnStats stat2 = new DtnStats()

for (int i = 1; i <= 1; i++) {
    println("\n===========\nSize - " + msgSize + " Freq - " + msgFreq + " Dist - " + dist + " TTL - " + msgTtl)

    simulate T, {
        def sensor = node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
//            container.add 'udp', new UdpLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
            container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, 0, false, DtnApp.Mode.REGULAR, stat1)
        }
        def auvR = node '2', address: 2, mobility: true, location: [dist.m, 0, -50.m], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
//            container.add 'udp', new UdpLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
            container.add 'testapp', new DtnApp(stat2)
        }
        def trajectory = [[duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                            [duration: 2000.seconds, heading: 270.deg, speed: 1.mps],
                            [duration: 600.seconds, heading: 180.deg, speed: 1.mps],
                            [duration: 2000.seconds, heading: 90.deg, speed: 1.mps],
                            [duration: 300.seconds, heading: 0.deg, speed: 1.mps]]
        auvR.motionModel = trajectory
        auvR.motionModel += trajectory
    }
}

stat1.printStats()
stat2.printStats()