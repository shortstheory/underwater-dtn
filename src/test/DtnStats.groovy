package test

class DtnStats {
    int payloadsSent      = 0
    int payloadsSuccess   = 0
    int payloadsFailure   = 0
    int payloadsReceived  = 0

    int datagramsSent     = 0
    int datagramsSuccess  = 0
    int datagramsFailure  = 0
    int datagramsReceived = 0

    int[] msgRecv = new int[100]
    HashSet<String> uniqueDatagrams = new HashSet<>()

    void printStats() {
        println("\nStats: ")
        println("Datagrams: " + "TX: " + datagramsSent + " RX: " + datagramsReceived + " S: " + datagramsSuccess + " F: " + datagramsFailure)
        println("Payloads:  " + "TX: " + payloadsSent + " RX: " + payloadsReceived + " S: " + payloadsSuccess + " F: " + payloadsFailure)
        println("Unique Datagrams - " + uniqueDatagrams.size())
        for (int i = 0; i < 100; i++) {
            if (msgRecv[i]) {
                println(i + "->" + msgRecv[i])
            }
        }
    }
}
