package com.arnavdhamija.dtn

import  groovy.transform.CompileStatic
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.RouteDiscoveryNtf
import org.arl.unet.net.Router
import org.arl.unet.net.RouterParam

@CompileStatic
class RouteInitialiser extends UnetAgent {
    Tuple2[] routes
    AgentID link
    RouteInitialiser(Tuple2[] r, String name) {
        routes = r
        link = agent(name)
    }

    @Override
    protected void startup() {
        AgentID dtnlink = agent("dtnlink")
        AgentID router = agent("router")
        // why does hopcount matter?
        for (Tuple2 route : routes) {
            router.send(new RouteDiscoveryNtf(to: (int)route.first, nextHop: (int)route.second, reliability: true, link: link))
        }
    }
}