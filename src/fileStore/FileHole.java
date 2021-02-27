package fileStore;

import java.io.Serializable;

import ax25.KissFrame;
import common.Log;

public class FileHole implements HoleLimits, Comparable<FileHole>, Serializable {

	private static final long serialVersionUID = 1L;
	public static final int SIZE = 5;
	long offset;
	int length;
	int[] bytes = new int[5];
	
	public static final int MAX_HOLE_LENGTH = 0xFFFF; //2^16;
	public static final long MAX_OFFSET = 0xFFFFFF; //2^24;
	
	public FileHole(long first, long last) {
		offset = first;
		if (offset > MAX_OFFSET)
			offset = MAX_OFFSET;
		length = (int)(last - offset + 1);
		if (length > MAX_HOLE_LENGTH) // make sure we limit to the maximum hole size
			length = MAX_HOLE_LENGTH;
		setBytes();
	}

	public long getFirst() {
		return offset;
	}
	public long getLast() {
		return offset + length -1;
	}
	
	private void setBytes() {
		int[] offby = KissFrame.littleEndian4(offset);
		bytes[0] = offby[0] & 0xff; // lsb of lower 16 bits
		bytes[1] = offby[1] & 0xff;
		bytes[2] = offby[2] & 0xff; // msb of 24 bit number
		if (length >= 0xFFFF) {
			bytes[3] = 0xff; // lsb of length
			bytes[4] = 0xff;			
		} else {
			int[] lenby = KissFrame.littleEndian2(length);
			bytes[3] = lenby[0] & 0xff; // lsb of length
			bytes[4] = lenby[1] & 0xff;
		}
		
//		int[] by2 = {bytes[0],bytes[1],bytes[2]};
//		long offset = KissFrame.getLongFromBytes(by2);
//		int[] by3 = {bytes[3],bytes[4]};
//		int length = KissFrame.getIntFromBytes(by3);
//		Log.println(" Made Hole: " + offset + " " + length);
		
	}
	public int[] getBytes() {
		return bytes;
	}

	@Override
	public int compareTo(FileHole fh) {
		if (offset > fh.offset) return 1;
		if (offset < fh.offset) return -1;
		return 0;
	}
	
	public String toString () {
		return getFirst() + " " + length;
	}
}
