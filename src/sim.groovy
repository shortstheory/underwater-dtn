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

def T = 3.hour
def f = 100*1000
def dist = 200.m

int[] dest1 = [2]
int[] dest2 = [3]
int[] dest3 = [1]

int nodeCount = 3

//println '''
//TX Count\tRX Count\tLoss %\t\tOffered Load\tThroughput
//--------\t--------\t------\t\t------------\t----------'''

println '''
Node\tTx  \tRx  \tFail\tSuc \tReq \tStor\tRsnt\tBeac\tF%  \tTx%\t\tMean\tSD'''

DtnStats stats
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
        node '2', address: 2, location: [dist, 0, 0], shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink()
            container.add 'testagent', new DatagramGenerator(dest2, f)
            //    container.add 'mac', new CSMA()
        }
        node '3', address: 3, location: [0, dist, 0], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink()
            container.add 'testagent', new DatagramGenerator(dest3, f)
            //    container.add 'mac', new CSMA()
//        container.add 'router', new Router()
        }
    }
    println("")
    for (int stat = 1; stat <= nodeCount; stat++) {
        Gson gson = new Gson()
        String json = new File(Integer.toString(stat)+".json").text
        DtnStats dtnStats = gson.fromJson(json, DtnStats.class)
        dtnStats.printValues()
    }
//    float loss = trace.txCount ? 100*trace.dropCount/trace.txCount : 0
//    println sprintf('%6d\t\t%6d\t\t%5.1f\t\t%7.3f\t\t%7.3f',
//            [trace.txCount, trace.rxCount, loss, trace.offeredLoad, trace.throughput])

}

