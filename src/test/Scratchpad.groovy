package test

import dtn.DtnLink
import dtn.DtnStorage
import groovy.transform.CompileStatic
import org.arl.unet.InputPDU
import org.arl.unet.OutputPDU

@CompileStatic
class Scratchpad {
    static OutputPDU encodePdu(byte[] data, int ttl, int protocol, boolean tbc, int payloadID, int startPtr) {
        int dataLength = (data == null) ? 0 : data.length
        OutputPDU pdu = new OutputPDU(dataLength + DtnLink.HEADER_SIZE)
        pdu.write24(ttl)
        pdu.write8(protocol)
        int payloadFields
//        int res = 1 << 31
        payloadFields = (tbc) ? (1 << 31) : 0
        payloadFields |= (payloadID << 23)
        payloadFields |= startPtr
        pdu.write32(payloadFields)
        if (data != null) {
            pdu.write(data)
        }
        return pdu
    }

    static HashMap decodePdu(byte[] pduBytes) {
        if (pduBytes.length < DtnLink.HEADER_SIZE) {
            return null
        }
        InputPDU pdu = new InputPDU(pduBytes)
        HashMap<String, Integer> map = new HashMap<>()
        map.put(DtnStorage.TTL_MAP, (int)pdu.read24())
        map.put(DtnStorage.PROTOCOL_MAP, (int)pdu.read8())
        int payloadFields = (int)pdu.read32()
        int tbc = ((payloadFields & 0x80000000).toInteger() >>> 31)
        int payloadID = ((payloadFields & 0x7F800000) >>> 23)
        int startPtr = (payloadFields & 0x007FFFFF)
        map.put(DtnStorage.TBC_BIT_MAP, tbc)
        map.put(DtnStorage.PAYLOAD_ID_MAP, payloadID)
        map.put(DtnStorage.START_PTR_MAP, startPtr)
        return map
    }

    public static void main(String[] args) {
        byte[] data = "AAAAAAAAAA".getBytes()
        int ttl = 2000
        int protocol = 20
        boolean tbc = true
        int id = 255
        int startPtr = 8000*1000
        byte[] res = encodePdu(data, ttl, protocol, tbc, id, startPtr).toByteArray()
        HashMap map = decodePdu(res)
        println("done")
    }
}
