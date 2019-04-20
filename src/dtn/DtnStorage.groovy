package dtn

import com.sun.istack.internal.Nullable
import groovy.transform.CompileStatic
import org.arl.unet.DatagramReq
import org.arl.unet.InputPDU
import org.arl.unet.OutputPDU

import java.nio.file.Files
import java.util.logging.Logger

/**
 * Helper class for managing the datagrams saved to the node's non-volatile storage
 * Manages the deletion of expired datagrams and encoding datagrams with PDU headers
 */
@CompileStatic
class DtnStorage {
    private final String directory
    private DtnLink dtnLink
    private List<String> payloadList
    private int payloadCounter
    private HashMap<String, DtnPduMetadata> metadataMap
    protected Logger log = Logger.getLogger(getClass().getName());

    /**
     * PDU Structure
     * |TTL (24)                          |         Protocol (8)|
     * |TBC Bit (1) AltBit (1) Payload ID (8) Start Pointer (23)|
     */

    public static final String TTL_MAP            = "ttl"
    public static final String PROTOCOL_MAP       = "protocol"
    public static final String ALT_BIT_MAP        = "alt"
    public static final String TBC_BIT_MAP        = "tbc"
    public static final String PAYLOAD_ID_MAP     = "pid"
    public static final String START_PTR_MAP      = "startptr"

    public static final int EXTRA_FILE_DATA = 8

    DtnStorage(DtnLink link, String dir) {
        directory = dir
        dtnLink = link
        File file = new File(directory)
        if (!file.exists()) {
            file.mkdir()
        }
        payloadCounter = 0
        payloadList = Arrays.asList(new String[DtnLink.MAX_PAYLOADS])
        metadataMap = new HashMap<>()
    }

    @Nullable DtnPduMetadata getMetadata(String messageID) {
        return metadataMap.get(messageID)
    }

    byte[] readPayload(int src, int payloadID) {
        String filename = Integer.toString(src) + "_" + Integer.toString(payloadID)
        return getMessageData(filename)
    }

    @Nullable byte[] getMessageData(String messageID) {
        byte[] pduBytes = Files.readAllBytes(new File(directory, messageID).toPath())
        if (pduBytes != null) {
            return Arrays.copyOfRange(pduBytes, dtnLink.HEADER_SIZE + EXTRA_FILE_DATA, pduBytes.length)
        }
        return null
    }

    void buildMetadataMap() {
        File[] files = new File(directory).listFiles()
        for (File file : files) {
            if (file.isFile()) {
                String filename = file.getName()
                if (filename.matches("/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$/")) {
                    DataInputStream dis = new DataInputStream(new FileInputStream(file))
                    try {
                        int nextHop = dis.readInt()
                        int expiryTime = dis.readInt()
                        metadataMap.put(filename, new DtnPduMetadata(nextHop, expiryTime))
                    } catch (IOException e) {
                        log.warning("Could not recover metadata for " + filename)
                    }
                } else {
                    log.fine("Discarding file " + filename)
                }
            }
        }
    }

    @Nullable HashMap getPDUInfo(String messageID) {
        RandomAccessFile raf = new RandomAccessFile(new File(directory, messageID), "r")
        try {
            byte[] pduBytes = new byte[DtnLink.HEADER_SIZE]
            raf.seek(EXTRA_FILE_DATA)
            if (raf.read(pduBytes) == DtnLink.HEADER_SIZE) {
                return decodePdu(pduBytes)
            }
            // First 8 bytes contain nextHop and the expiryTime, which we need to recreate the metadata map
            return null
        } catch (Exception e) {
            log.warning("Message ID " + messageID + " not found " + dtnLink.currentTimeSeconds())
            return null
        } finally {
            raf.close()
        }
    }

