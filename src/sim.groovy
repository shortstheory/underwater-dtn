//!Simulation

import org.arl.fjage.DiscreteEventSimulator
import org.arl.fjage.RealTimePlatform
import dtn.*
import org.arl.fjage.shell.*
import org.arl.unet.net.*
import org.arl.unet.link.*
import org.arl.unet.mac.CSMA
import org.arl.unet.nodeinfo.*


//platform = RealTimePlatform
platform = DiscreteEventSimulator


println "Starting simulation!"

def T = 100.second

//def testStack = { container ->
//  container.add 'link', new ReliableLink()
//  container.add 'dtnlink', new DtnLink()
//  container.add 'testagent', new TestAgent()
//}


simulate T, {
    node '1', address: 1, location: [0, 0, 0], shell: 5000, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink()
//    container.add 'mac', new CSMA()
//    container.add 'testagent', new TestAgent(2)
    }
  node '2', address: 2, location: [200.m, 0, 0], shell: true, stack: { container ->
    container.add 'link', new ReliableLink()
    container.add 'dtnlink', new DtnLink()
    container.add 'testagent', new TestAgent(1)
//    container.add 'mac', new CSMA()
  }
}
