import dtn.DtnLink
import org.apache.commons.io.FileUtils
import org.arl.fjage.DiscreteEventSimulator
import org.arl.unet.link.ReliableLink
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.channels.ProtocolChannelModel
import test.DtnApp
import test.DtnApp

import java.nio.file.Files
platform = DiscreteEventSimulator

channel.model = BasicAcousticChannel
//channel.model = ProtocolChannelModel
//channel.pDetection = 1.0
//channel.pDecoding = 1.0

int[] dest1 = [2]

def dist = 200.m
def msgSize = 5*1000
def msgFreq = 100
def msgTtl = 100000

def T = 1.hour
int nodeCount = 2

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

test.DtnStats stat1 = new test.DtnStats()
test.DtnStats stat2 = new test.DtnStats()

simulate T, {
    node 'a', address: 1, location: [0, 0, 0], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1), DtnLink.DatagramPriority.ARRIVAL)
        container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, 0, false, DtnApp.Mode.PAYLOAD, stat1)
    }
    node 'b', address: 2, location: [dist, 0, 0], shell: 5000, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'receiver', new DtnApp(stat2)
    }
}

stat1.printStats()
stat2.printStats()

