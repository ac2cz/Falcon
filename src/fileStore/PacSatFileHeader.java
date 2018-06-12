package fileStore;

import java.util.Arrays;
import java.util.Date;

import pacSat.Crc16;
import pacSat.frames.KissFrame;
import pacSat.frames.UiFrame;

public class PacSatFileHeader {
	public static final int TAG1 = 0xaa;
	public static final int TAG2 = 0x55;
	public static final int MAX_TABLE_FIELDS = 9;
	
	int[] rawBytes;
	PacSatField fileId;
	PacSatField fileName ;
	PacSatField fileExt;
	PacSatField fileLength;
	PacSatField creationTime;
	PacSatField lastModifiedTime;
	PacSatField uploadTime;
	PacSatField singleEventUpsetInFile;
	PacSatField type;
	PacSatField bodyChecksum;
	PacSatField headerChecksum;
	PacSatField bodyOffset;
	
	String toCallsign;
	String fromCallsign;
	
	int state;
	String[] states = {"GET","IGN"};
	
	int p; // points to current bytes we are processing
	
	public PacSatFileHeader(String to, String from, int[] bytes) throws MalformedPfhException {
		toCallsign = to;
		fromCallsign = from;
		rawBytes = bytes;
		
		int check1 = bytes[0];
		int check2 = bytes[1];
		if (check1 != TAG1) throw new MalformedPfhException("Missing "+TAG1);
		if (check2 != TAG2) throw new MalformedPfhException("Missing "+TAG2);
		
		p = 2; // our byte position in the header
		// Header fields follow in the format ID, LEN, DATA
		
		// Mandatory Header Follows
		
		fileId = new PacSatField(p,bytes);
		p = p + fileId.length + 3;
		fileName = new PacSatField(p,bytes);
		p = p + fileName.length + 3;
		fileExt = new PacSatField(p,bytes);
		p = p + fileExt.length + 3;
		fileLength = new PacSatField(p,bytes);
		p = p + fileLength.length + 3;
		creationTime = new PacSatField(p,bytes);
		p = p + creationTime.length + 3;
		lastModifiedTime = new PacSatField(p,bytes);
		p = p + lastModifiedTime.length + 3;
		uploadTime = new PacSatField(p,bytes);
		p = p + uploadTime.length + 3;
		singleEventUpsetInFile = new PacSatField(p,bytes);
		p = p + singleEventUpsetInFile.length + 3;
		type = new PacSatField(p,bytes);
		p = p + type.length + 3;
		bodyChecksum = new PacSatField(p,bytes);
		p = p + bodyChecksum.length + 3;
		headerChecksum = new PacSatField(p,bytes);
		p = p + headerChecksum.length + 3;
		bodyOffset = new PacSatField(p,bytes);
		p = p + bodyOffset.length + 3;
		PacSatField firstExtended = new PacSatField(p,bytes);
		if (!firstExtended.isNull()) {// then we have extended header
			System.err.println("Found ext header");
		}
	} 
	
	private long getNextField(int[] bytes) {
		int id = KissFrame.getIntFromBytes(bytes[p],bytes[p+1]);
		int dataLength = bytes[p+2];
		long value = 0;
		for (int i = 0; i < dataLength; i++) {
		   value += (bytes[i+p+3] & 0xffL) << (8 * i);
		}
		p=p+dataLength;
		return value;
	}

	
	public String[] getTableFields() {
		String[] fields = new String[MAX_TABLE_FIELDS];
		fields[0] = fileId.getLongString();
		fields[1] = states[state];
		fields[2] = toCallsign;
		fields[3] = fromCallsign;
		fields[4] = ""+uploadTime.getDateValue();
		fields[5] = ""+fileLength.getLongString();
		fields[6] = ""; // %
		fields[7] = fileName.getStringValue() +'.' + fileExt.getStringValue();
		fields[8] = "";
		
		return fields;
	}
	
	public String toString() {
		String s = "";
		s = s + " FILE: " + fileId.getLongString();
		s = s + " NAME: " + fileName.getStringValue() +'.' + fileExt.getStringValue();
		s = s + " FLEN: " + fileLength.getLongString();
		s = s + " CR: " + creationTime.getDateValue();
		s = s + " MOD: " + lastModifiedTime.getDateValue();
		s = s + " UP: " + uploadTime.getDateValue();
		s = s + " SEU: " + singleEventUpsetInFile.getLongString();
		s = s + " TYPE: " + type.getLongString();
		s = s + " BCRC: " + bodyChecksum.getLongString();
		s = s + " HCRC: " + headerChecksum.getLongString();
		
		return s;
	}
}
