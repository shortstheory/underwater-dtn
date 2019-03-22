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

    void printStats() {
        println("\nStats: ")
        println("Datagrams: " + "TX: " + datagramsSent + " S: " + datagramsSuccess + " F: " + datagramsFailure + " RX: " + datagramsReceived)
        println("Payloads:  " + "TX: " + payloadsSent + " S: " + payloadsSuccess + " F: " + payloadsFailure + " RX: " + payloadsReceived)
        println()
    }
}
