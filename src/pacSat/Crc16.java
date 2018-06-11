package pacSat;

/******************************************************************************
 *  Compilation:  javac CRC16CCITT.java
 *  Execution:    java CRC16CCITT s
 *  Dependencies: 
 *  
 *  Reads in a sequence of bytes and prints out its 16 bit
 *  Cylcic Redundancy Check (CRC-CCIIT 0xFFFF).
 *
 *  1 + x + x^5 + x^12 + x^16 is irreducible polynomial.
 *
 *  % java CRC16-CCITT 123456789
 *  CRC16-CCITT = 29b1
 *
 ******************************************************************************/

public class Crc16 { 

    public static int calc(int[] bytes) { 
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 

        // byte[] testBytes = "123456789".getBytes("ASCII");

        for (int b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }
    
    public static int calc_xmodem(byte[] bytes) { 
        int crc = 0;
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 

        for (byte b : bytes) {
        	crc = crc ^ b << 8;
            for (int i = 0; i < 8; i++) {
            	if ((crc  & 0x8000) == 1)
            		crc = crc << 1 ^ (polynomial & 0xffff);
            	else
            		crc = crc << 1;   	
            }
        }
        crc &= 0xffff;
        return crc;
    }
    
    // 2E 0D - little endian, so 0D2E is checksum
    public static void main(String[] args) {
    	byte[] by= {(byte) 0xC0,0x00,(byte) 0xA0,(byte) 0x84,(byte) 0x98,(byte) 0x92,(byte) 0xA6,(byte) 0xA8,
    			0x00,(byte) 0xA0,(byte) 0x8C,(byte) 0xA6,0x66,0x40,0x40,0x17,
    		0x03,(byte) 0xF0,0x50,0x42,0x3A,0x20,0x45,0x6D,0x70,0x74,0x79,0x2E,0x0D,(byte) 0xC0};
    	
    	byte[] by2 = {(byte) 0x00,(byte) 0xA0,(byte) 0x84,(byte) 0x98,(byte) 0x92,(byte) 0xA6,(byte) 0xA8,
    			0x00,(byte) 0xA0,(byte) 0x8C,(byte) 0xA6,0x66,0x40,0x40,0x17,
    		0x03,(byte) 0xF0,0x50,0x42,0x3A,0x20,0x45,0x6D,0x70,0x74,0x79};
    	
    	}

}