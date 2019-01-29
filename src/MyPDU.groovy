 import java.nio.ByteOrder
 import org.arl.unet.PDU

 class MyPDU extends PDU {
   void format() {
     //length(16)                     // 16 byte PDU
     order(ByteOrder.BIG_ENDIAN)    // byte ordering is big endian
     uint8('type')                  // 1 byte field 'type'
  //   uint8(0x01)                    // literal byte 0x01
  //   filler(2)                      // 2 filler bytes
     uint16('data')                 // 2 byte field 'data' as unsigned short
     padding(0xff)                  // padded with 0xff to make 16 bytes
   }
}