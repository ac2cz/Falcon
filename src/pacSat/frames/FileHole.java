package pacSat.frames;

public class FileHole extends Hole {
	long offset;
	int length;
	int[] bytes = new int[5];
	
	public FileHole(long off, int len) {
		offset = off;
		length = len;
		setBytes();
	}

	private void setBytes() {
		int[] offby = KissFrame.bigEndian4(offset);
		bytes[0] = offby[1] & 0xff; // msb of lower 16 bits
		bytes[1] = offby[2] & 0xff;
		bytes[2] = offby[0] & 0xff; // msb of 24 bit number
		int[] lenby = KissFrame.bigEndian2(length);
		bytes[3] = lenby[0] & 0xff; // msb of length
		bytes[4] = lenby[1] & 0xff;
	}
	public int[] getBytes() {
		return bytes;
	}
}
