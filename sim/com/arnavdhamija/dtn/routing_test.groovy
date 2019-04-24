//!Simulation
/// Output trace file: logs/trace.nam

import com.arnavdhamija.dtn.DtnLink
import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import org.arl.unet.link.*
import org.arl.unet.net.RouteDiscoveryNtf
import org.arl.unet.net.RouteDiscoveryProtocol
import org.arl.unet.net.Router
import org.arl.unet.sim.channels.ProtocolChannelModel
import org.arl.unet.transport.SWTransport
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel
import org.arl.unet.shell.*

import java.nio.file.Files

platform = RealTimePlatform

def range = 1000.m
def nodeDistance = 800.m
//
//channel.model = BasicAcousticChannel
channel.model = ProtocolChannelModel
channel.pDetection = 1.0
channel.pDecoding = 1.0
channel.communicationRange = range
channel.detectionRange = range
channel.interferenceRange = range

int nodeCount = 4

println "Starting Routing simulation!"

ArrayList<Tuple2> routes1 = new ArrayList<>()
//routes1.add(new Tuple2(3,2))

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

simulate {
    node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1))
        container.add 'router', new Router()
//        container.add 'router_init', new RouteInitialiser((Tuple2[])routes1.toArray())
        container.add 'rdp', new RouteDiscoveryProtocol()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '2', address: 2, location: [nodeDistance, 0, -50.m], shell: 5001, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'router', new Router()
        container.add 'rdp', new RouteDiscoveryProtocol()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '3', address: 3, location: [nodeDistance*2, 0, -50.m], shell: 5002, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(3))
        container.add 'router', new Router()
        container.add 'rdp', new RouteDiscoveryProtocol()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '4', address: 4, location: [nodeDistance*3, 0, -50.m], shell: 5003, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(4))
        container.add 'router', new Router()
        container.add 'rdp', new RouteDiscoveryProtocol()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
}
