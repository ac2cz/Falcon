package fileStore;

import java.util.Date;

import pacSat.frames.KissFrame;

public class DirHole extends Hole {
	Date fromDate;
	Date toDate;
	int[] bytes = new int[8];
	
	public DirHole(Date from, Date to) {
		fromDate = from;
		toDate = to;
		setBytes();
	}

	private void setBytes() {
		int[] fromby = KissFrame.littleEndian4(fromDate.getTime()/1000);
		bytes[0] = fromby[0] & 0xff; 
		bytes[1] = fromby[1] & 0xff;
		bytes[2] = fromby[2] & 0xff; 
		bytes[3] = fromby[3] & 0xff;
		int[] toby = KissFrame.littleEndian4(toDate.getTime()/1000);
		bytes[4] = toby[0] & 0xff; 
		bytes[5] = toby[1] & 0xff;
		bytes[6] = toby[2] & 0xff; 
		bytes[7] = toby[3] & 0xff;
	}
	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	public String toString () {
		return fromDate + " " + toDate;
	}

}
