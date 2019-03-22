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
        println("Datagrams: " + "TX: " + datagramsSent + " RX: " + datagramsReceived + " S: " + datagramsSuccess + " F: " + datagramsFailure)
        println("Payloads:  " + "TX: " + payloadsSent + " RX: " + payloadsReceived + " S: " + payloadsSuccess + " F: " + payloadsFailure)
        println()
    }
}
