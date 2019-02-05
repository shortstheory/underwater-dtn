//!Simulation

import org.arl.fjage.DiscreteEventSimulator
import dtn.*
import org.arl.unet.link.*

//platform = RealTimePlatform
platform = DiscreteEventSimulator


println "Starting simulation!"

def T = 24.hour

//def testStack = { container ->
//  container.add 'link', new ReliableLink()
//  container.add 'dtnlink', new DtnLink()
//  container.add 'testagent', new DatagramGenerator()
//}


simulate T, {
    node '1', address: 1, location: [0, 0, 0], shell: 5000, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink()
//    container.add 'mac', new CSMA()
        container.add 'testagent', new DatagramGenerator(2)
    }
  node '2', address: 2, location: [200.m, 0, 0], shell: true, stack: { container ->
    container.add 'link', new ReliableLink()
    container.add 'dtnlink', new DtnLink()
    container.add 'testagent', new DatagramGenerator(1)
//    container.add 'mac', new CSMA()
  }
}
