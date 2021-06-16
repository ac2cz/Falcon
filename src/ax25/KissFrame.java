package ax25;

import java.util.ArrayList;
import java.util.Arrays;
import com.g0kla.telem.server.STPable;
import pacSat.frames.FrameException;

public class KissFrame implements STPable {
	public static final int FEND = 0xc0;
	public static final int FESC = 0xdb;
	public static final int TFEND = 0xdc;
	public static final int TFESC = 0xdd;

	public static final int DATA_FRAME = 0x00;
	public static final int TX_DELAY = 0x01;
	public static final int PERSISTENCE = 0x02;
	public static final int SLOT_TIME = 0x03;
	public static final int TX_TAIL = 0x04;
	public static final int FULL_DUPLEX = 0x05;
	public static final int SET_HARDWARE = 0x06;
	public static final int RETURN = 0xff;

	//int pid;
	//byte command;
	int length;
	int bytesAdded;
	int commandCode;
	int portIndex;
	int[] sourceBytes;
	int[] bytes;
	int prevByte = 0x00;
	boolean foundStart = false;
	boolean frameFull = false;
	
	/**
	 * Constructor that takes a set of bytes, most likely from a UI frame, and builds the kiss frame ready for transmission
	 * This handles escaping and FEND wrapping
	 * @param ui
	 */
	public KissFrame(int portIndex, int command, int[] data) {
		ArrayList<Integer> byteList = new ArrayList<Integer>();
		int commandByte = (portIndex << 4) & 0xf0;
		commandByte = commandByte | (command & 0xf);
		byteList.add(FEND);
		byteList.add(commandByte);
		for (int i : data) {
			if (i == FEND) {
				byteList.add(FESC);
				byteList.add(TFEND);
			} else if (i == FESC) {
				byteList.add(FESC);
				byteList.add(TFESC);				
			} else {
				byteList.add(i);
			}
		}
		byteList.add(FEND);
		int j = 0;
		bytes = new int[byteList.size()];
		for (Integer in : byteList)
			bytes[j++] = in;
	}
	
	public KissFrame() {
		bytes = new int[2048]; // practical packet limit size, this is resized once final FEND received
		sourceBytes = new int[2048]; 
		length = 0; // init to zero.  Grows as data added.
	}

	public int[] getRawBytes() {
		return sourceBytes;
	}
	
	public int[] getDataBytes() {
		return bytes;
	}

	
	/**
	 * Returns true if we can add the byte, false once the frame is full
	 * @param b
	 * @return
	 */
	public boolean add(int b) {
		sourceBytes[bytesAdded++] = b;
		if (b == FESC) {
			prevByte = b;
			return true;
		}
		if (b == FEND) {
			if (length == 0) {
				if (foundStart) //ignore multiple FEND at start of frame
					return true;
				foundStart = true;
				prevByte = FEND;
				return true;
			} else {
				frameFull = true;
				bytes = Arrays.copyOfRange(bytes, 0, length);
				sourceBytes = Arrays.copyOfRange(sourceBytes, 0, bytesAdded);
				return false;
			}
		}
		if (prevByte == FEND) {
			prevByte = b; 
			commandCode = b & 0x0f;
			portIndex = b & 0xf0;
			return true;
		}
		if (prevByte == FESC)
			if (b == TFEND) {
				bytes[length++] = FEND;
				prevByte = b;
				return true;
			} else if (b == TFESC) {
				bytes[length++] = FESC;
				prevByte = b;
				return true;
			} else {
				System.err.println("ERROR: ADDED ESC WITHOUT TFESC");
				bytes[length++] = FESC;
			}
		if (!foundStart) return true;
		bytes[length++] = b;
		prevByte = b;
		return true;
	}
	
	/**
	 * PACSATS are Little Endian
	 * @param by
	 * @return
	 */
	public static long getLongFromBytes(int[] by) {
		long value = 0;
		for (int i = 0; i < by.length; i++) {
		   value += (by[i] & 0xffL) << (8 * i);
		}
		return value;
	}
	
	public static int getIntFromBytes(int[] by) {
		int value = 0;
		for (int i = 0; i < by.length; i++) {
		   value += (by[i] & 0xff) << (8 * i);
		}
		return value;
	}
		
	public static int[] littleEndian2(long in) {
	int[] b = new int[2];
	
	b[1] = (int)((in >> 8) & 0xff);
	b[0] = (int)((in >> 0) & 0xff);
	return b;
}
	
	public static int[] littleEndian4(long in) {
		int[] b = new int[4];
		
		b[3] = (int) ((in >> 24) & 0xff);
		b[2] = (int) ((in >> 16) & 0xff);
		b[1] = (int) ((in >> 8) & 0xff);
		b[0] = (int) ((in >> 0) & 0xff);
		return b;
	}
	
	public static boolean[] intToBin8(int word) {
		boolean b[] = new boolean[8];
		for (int i=0; i<8; i++) {
			if (((word >>i) & 0x01) == 1) b[7-i] = true; else b[7-i] = false; 
		}
		return b;
	}
	
	public String toByteString() {
		String s = "";
		for (int b : bytes) {
			s = s + Integer.toHexString(b&0xff);
		}
		return s;
	}
	
	public String toString() {
		String s = "";
		for (int b : bytes) {
			char ch = (char) b;
			s = s + ch;
		}
		return s;
	}
	
	// Test routine
	public static final void main(String[] argc) throws FrameException {
		KissFrame kiss = new KissFrame();
		//int[] by = {0xC0,0x00,0xA0,0x84,0x98,0x92,0xA6,0xA8,0x00,0xA0,0x8C,0xA6,0x66,0x40,0x40,
		//		0x17,0x03,0xF0,0x50,0x42,0x3A,0x20,0x45,0x6D,0x70,0x74,0x79,0x2E,0x0D,0xC0};
	//	int[] by = {0xC0,0x00,0x96,0x68,0x96,0x88,0xA4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x73,0xC0};
	//	int[] by = {0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x31,0xC0};
		int[] by = {0xC0,0x00,0x96,0x68,0x96,0x88,0xA4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x84,0xF0,0x00,0x06,0xC0};  // example I FRAME WITH DATA
	//	int[] by = {0xC0,0x00,0x96,0x68,0x96,0x88,0xA4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x81,0xC0};
	//	int[] by = {0xC0,0x00,0xE6,0x59,0x86,0xFF,0x6C,0xF5,0xAE,0x41,0xBE,0xA6,0xC0};
		for (int b : by)
			kiss.add(b);
		Ax25Frame bf = new Ax25Frame(kiss);
		System.out.println(kiss);
		System.out.println(kiss.length);
		int[] raw = kiss.getRawBytes();
		for (int b : raw)
			System.out.print(b + " ");
		System.out.println("");
		
		System.out.println(bf);
	}
}
