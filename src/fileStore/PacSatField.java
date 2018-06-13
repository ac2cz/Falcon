package fileStore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import pacSat.frames.KissFrame;
import pacSat.frames.UiFrame;

public class PacSatField {
	public int id;
	public int length;
	int[] data;
	
	/**
	 * Given a byte array and the current pointer into it, parse the next field
	 * @param p
	 * @param bytes
	 * @throws MalformedPfhException 
	 */
	public PacSatField(int p, int[] bytes) throws MalformedPfhException {
		if (p+2 > bytes.length) throw new MalformedPfhException("Id Field past end of PFH");
		id = KissFrame.getIntFromBytes(bytes[p],bytes[p+1]);
		if (id == 0x00) return; // this is the termination item
		if (p+3 > bytes.length) throw new MalformedPfhException("Length Field past end of PFH");
		length = bytes[p+2];
		if (length + p + 3 > bytes.length) throw new MalformedPfhException("Corrupt Length in PFH");
		data = new int[length];
		for (int i = 0; i < length; i++) {
		   data[i] = (bytes[i+p+3] & 0xff);
		}
	}
	
	boolean isNull() {
		if (id == 0 && length == 0) return true;
		return false;
	}
	
	public String getStringValue() {
		String fileName = "";
		for (int i=0; i<length; i++) {
			if (UiFrame.isPrintableChar((char)data[i]))
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
	
	public static final DateFormat dateFormat = new SimpleDateFormat(
			"HH:mm dd MMM yy", Locale.ENGLISH);
	
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
			if (UiFrame.isPrintableChar((char)data[i]))
				s = s + (char)data[i];
		}
		return s;
	}
}
