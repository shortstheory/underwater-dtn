import dtn.DtnLink
import org.apache.commons.io.FileUtils
import org.arl.fjage.DiscreteEventSimulator
import org.arl.unet.link.ReliableLink
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.channels.ProtocolChannelModel
import test.DatagramGenerator
import test.DummyApp

import java.nio.file.Files
platform = DiscreteEventSimulator

//channel.model = BasicAcousticChannel
channel.model = ProtocolChannelModel
channel.pDetection = 1.0
channel.pDecoding = 0.8

int[] dest1 = [2]

def dist = 200.m
def msgSize = 5*1000
def msgFreq = 900*1000
def msgTtl = 100000

def T = 1.hour
int nodeCount = 2

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

simulate {
    node 'a', address: 1, location: [0, 0, 0], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1))
        container.add 'testagent', new DatagramGenerator(dest1, msgFreq, msgSize, msgTtl, DatagramGenerator.Mode.PAYLOAD)
    }
    node 'b', address: 2, location: [dist, 0, 0], shell: 5000, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'receiver', new DatagramGenerator()
    }
}