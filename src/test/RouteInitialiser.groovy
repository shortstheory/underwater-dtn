package test

import groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.RouteDiscoveryNtf

@CompileStatic
class RouteInitialiser extends UnetAgent {
    Tuple2[] routes

    RouteInitialiser(Tuple2[] r) {
        routes = r
    }

    @Override
    protected void startup() {
        AgentID dtnlink = agent("dtnlink")
        AgentID router = agent("router")
        // why does hopcount matter?
        for (Tuple2 route : routes) {
            router.send(new RouteDiscoveryNtf(to: (int)route.first, nextHop: (int)route.second, reliability: true, link: dtnlink))
        }
    }
}