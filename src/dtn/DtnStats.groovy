package dtn

import com.google.gson.Gson
import groovy.transform.CompileStatic

import java.nio.file.Files

@CompileStatic
class DtnStats {
    int address

    public int datagrams_requested = 0
    public int datagrams_buffer = 0
    public int beacons_snooped = 0
    public int datagrams_sent = 0
    public int datagrams_received = 0
    public int datagrams_failed = 0
    public int datagrams_success = 0

    DtnStats(int nodeAddress) {
        address = nodeAddress
    }

    void writeToFile() {
        String serialized = new Gson().toJson(this)
        File file = new File(Integer.toString(address)+".json")
        Files.write(file.toPath(), serialized.getBytes())
    }

    void printStats() {
        datagrams_buffer = new File(Integer.toString(address)).listFiles().length
        println "Node " + address + " stats\n========="
        println "DRs sent:        " + datagrams_sent
        println "DRs received:    " + datagrams_received
        println "DRs failed:      " + datagrams_failed
        println "DRs succeeded:   " + datagrams_success
        println "DRs requested:   " + datagrams_requested
        println "DRs in storage:  " + datagrams_buffer
        println "Beacons snooped: " + beacons_snooped
    }
}
