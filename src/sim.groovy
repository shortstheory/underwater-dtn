//!Simulation
/// Output trace file: logs/trace.nam

import org.arl.fjage.*
import dtn.*
import org.arl.fjage.RealTimePlatform
import org.arl.unet.link.*
import org.arl.unet.sim.NamTracer

//platform = RealTimePlatform
platform = DiscreteEventSimulator


println "Starting simulation!"

def T = 3.hour

//def testStack = { container ->
//  container.add 'link', new ReliableLink()
//  container.add 'dtnlink', new DtnLink()
//  container.add 'testagent', new DatagramGenerator()
//}

int[] dest1 = [2]
int[] dest2 = [1]
int[] dest3 = [1, 2]

def trace = new NamTracer()
trace.open('logs/trace.nam')

simulate T, {
    node '1', address: 1, location: [0, 0, 0], shell: 5000, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink()
        //    container.add 'mac', new CSMA()
        container.add 'testagent', new DatagramGenerator(dest1, 300*1000)
    }
    node '2', address: 2, location: [200.m, 0, 0], shell: 5001, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink()
        container.add 'testagent', new DatagramGenerator(dest2, 300*1000)
        //    container.add 'mac', new CSMA()
    }
    node '3', address: 3, location: [0, 200.m, 0], shell: true, stack: { container ->
        container.add 'link', new ReliableLink()
        container.add 'dtnlink', new DtnLink()
        container.add 'testagent', new DatagramGenerator(dest3, 300*1000)
        //    container.add 'mac', new CSMA()
    }
    float loss = trace.txCount ? 100*trace.dropCount/trace.txCount : 0
    println sprintf('%6d\t\t%6d\t\t%5.1f\t\t%7.3f\t\t%7.3f',
            [trace.txCount, trace.rxCount, loss, trace.offeredLoad, trace.throughput])
}


