package fileStore;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ax25.Ax25Frame;
import ax25.KissFrame;

public class PacSatField implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final DateFormat dateFormat = new SimpleDateFormat(
			"dd MMM yy HH:mm:ss", Locale.ENGLISH);
	public static final DateFormat dateFormatSecs = new SimpleDateFormat(
			"dd MMM yy HH:mm:ss", Locale.ENGLISH);
	public int id;
	public int length;
	int[] data;
	
	static {
		PacSatField.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		PacSatField.dateFormatSecs.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	/**
	 * Given a string and a type construct a pacsat header field
	 * @param str
	 * @param id 
	 */
	public PacSatField(String str, int id) {
		this.id = id;
		this.length = str.length();
		data = new int[length];
		for (int i=0; i<length; i++) {
			data[i] = (int)str.charAt(i);
		}
	}
	
	public PacSatField(int[] bytes, int id) {
		this.id = id;
		this.length = bytes.length;
		data = new int[length];
		for (int i=0; i<length; i++) {
			data[i] = bytes[i];
		}
	}
	
	public PacSatField(Date date, int id) {
		this.id = id;
		this.length = 4;
		data = new int[length];
		long time = date.getTime()/1000;
		int[] bytes = KissFrame.littleEndian4(time);
		for (int i=0; i<length; i++) {
			data[i] = bytes[i];
		}
	}
	
	public PacSatField(byte value, int id) {
		this.id = id;
		this.length = 1;
		data = new int[length];
		data[0] = value & 0xff;
	}
	
	public PacSatField(short value, int id) {
		this.id = id;
		this.length = 2;
		data = new int[length];
		int[] by2 = KissFrame.littleEndian2(value);
		data[0] = by2[0];
		data[1] = by2[1];
	}
	
	public PacSatField(int value, int id) {
		this.id = id;
		this.length = 2;
		data = new int[length];
		int[] by2 = KissFrame.littleEndian2(value);
		data[0] = by2[0];
		data[1] = by2[1];
	}

	public PacSatField(long value, int id) {
		this.id = id;
		this.length = 4;
		data = new int[length];
		int[] by2 = KissFrame.littleEndian4(value);
		data[0] = by2[0];
		data[1] = by2[1];
		data[2] = by2[2];
		data[3] = by2[3];
	}

	
	/**
	 * Given a byte array and the current pointer into it, parse the next field
	 * @param p
	 * @param bytes
	 * @throws MalformedPfhException 
	 */
	public PacSatField(int p, int[] bytes) throws MalformedPfhException {
		if (p+2 > bytes.length) throw new MalformedPfhException("Id Field past end of PFH");
		int[] by = {bytes[p],bytes[p+1]};
		id = KissFrame.getIntFromBytes(by);
		if (id == 0x00) return; // this is the termination item
		if (p+3 > bytes.length) {
			//throw new MalformedPfhException("Length Field past end of PFH");
			length=0;
			id=0;
			return;
		}
		length = bytes[p+2];
		if (length + p + 3 > bytes.length) {
			//throw new MalformedPfhException("Corrupt Length in PFH");
			length=0;
			id=0;
			return;
		}
		data = new int[length];
		for (int i = 0; i < length; i++) {
		   data[i] = (bytes[i+p+3] & 0xff);
		}
	}
	
	public void copyFrom(PacSatField pf) {
		id = pf.id;
		length = pf.length;
		data = new int[length];
		for (int i = 0; i < length; i++) {
		   data[i] = (pf.data[i]);
		}
	}
	
	boolean isNull() {
		if (id == 0 && length == 0) return true;
		return false;
	}
	
	public static String getDateString(Date date) {
		String s = "";
		try {
			s = PacSatField.dateFormat.format(date);
		} catch (Exception e) {};
		return s;
	}

	public static String getDateStringSecs(Date date) {
		String s = "";
		try {
			s = PacSatField.dateFormatSecs.format(date);
		} catch (Exception e) {};
		return s;
	}

	public String getStringValue() {
		String fileName = "";
		for (int i=0; i<length; i++) {
			if (Ax25Frame.isPrintableChar((char)data[i]))
				fileName = fileName + (char)data[i];
		}
		return fileName;
	}
	
	public long getLongValue() {
		long value = 0;
		for (int i = 0; i < length; i++) {
		   value += (data[i] & 0xffL) << (8 * i);
		}
		return value;
	}
	
	public Date getDateValue() {
		long time = getLongValue();
		Date crDate = new Date(time*1000);
		return crDate;
	}
	
	public String getDateString() {
		Date d = getDateValue();
		if (d == null) return "";
		String s = "";
		try {
			s = PacSatField.dateFormat.format(d);
		} catch (Exception e) {};
		return s;
	}
	
	public String getLongString() {
		return "" + (getLongValue() & 0xffffffff);
	}
	public String getLongHexString() {
		return Long.toHexString(getLongValue() & 0xffffffff);
	}
	
	public int[] getBytes() {
		int[] bytes = new int[3+data.length];
		int[] by = KissFrame.littleEndian2(id);
		bytes[0] = by[0];
		bytes[1] = by[1];
		bytes[2] = length;
		for (int i=0; i < data.length; i++)
			bytes[3+i] = data[i];
		return bytes;
	}
	
	public String toString() {
		String s = "" + Integer.toHexString(id & 0xff) + " " + length;
		for (int i = 0; i < length; i++) {
			s = s + " " + Integer.toHexString(data[i] & 0xff);
		}
		s = s + " | ";
		for (int i = 0; i < length; i++) {
			if (Ax25Frame.isPrintableChar((char)data[i]))
				s = s + (char)data[i];
		}
		return s;
	}
	
	public static void main(String[] args) {
		String title = "12346";
		if (title.length() > 5)
		title = title.substring(0,5);
		PacSatField pf = new PacSatField(title, PacSatFileHeader.FILE_NAME);
		//PacSatField pf = new PacSatField("G0KLA",1);
//		PacSatField pf = new PacSatField((byte)0xff,7);
		//PacSatField pf = new PacSatField(0xDE,7);
		//PacSatField pf = new PacSatField((long)0xDEADBEEF,7);
		System.out.println(pf);
		int[] by = pf.getBytes();
		for (int i : by)
			System.out.print(Integer.toHexString(i) + " ");
		System.out.println();
	}
}
