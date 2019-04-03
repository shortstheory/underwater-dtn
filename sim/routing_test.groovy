//!Simulation
/// Output trace file: logs/trace.nam

import org.apache.commons.io.FileUtils
import org.arl.fjage.*
import dtn.*
import org.arl.unet.link.*
import org.arl.unet.net.RouteDiscoveryNtf
import org.arl.unet.net.Router
import org.arl.unet.transport.SWTransport
import org.arl.unet.sim.NamTracer
import org.arl.unet.sim.channels.BasicAcousticChannel
import org.arl.unet.sim.MotionModel
import org.arl.unet.shell.*
import test.RouteInitialiser

import java.nio.file.Files

platform = RealTimePlatform

channel.model = BasicAcousticChannel

int nodeCount = 4

println "Starting Routing simulation!"

ArrayList<Tuple2> routes1 = new ArrayList<>()
routes1.add(new Tuple2(3,2))

for (int f = 1; f <= nodeCount; f++) {
    FileUtils.deleteDirectory(new File(Integer.toString(f)))
    Files.deleteIfExists((new File(Integer.toString(f)+".json")).toPath())
}

simulate {
    node '1', address: 1, location: [0, 0, -50.m], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(1))
        container.add 'router', new Router()
        container.add 'router_init', new RouteInitialiser((Tuple2[])routes1.toArray())
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '2', address: 2, location: [200.m, 0, -50.m], shell: 5001, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(2))
        container.add 'router', new Router()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '3', address: 3, location: [400.m, 0, -50.m], shell: 5002, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(3))
        container.add 'router', new Router()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
    node '4', address: 4, location: [600.m, 0, -50.m], shell: 5003, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink(Integer.toString(4))
        container.add 'router', new Router()
        container.shell.addInitrc "/home/nic/nus/UnetStack3-prerelease-20190128/etc/fshrc.groovy"
    }
}
