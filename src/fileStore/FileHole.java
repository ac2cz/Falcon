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
		length = (int)(last - offset + 1);
		setBytes();
	}

	public long getFirst() {
		return offset;
	}
	public long getLast() {
		return offset + length -1;
	}
	
	/**
	 * Set the bytes ready for transmission.  Make sure the lengths do not exceed
	 * what the spacecraft can cope with.  We truncate them here rather than in the constructor
	 * so we hold an accurate picture of the whole file and holes on disk, even if the holes are 
	 * too long.
	 * 
	 */
	private void setBytes() {
		long offset = this.offset;
		if (offset > MAX_OFFSET)
			offset = MAX_OFFSET;
		int length = this.length;
		if (length > MAX_HOLE_LENGTH) // make sure we limit to the maximum hole size
			length = MAX_HOLE_LENGTH;
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
		setBytes();
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
