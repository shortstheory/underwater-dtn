package dtn

import com.google.gson.Gson
import groovy.transform.CompileStatic
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

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
    public int datagrams_resent = 0
    public int datagrams_expired = 0
    public int frame_collisions = 0
    public int bad_frames = 0

    public ArrayList<Integer> delivery_times = new ArrayList<>()

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

    double getMean(ArrayList<Integer> list) {
        DescriptiveStatistics stats = new DescriptiveStatistics()
        for (Integer x : list) {
            stats.addValue(x)
        }
        return stats.getMean()
    }

    double getStandardDeviation(ArrayList<Integer> list) {
        DescriptiveStatistics stats = new DescriptiveStatistics()
        for (Integer x : list) {
            stats.addValue(x)
        }
        return stats.getStandardDeviation()
    }

    void printValues() {
        println sprintf('%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%3d\t\t%5.3f\t%5.3f\t%5.2f\t%5.2f',
                [address,
                datagrams_sent,
                datagrams_received,
                datagrams_failed,
                datagrams_success,
                datagrams_requested,
                datagrams_buffer,
                datagrams_resent,
                datagrams_expired,
                beacons_snooped,
                frame_collisions,
                bad_frames,
                (float)datagrams_failed/datagrams_sent,
                (float)datagrams_success/datagrams_requested,
                getMean(delivery_times),
                getStandardDeviation(delivery_times)
                ])
    }

    static public void printAllStats(int nodes) {
        println '''Node\tTx  \tRx  \tFail\tSuc \tReq \tStor\tRsnt\tExpr\tBeac\tColl\tBadF\tF%  \tTx%\t\tMean\tSD'''
        println("")
        for (int stat = 1; stat <= nodes; stat++) {
            Gson gson = new Gson()
            String json = new File(Integer.toString(stat)+".json").text
            DtnStats dtnStats = gson.fromJson(json, DtnStats.class)
            dtnStats.printValues()
        }
    }
}
