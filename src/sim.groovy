//!Simulation
import org.arl.fjage.RealTimePlatform
import dtn.*
import org.arl.fjage.shell.*
import org.arl.unet.net.*
import org.arl.unet.link.*
import org.arl.unet.nodeinfo.*


platform = RealTimePlatform

println "Starting simulation!"

simulate {
  node '1', address: 1, location: [0, 0, 0], shell: true, stack: { container ->
    container.add 'link', new org.arl.unet.link.ReliableLink()
    container.add 'dtnlink', new DtnLink()
  }
  node '2', address: 2, location: [200.m, 0, 0], shell: false, stack: { container ->
    container.add 'link', new org.arl.unet.link.ReliableLink()
    container.add 'dtnLink', new DtnLink()
  }
}
