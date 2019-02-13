//!Simulation
/// Output trace file: logs/trace.nam

import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import dtn.*
import org.arl.fjage.RealTimePlatform
import org.arl.unet.link.*
import org.arl.unet.net.Router
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.ProtocolChannelModel

//platform = RealTimePlatform
platform = DiscreteEventSimulator

channel.model = ProtocolChannelModel

channel.soundSpeed = 1500.mps           // c
channel.communicationRange = 2000.m     // Rc
channel.detectionRange = 2500.m         // Rd
channel.interferenceRange = 3000.m      // Ri
channel.pDetection = 1                  // pd
channel.pDecoding = 1                   // pc

println "Starting simulation!"

def T = 1.hour
def f = 10*1000

int[] dest1 = [2]
int[] dest2 = [1]
int[] dest3 = [1, 2]

println '''
TX Count\tRX Count\tLoss %\t\tOffered Load\tThroughput
--------\t--------\t------\t\t------------\t----------'''

for (def i = 0; i < 10; i++) {
    // add housekeeping here
    FileUtils.deleteDirectory(new File("1"))
    FileUtils.deleteDirectory(new File("2"))
    FileUtils.deleteDirectory(new File("3"))
    simulate T, {
        node '1', address: 1, location: [0, 0, 0], shell: 5000, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink()
            //    container.add 'mac', new CSMA()
            container.add 'testagent', new DatagramGenerator(dest1, f)
        }
        node '2', address: 2, location: [2000.m, 0, 0], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink()
            container.add 'testagent', new DatagramGenerator(dest2, f)
            //    container.add 'mac', new CSMA()
        }
        node '3', address: 3, location: [0, 2000.m, 0], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink()
            container.add 'testagent', new DatagramGenerator(dest3, f)
            //    container.add 'mac', new CSMA()
//        container.add 'router', new Router()
        }
    }
    float loss = trace.txCount ? 100*trace.dropCount/trace.txCount : 0
    Container[] containers = platform.getContainers()
    println sprintf('%6d\t\t%6d\t\t%5.1f\t\t%7.3f\t\t%7.3f',
            [trace.txCount, trace.rxCount, loss, trace.offeredLoad, trace.throughput])
}

