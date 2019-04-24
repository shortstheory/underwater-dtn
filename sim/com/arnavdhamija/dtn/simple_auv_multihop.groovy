//!Simulation
/// Output trace file: logs/trace.nam

import com.arnavdhamija.dtn.*
import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import org.arl.unet.link.*
import org.arl.unet.net.RouteDiscoveryNtf
import org.arl.unet.net.Router
import org.arl.unet.sim.channels.ProtocolChannelModel
import org.arl.unet.transport.SWTransport
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel
import org.arl.unet.shell.*
import org.arl.fjage.AgentID

import java.nio.file.Files

platform = DiscreteEventSimulator

def range = 600.m
def nodeDistance = 900.m

channel.model = ProtocolChannelModel
channel.pDetection = 1.0
channel.pDecoding = 1.0
channel.communicationRange = range
channel.detectionRange = range
channel.interferenceRange = range

int nodeCount = 3
int[] dest1 = [3]
int[] dest3 = [1]


println "Starting Routing simulation!"

ArrayList<Tuple2> routesSrc = new ArrayList<>()
ArrayList<Tuple2> routesAUV = new ArrayList<>()
ArrayList<Tuple2> routesDest = new ArrayList<>()
routesSrc.add(new Tuple2(3,2))
routesAUV.add(new Tuple2(3,3))
routesAUV.add(new Tuple2(1,1))
routesDest.add(new Tuple2(1,2))


for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
}

//def T = 8800.second
def T = (8800)

def msgSize = 50
def msgFreq = 10
def msgTtl = T
def lastMsg = 1000

DtnStats stat1
DtnStats stat2
for (int i = 1; i <= 10; i+=1) {

    stat1 = new DtnStats()
    stat2 = new DtnStats()

    channel.pDetection = 0.1*i

    stat2.pDecode = channel.pDecoding
    stat2.pDetect = channel.pDetection
    stat2.simTime = T
    stat2.agentName = "dtnlink"
    stat2.msgSize = msgSize

    stat1.pDecode = channel.pDecoding
    stat1.pDetect = channel.pDetection
    stat1.simTime = T
    stat1.agentName = "dtnlink"
    stat1.msgSize = msgSize

    simulate T, {
        def src = node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(1))
            container.add 'router', new Router()
//            container.add 'testagent', new DtnApp(stat1, true)
            container.add 'router_init', new RouteInitialiser((Tuple2[]) routesSrc.toArray(), "dtnlink")
            container.add 'testagent', new DtnApp(dest1, msgFreq, msgSize, msgTtl, lastMsg, true, DtnApp.Mode.REGULAR, stat1)
        }
        def auv = node '2', address: 2, location: [0, 0, -50.m], mobility: true, shell: 5001, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(2))
            container.add 'router', new Router()
            container.add 'router_init', new RouteInitialiser((Tuple2[]) routesAUV.toArray(), "dtnlink")
        }
        def trajectory = [[duration: 300.seconds, heading: 0.deg, speed: 1.mps],
                          [duration: 1600.seconds, heading: 90.deg, speed: 1.mps],
                          [duration: 300.seconds, heading: 180.deg, speed: 1.mps],
                          [duration: 300.seconds, heading: 180.deg, speed: 1.mps],
                          [duration: 1600.seconds, heading: 270.deg, speed: 1.mps],
                          [duration: 300.seconds, heading: 0.deg, speed: 1.mps]]//,
        auv.motionModel = trajectory
        auv.motionModel += trajectory
        auv.motionModel += trajectory
        auv.motionModel += trajectory

        def dest = node '3', address: 3, location: [nodeDistance * 2, 0, -50.m], shell: 5002, stack: { container ->
            container.add 'link', new ReliableLink()
            container.add 'dtnlink', new DtnLink(Integer.toString(3))
            container.add 'router', new Router()
            container.add 'router_init', new RouteInitialiser((Tuple2[]) routesDest.toArray(), "dtnlink")
//            container.add 'testagent', new DtnApp(stat2, true)
            container.add 'testagent', new DtnApp(dest3, msgFreq, msgSize, msgTtl, lastMsg, true, DtnApp.Mode.REGULAR, stat2)
        }
    }

    stat1.printStats()
    stat2.printStats()
    String filename1 = "results/" + "dtnlink_auv_s1_" + i + "_" + msgSize + ".json"
    stat1.saveResults(filename1)
    String filename2 = "results/" + "dtnlink_auv_s3_" + i + "_" + msgSize + ".json"
    stat2.saveResults(filename2)
}

