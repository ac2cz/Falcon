package fileStore;

import java.util.Date;

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
	 */
	public PacSatField(int p, int[] bytes) {
		id = KissFrame.getIntFromBytes(bytes[p],bytes[p+1]);
		length = bytes[p+2];
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
	
	public String getLongString() {
		return Long.toHexString(getLongValue() & 0xffffffff);
	}
	public String toString() {
		String s = "" + id + " " + length;
		for (int i = 0; i < length; i++) {
			s = s + " " + Integer.toHexString(data[i] & 0xff);
		}
		return s;
	}
}
