//!Simulation

package com.arnavdhamija.dtn
import org.apache.commons.io.FileUtils
import org.arl.fjage.DiscreteEventSimulator
import org.arl.unet.link.ReliableLink
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.channels.ProtocolChannelModel


import java.nio.file.Files
platform = DiscreteEventSimulator

//channel.model = BasicAcousticChannel
channel.model = ProtocolChannelModel
channel.pDetection = 1.0
channel.pDecoding = 1.0

int[] dest1 = [2]

def T = 4.hour
def dist = 200.m
def msgSize = 5*1000
def msgFreq = 10.second
def msgTtl = T

int nodeCount = 2

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

DtnStats stat1 = new DtnStats()
DtnStats stat2 = new DtnStats()

simulate T, {
    node 'a', address: 1, location: [0, 0, 0], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1), DtnLink.DatagramPriority.ARRIVAL)
        container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, msgFreq*20, false, DtnApp.Mode.PAYLOAD, stat1)
    }
    node 'b', address: 2, location: [dist, 0, 0], shell: 5000, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'receiver', new DtnApp(stat2)
    }
}

stat1.printStats()
stat2.printStats()

