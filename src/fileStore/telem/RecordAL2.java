	package fileStore.telem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.ConversionTable;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class RecordAL2 extends DataRecord {
	
	public static final int EVENT_FIELD = 0; 
	public static final int LENGTH_FIELD = 1; 
	public static final int TSTAMP_FIELD = 2; 
	public static final int CALL_FIELD1 = 5; 
	public static final int RXCHAN_FIELD = 4; 
	public static final int SSID_FIELD = 11; 
	public static final int VAR1_FIELD = 12; 
	public static final int VAR2_FIELD = 13; 
	public static final int VAR3_FIELD = 14; 
	public static final int VAR4_FIELD = 15; 
	public static final int VAR5_FIELD = 16; 
	public static final int VAR6_FIELD = 17; 

	public static final String VAR3 = "var3"; 
	
	public RecordAL2(int id, int resets, long uptime, int type, int[] data) throws LayoutLoadException, IOException {
		super(new ByteArrayLayout("AL", "spacecraft\\ALOG_2F_format.csv"), id, resets, uptime, type, data);
	}
	
	public String getSsid() {
		String s = "";
		s = s + fieldValue[SSID_FIELD];
		return s;
	}

	public String getRxChan() {
		String s = "";
		s = s + fieldValue[RXCHAN_FIELD];
		return s;
	}

	public String getCallString() {
		String s = "";
		for (int j=0; j<6; j++)
			if (fieldValue[CALL_FIELD1+j] != 0x00)
			s = s + String.valueOf((char)fieldValue[CALL_FIELD1+j]);
		return s;
	}

	public String getField(int field) {
		String s = null;
		s = s + fieldValue[field];
		return s;
	}

	
	public String toString() {
		String s = new String();
		Date date2 = new Date(fieldValue[2]*1000);
		s = s + "Event:" + this.fieldValue[0] + " LEN:" + this.fieldValue[1]+ " ";
		s = s + date2;
		return s;
	}
}
