package fileStore;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import pacSat.frames.KissFrame;
import pacSat.frames.Ax25Frame;

public class PacSatField implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final DateFormat dateFormat = new SimpleDateFormat(
			"HH:mm:ss dd MMM yy", Locale.ENGLISH);
	public int id;
	public int length;
	int[] data;

	int[] bytes;  //////////////////These needs to be the raw bytes 
	
	/**
	 * Given a string and a type construct a pacsat header field
	 * @param str
	 * @param id 
	 */
	public PacSatField(String str, int id) {
		this.id = id;
		this.length = str.length();
		data = new int[length];
		int[] by = KissFrame.littleEndian2(id);
		data[0] = by[0];
		data[1] = by[1];
		data[2] = length;
		for (int i=0; i<length; i++) {
			data[i] = (int)str.charAt(i);
		}
	}
	
	public PacSatField(int[] bytes, int id) {
		this.id = id;
		this.length = bytes.length;
		data = new int[length+3];
		int[] by = KissFrame.littleEndian2(id);
		data[0] = by[0];
		data[1] = by[1];
		data[2] = length;
		for (int i=0; i<length; i++) {
			data[3+i] = bytes[i];
		}
	}
	
	public PacSatField(Date date, int id) {
		this.id = id;
		this.length = 4;
		data = new int[length+3];
		int[] by = KissFrame.littleEndian2(id);
		data[0] = by[0];
		data[1] = by[1];
		data[2] = length;
		long time = date.getTime()/1000;
		int[] bytes = KissFrame.littleEndian4(time);
		for (int i=0; i<length; i++) {
			data[3+i] = bytes[i];
		}
	}
	
	public PacSatField(int value, int id) {
		this.id = id;
		this.length = 2;
		data = new int[4];
		int[] by = KissFrame.littleEndian2(id);
		data[0] = by[0];
		data[1] = by[1];
		data[2] = length;
		int[] by2 = KissFrame.littleEndian2(value);
		data[3] = by2[0];
		data[4] = by2[1];
	}

	public PacSatField(long value, int id) {
		this.id = id;
		this.length = 4;
		data = new int[4];
		int[] by = KissFrame.littleEndian2(id);
		data[0] = by[0];
		data[1] = by[1];
		data[2] = length;
		int[] by2 = KissFrame.littleEndian4(value);
		data[3] = by2[0];
		data[4] = by2[1];
		data[5] = by2[2];
		data[6] = by2[3];
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
	
	boolean isNull() {
		if (id == 0 && length == 0) return true;
		return false;
	}
	
	public static String getDateString(Date date) {
		PacSatField.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return PacSatField.dateFormat.format(date);
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
		PacSatField.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return PacSatField.dateFormat.format(getDateValue());
	}
	
	public String getLongString() {
		return "" + (getLongValue() & 0xffffffff);
	}
	public String getLongHexString() {
		return Long.toHexString(getLongValue() & 0xffffffff);
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
		PacSatField pf = new PacSatField("G0KLA",1);
		System.out.println(pf);
	}
}
