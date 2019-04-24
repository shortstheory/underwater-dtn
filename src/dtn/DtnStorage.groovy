/***************************************************************************
 *  Copyright (C) 2019 by Arnav Dhamija <arnav.dhamija@gmail.com>          *
 *  Distributed under the MIT License (http://opensource.org/licenses/MIT) *
 ***************************************************************************/

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
    private int idCounter
    private HashMap<String, DtnPduMetadata> metadataMap
    protected Logger log = Logger.getLogger(getClass().getName());

    /**
     * PDU Structure
     * |TTL (24)                          |         Protocol (8)|
     * |TBC Bit (1) AltBit (1) Payload ID (8) Start Pointer (23)|
     */

    public static final String TTL_MAP            = "ttl"
    public static final String PROTOCOL_MAP       = "protocol"
    public static final String TBC_BIT_MAP        = "tbc"
    public static final String UNIQUE_ID_MAP = "pid"
    public static final String START_PTR_MAP      = "startptr"

    public static final int EXTRA_FILE_DATA = 8

    DtnStorage(DtnLink link, String dir) {
        directory = dir
        dtnLink = link
        File file = new File(directory)
        if (!file.exists()) {
            file.mkdir()
        }
        idCounter = 0
        payloadList = Arrays.asList(new String[DtnLink.MAX_UNIQUE_ID])
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

    /**
     * Recovers the pending datagrams which are in a node's non-volatile storage after reboot
     * Run only once, on DtnLink's startup
     */
    void recoverMetadataMap() {
        File[] files = new File(directory).listFiles()
        for (File file : files) {
            String messageID = file.getName()
            if (file.isFile()) {
                DataInputStream dis = new DataInputStream(new FileInputStream(file))
                try {
                    int nextHop = dis.readInt()
                    int expiryTime = dis.readInt()
                    if (nextHop != DtnPduMetadata.INBOUND_HOP) {
                        metadataMap.put(messageID, new DtnPduMetadata(nextHop, expiryTime))
                        metadataMap.get(messageID).size = getDatagramSize(messageID)
                    } else {
                        file.delete()
                    }
                } catch (IOException e) {
                    log.warning("Could not recover metadata for " + messageID)
                }
            } else {
                log.fine("Discarding file " + messageID)
            }
        }
    }

    int getDatagramSize(String messageID) {
        File file = new File(directory, messageID)
        return (int)(file.length() - EXTRA_FILE_DATA - DtnLink.HEADER_SIZE)
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

    int generateUniqueID() {
        return dtnLink.random.nextInt(256)
    }

    boolean saveFragment(int src, int uniqueID, int protocol, int startPtr, int ttl, byte[] data) {
        String messageID = Integer.toString(src) + "_" + Integer.toString(uniqueID)
        File file = new File(directory, messageID)
        int fragmentSize = data.length - DtnLink.HEADER_SIZE
        if (!file.exists() && !startPtr) {
            OutputPDU pdu = encodePdu(data, ttl, protocol, false, uniqueID, 0)
            FileOutputStream fos = new FileOutputStream(file)
            DataOutputStream dos = new DataOutputStream(fos)
            // Only thing the tracking map is doing for INBOUND fragments is maintaining TTL and delivered status
            int nextHop = DtnPduMetadata.INBOUND_HOP
            int expiryTime = ttl + dtnLink.currentTimeSeconds()
            metadataMap.put(messageID, new DtnPduMetadata(nextHop, expiryTime))
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
        } else if (file.exists()
                && startPtr <= getDatagramSize(messageID)
                && (startPtr + fragmentSize) > getDatagramSize(messageID)) {
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
            // No point of the payload if sent out of order, just delete it lah!
            file.delete()
            metadataMap.remove(messageID)
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
        // FIXME: payload ID can go here
        OutputPDU outputPDU = encodePdu(data, ttl, protocol, false, generateUniqueID(), 0)
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
            if (dtnLink.currentTimeSeconds() > metadata.expiryTime) {
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

    void removeDatagram(String messageID) {
        File file = new File(directory, messageID)
        file.delete()
        metadataMap.remove(messageID)
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

    static OutputPDU encodePdu(byte[] data, int ttl, int protocol, boolean tbc, int uniqueID, int startPtr) {
        int dataLength = (data == null) ? 0 : data.length
        OutputPDU pdu = new OutputPDU(dataLength + DtnLink.HEADER_SIZE)
        pdu.write24(ttl)
        int upperByte = 0
        upperByte |= (byte)((tbc) ? (1 << 7) : 0)
        upperByte |= (byte)((protocol & 0x3F))
        pdu.write8(upperByte)
        pdu.write8(uniqueID)
        pdu.write24(startPtr)
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
        byte upperByte = (byte)pdu.read8()
        int tbc = ((upperByte & 0x80) >>> 7)
        int protocol = (upperByte & 0x3F)
        map.put(TBC_BIT_MAP, tbc)
        map.put(PROTOCOL_MAP, protocol)
        map.put(UNIQUE_ID_MAP, (int)pdu.read8())
        map.put(START_PTR_MAP, pdu.read24())
        return map
    }
}