    ArrayList<String> getNextHopDatagrams(int nextHop) {
        ArrayList<String> data = new ArrayList<>()
        for (Map.Entry<String, DtnPduMetadata> entry : metadataMap.entrySet()) {
            String messageID = entry.getKey()
            DtnPduMetadata metadata = entry.getValue()
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime
                || metadata.delivered
                || metadata.getMessageType() == DtnPduMetadata.MessageType.INBOUND) {
                // we don't delete here, as it will complicate the logic
                // instead, it will be deleted by the next GC sweep
                continue
            }
            if (metadata.nextHop == nextHop) {
                data.add(messageID)
            }
        }
        return data
    }

    int getPayloadID(String messageID) {
        int id
        // Yes, it's not the most efficient. But we will only have 256 strings to go through at most
        if ((id = payloadList.indexOf(messageID)) != -1) {
            // We can't have payloadID = 0 as that will make the receiver think we're sending a regular Datagram
            return id + 1
        }
        payloadCounter %= DtnLink.MAX_PAYLOADS
        payloadList[payloadCounter] = messageID
        return ++payloadCounter
    }

    boolean saveFragment(int src, int payloadID, int protocol, int startPtr, int ttl, byte[] data) {
        String filename = Integer.toString(src) + "_" + Integer.toString(payloadID)
        File file = new File(directory, filename)

        if (file.exists()) {
            // FIXME: if OoO just discard the payload
            RandomAccessFile raf = new RandomAccessFile(file, "rw")
            raf.seek(EXTRA_FILE_DATA + DtnLink.HEADER_SIZE + startPtr)
            try {
                raf.write(data)
                return true
            } catch (IOException e) {
                return false
            } finally {
                raf.close()
            }
        } else {
            OutputPDU pdu = encodePdu(data, ttl, protocol, false, false, payloadID, 0)
            FileOutputStream fos = new FileOutputStream(file)
            DataOutputStream dos = new DataOutputStream(fos)
            // Only thing the tracking map is doing for INBOUND fragments is maintaining TTL and delivered status
            int nextHop = DtnPduMetadata.INBOUND_HOP
            int expiryTime = ttl + dtnLink.currentTimeSeconds()
            metadataMap.put(filename, new DtnPduMetadata(nextHop, expiryTime))
            try {
                dos.writeInt(nextHop)
                dos.writeInt(expiryTime)
                pdu.writeTo(fos)
                return true
            } catch (IOException e) {
                return false
            } finally {
                dos.close()
            }
        }
    }

    boolean saveDatagram(DatagramReq req) {
        int protocol = req.getProtocol()
        int nextHop = req.getTo()
        int ttl = (Math.round(req.getTtl()))
        String messageID = req.getMessageID()
        byte[] data = req.getData()
        if (data != null && dtnLink.getMTU() < data.length) {
            return false
        }
        OutputPDU outputPDU = encodePdu(data, ttl, protocol, false, false, 0, 0)
        File file = new File(directory, messageID)
        FileOutputStream fos = new FileOutputStream(file)
        DataOutputStream dos = new DataOutputStream(fos)
        try {
            int expiryTime = ttl + dtnLink.currentTimeSeconds()
            dos.writeInt(nextHop)
            dos.writeInt(expiryTime)
            outputPDU.writeTo(fos)
            metadataMap.put(messageID, new DtnPduMetadata(nextHop, expiryTime))
            metadataMap.get(messageID).size = (data == null) ? 0 : data.length
            return true
        } catch (IOException e) {
            log.warning("Could not save file for " + messageID)
            return false
        } finally {
            dos.close()
        }
    }

    void deleteFiles() {
        Iterator it = metadataMap.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next()
            String messageID = (String)entry.getKey()
            DtnPduMetadata metadata = (DtnPduMetadata)entry.getValue()
            if (metadata.delivered || dtnLink.currentTimeSeconds() > metadata.expiryTime) {
                // Delete the file
                File file = new File(directory, messageID)
                file.delete()
                if (metadata.getMessageType() == DtnPduMetadata.MessageType.OUTBOUND) {
                    if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
                        if (metadata.getMessageType() == DtnPduMetadata.MessageType.OUTBOUND) {
                            dtnLink.sendFailureNtf(messageID, metadata.nextHop)
                        }
                    }
                }
                it.remove()
            }
        }
    }


    int getArrivalTime(String messageID) {
        HashMap<String, Integer> map = getPDUInfo(messageID)
        if (map != null) {
            int ttl = map.get(TTL_MAP)
            int expiryTime = getMetadata(messageID).expiryTime
            return expiryTime - ttl
        }
        return -1
    }

    static OutputPDU encodePdu(byte[] data, int ttl, int protocol, boolean alternatingBit, boolean tbc, int payloadID, int startPtr) {
        int dataLength = (data == null) ? 0 : data.length
        OutputPDU pdu = new OutputPDU(dataLength + DtnLink.HEADER_SIZE)
        pdu.write24(ttl & 0x00FFFFFF)
        pdu.write8(protocol)
        int payloadFields
        payloadFields = (tbc) ? (1 << 31) : 0
        payloadFields |= (alternatingBit) ? (1 << 30) : 0
        payloadFields |= ((payloadID & 0x000000FF) << 22)
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
        map.put(TTL_MAP, (int)pdu.read24())
        map.put(PROTOCOL_MAP, (int)pdu.read8())
        int payloadFields = (int)pdu.read32()
        int tbc = ((payloadFields & 0x80000000).toInteger() >>> 31)
        int alternatingBit = ((payloadFields & 0x40000000).toInteger() >>> 30)
        int payloadID = ((payloadFields & 0x3FC00000) >>> 22)
        int startPtr = (payloadFields & 0x003FFFFF)
        map.put(TBC_BIT_MAP, tbc)
        map.put(ALT_BIT_MAP, alternatingBit)
        map.put(PAYLOAD_ID_MAP, payloadID)
        map.put(START_PTR_MAP, startPtr)
        return map
    }
}
