	package fileStore.telem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.ConversionTable;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

import common.Config;
import fileStore.PacSatField;

public class RecordAL1 extends DataRecord {
	
	public static final int EVENT_FIELD = 0; 
	public static final int LENGTH_FIELD = 1; 
	public static final int TSTAMP_FIELD = 2; 
	public static final int VAR1_FIELD = 5; 
	public static final int VAR2_FIELD = 6; 
	public static final int VAR3_FIELD = 7; 
	public static final int VAR4_FIELD = 8; 
	public static final int VAR5_FIELD = 9; 
	public static final int VAR6_FIELD = 10; 


	

	/* Events logged in the ALOG, in numerical order of their event code,
	** beginning with event 0 (nothing).
	*/
	String event_text[] = {
		"         ",  //0
		"STARTUP  ",
		"SHUTDOWN ",
		"LOGIN    ",
		"LOGOUT   ",
		"BLOWOFF  ", //5
		"REFUSED  ",
		"BCST ON  ",
		"BCST OFF ",
		"FREE DISK",
		"DELETE   ",
		"DOWNLOAD ",
		"UPLOAD   ",
		"SHUT     ",
		"OPEN     ",
		"DIR      ",
		"SELECT   ",
		"ADEL OK  ",
		"ADEL FAIL",
		"DL DONE  ",
		"UL DONE  ",
		"DIR DONE ",
		"SEL DONE ",
		};
	
	public RecordAL1(int id, int resets, long uptime, int type, int[] data) throws LayoutLoadException, IOException {
		super(new ByteArrayLayout("AL", Config.currentDir + "\\spacecraft\\ALOG_1F_format.csv"), id, resets, uptime, type, data);
	}
	
	String getTimeStamp() {
		String s = "";
		long t = fieldValue[TSTAMP_FIELD];
		Date d = new Date(t*1000);
		s = s + PacSatField.getDateStringSecs(d);
		return s;
	}
	
	String getEventString() {
		String s = "UNKNOWN  ";
		int e = fieldValue[EVENT_FIELD];
		s = event_text[e];
		return s;
	}
	
	public String toString() {
		String s = new String();
		s = s + getTimeStamp() + "\t";
		s = s + getEventString() + " ";
		return s;
	}
}
