package fileStore;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ax25.KissFrame;

public class DirHole implements HoleLimits, Comparable<DirHole>, Serializable {

	private static final long serialVersionUID = 1L;
	public static final int SIZE = 8;
	Date fromDate;
	Date toDate;
	long from;
	long to;
	int[] bytes = new int[8];
	
	/**
	 * Make a hole between the two upload dates that are passed.
	 * @param from
	 * @param to
	 */
	public DirHole(Date from, Date to) {
		fromDate = from;
		this.from = fromDate.getTime()/1000;
		toDate = to;
		this.to= toDate.getTime()/1000;
		setBytes();
	}
	
	public DirHole(long from, long to) {
		this.from = from;
		this.to = to;
		setBytes();
	}
	
	public DirHole(int[] data) {
		bytes = data;
		int[] by2 = {bytes[0],bytes[1],bytes[2],bytes[3]};
		from = KissFrame.getLongFromBytes(by2);
		int[] by3 = {bytes[4],bytes[5],bytes[6],bytes[7]};
		to = KissFrame.getLongFromBytes(by3);
	}

	private void setBytes() {
		int[] fromby = KissFrame.littleEndian4(from);
		bytes[0] = fromby[0] & 0xff; 
		bytes[1] = fromby[1] & 0xff;
		bytes[2] = fromby[2] & 0xff; 
		bytes[3] = fromby[3] & 0xff;
		int[] toby = KissFrame.littleEndian4(to);
		bytes[4] = toby[0] & 0xff; 
		bytes[5] = toby[1] & 0xff;
		bytes[6] = toby[2] & 0xff; 
		bytes[7] = toby[3] & 0xff;
	}
	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	public long getFirst() {
		return from;
	}
	public long getLast() {
		return to;
	}
	
	public Date getFirstDate() {
		return new Date(from*1000);
	}
	public Date getLastDate() {
		return new Date(to*1000);
	}
	
	@Override
	public int compareTo(DirHole o) {
		// Order the holes so that most recent dates are first
		if (from > o.from) return -1;
		if (from < o.from) return 1;
		return 0;
	}
	

	
	public String toString () {
		Date fDate = new Date(from*1000);
		Date tDate = new Date(to*1000);
		
		return "Frm: " + PacSatField.getDateString(fDate) + " To: " + PacSatField.getDateString(tDate);
	}

	public static final void main(String[] args) {
//		int[] data = {0x3C,0xB1,0x4F,0x5B,0xFF,0xFF,0xFF,0x7F,0x1A,0x7A,0x3C,0x5B,0xBA,0x44,0x3D,
//				0x5B,0x6C,0x5E,0x3D,0x5B,0xFF,0xFF,0xFF,0x7F,0x5A,0x5A,0x2D,0x5B,0x31,0x2C,0x2E,
//				0x5B,0x86,0x47,0x2F,0x5B,0x30,0x58,0x2F,0x5B,0x01,0x00,0x00,0x00,0x3C,0xB1,0x4F,0x5B};
		
		//int[] data = {0x32,0xD2,0x50,0x5B,0x2A,0xE7,0x50,0x5B,0xD0,0xF6,0x50,0x5B,0xE4,0xF6,0x50,
		//	0x5B,0x7C,0x00,0x51,0x5B,0xFF,0xFF,0xFF,0x7F};
		
		//int[] data = {77, 28, 82, 91, 236, 60, 82, 91};
		int[] data = {0x4D,0x1C,0x52,0x5B,0xEC,0x3C,0x52,0x5B};
		
		for (int h=0; h< data.length; h=h+8) {
			int[] by1 = {data[h+0],data[h+1],data[h+2],data[h+3],
					data[h+4],data[h+5],data[h+6],data[h+7]};
			DirHole dh1 = new DirHole(by1);
			System.out.println(dh1);
		}
		
//		int[] by2 = {0x86,0x47,0x2F,0x5B,0x30,0x58,0x2F,0x5B};
//		DirHole dh1 = new DirHole(by2);
//		System.out.println(dh1);
//		
//		int[] by3 = {0x45,0xB8,0x50,0x5B,0xFF,0xFF,0xFF,0x7F};
//		DirHole dh = new DirHole(by3);
//		System.out.println(dh);
	}

}
