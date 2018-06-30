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
		int[] fromby = KissFrame.bigEndian4(fromDate.getTime());
		bytes[0] = fromby[0] & 0xff; 
		bytes[1] = fromby[1] & 0xff;
		bytes[2] = fromby[2] & 0xff; 
		bytes[3] = fromby[3] & 0xff;
		int[] toby = KissFrame.bigEndian4(toDate.getTime());
		bytes[0] = toby[0] & 0xff; 
		bytes[1] = toby[1] & 0xff;
		bytes[2] = toby[2] & 0xff; 
		bytes[3] = toby[3] & 0xff;
	}
	@Override
	public int[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

}
