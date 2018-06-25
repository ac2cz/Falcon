package pacSat.frames;

import java.util.ArrayList;

public class KissFrame {
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
	int commandCode;
	int portIndex;
	int[] rawBytes;
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
		rawBytes = new int[2048]; // practical packet limit size, this is resized once final FEND received
		length = 0; // init to zero.  Grows as data added.
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
				bytes = new int[length+1];
				for (int i=0; i<=length; i++)
					bytes[i] = rawBytes[i];
				rawBytes = null;
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
			if (b == TFEND)
				b = FEND;
			else if (b == TFESC)
				b = FESC;
			else {
				rawBytes[length++] = FESC;
			}
		if (!foundStart) return true;
		rawBytes[length++] = b;
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
	
	public static int getIntFromBytes(int b1, int b2) {
		int value =  ((b2 & 0xff) << 8)
		     | ((b1 & 0xff) << 0);
		return value;
	}
	
	public static int[] bigEndian4(long in) {
		int[] b = new int[4];
		
		b[0] = (int) ((in >> 24) & 0xff);
		b[1] = (int) ((in >> 16) & 0xff);
		b[2] = (int) ((in >> 8) & 0xff);
		b[3] = (int) ((in >> 0) & 0xff);
		return b;
	}
	
	public static int[] bigEndian2(long in) {
		int[] b = new int[2];
		
		b[0] = (int)((in >> 8) & 0xff);
		b[1] = (int)((in >> 0) & 0xff);
		return b;
	}
	
	public static int[] bigEndian2(int in) {
		int[] b = new int[2];
		
		b[0] = (int)((in >> 8) & 0xff);
		b[1] = (int)((in >> 0) & 0xff);
		return b;
	}
	
	public static int[] bigEndian4(int in) {
		int[] b = new int[4];
		
		b[0] = (int)((in >> 24) & 0xff);
		b[1] = (int)((in >> 16) & 0xff);
		b[2] = (int)((in >> 8) & 0xff);
		b[3] = (int)((in >> 0) & 0xff);
		return b;
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
//	public static final void main(String[] argc) throws FrameException {
//		KissFrame kiss = new KissFrame();
//		//byte[] by = {0x00, 0x10, (byte) 0xdb, (byte) 0xdc, 0x20};
//		int[] by = {0xC0,0x00,0xA0,0x84,0x98,0x92,0xA6,0xA8,0x00,0xA0,0x8C,0xA6,0x66,0x40,0x40,
//				0x17,0x03,0xF0,0x50,0x42,0x3A,0x20,0x45,0x6D,0x70,0x74,0x79,0x2E,0x0D,0xC0};
//				
//		for (int b : by)
//			kiss.add((byte)b);
//		//System.out.println(ui);
//		UiFrame bf = new UiFrame(kiss);
//		System.out.println(bf);
//	}
}
