package fileStore;

import java.io.Serializable;
import java.util.ArrayList;

public class PacSatFileHeader implements Comparable<PacSatFileHeader>, Serializable{

	private static final long serialVersionUID = 2791224214441336999L;
	public static final int TAG1 = 0xaa;
	public static final int TAG2 = 0x55;
	public static final int MAX_TABLE_FIELDS = 9;
	
	int[] rawBytes;
	ArrayList<PacSatField> fields;
	public static final int FILE_ID = 0x01;
	public static final int FILE_NAME = 0x02;
	public static final int FILE_EXT = 0x03;
	public static final int FILE_SIZE = 0x04;
	public static final int CREATE_TIME = 0x05;
	public static final int LAST_MOD_TIME = 0x06;
	public static final int SEU_FLAG = 0x07;
	public static final int FILE_TYPE = 0x08;
	public static final int BODY_CHECKSUM = 0x09;
	public static final int HEADER_CHECKSUM = 0x0a;
	public static final int BODY_OFFSET = 0x0b;
	
	public static final int SOURCE = 0x10;
	public static final int AX25_UPLOADER = 0x11;
	public static final int UPLOAD_TIME = 0x12;
	public static final int DOWNLOAD_COUNT = 0x13;	
	public static final int DESTINATION = 0x14;
	public static final int AX25_DOWNLOADER = 0x15;
	public static final int DOWNLOAD_TIME = 0x16;
	public static final int EXPIRE_TIME = 0x17;
	public static final int PRIORITY = 0x18;
	public static final int COMPRESSION_TYPE = 0x19;
	public static final int BBS_MSG_TYPE = 0x20;
	public static final int BULLETIN_ID_NUMBER = 0x21;
	public static final int TITLE = 0x22;
	public static final int KEYWORDS = 0x23;
	public static final int FILE_DESCRIPTION = 0x24;
	public static final int COMPRESSION_DESCRIPTION = 0x25;
	public static final int USER_FILE_NAME = 0x26;
	
	// Header 2 test file has many more fields..
	
	// Compression types
	public static final int BODY_NOT_COMPRESSED = 0x00;
	public static final int BODY_COMPRESSED_PKARC = 0x01;
	public static final int BODY_COMPRESSED_PKZIP = 0x02;
	//public static final int BODY_COMPRESSED_GZIP = 0x03;
	
	int state;
	public static final int NONE = 0;
	public static final int P1 = 1;
	public static final int P2 = 2;
	public static final int P3 = 3;
	public static final int P4 = 4;
	public static final int P5 = 5;
	public static final int P6 = 6;
	public static final int P7 = 7;
	public static final int P8 = 8;
	public static final int P9 = 9;
	public static final int D = 10;
	public static final int G = 11;
	public static final String[] states = {"","P1","P2","P3","P4","P5","P6","P7","P8","P9","D","G"};
	
	long downloadedBytes = 0;
	
	int p; // points to current bytes we are processing
	
	public PacSatFileHeader(int[] bytes) throws MalformedPfhException {
		rawBytes = bytes;
		fields = new ArrayList<PacSatField>();
		
		int check1 = bytes[0];
		int check2 = bytes[1];
		if (check1 != TAG1) throw new MalformedPfhException("Missing "+TAG1);
		if (check2 != TAG2) throw new MalformedPfhException("Missing "+TAG2);
		
		boolean readingHeader = true;
		p = 2; // our byte position in the header
		// Header fields follow in the format ID, LEN, DATA
		while (readingHeader) {
			PacSatField field = new PacSatField(p,bytes);
			p = p + field.length + 3;
			if (field.isNull())
				readingHeader = false;
			else
				fields.add(field);
		}
		
	} 
	
	public long getFileId() {
		return getFieledById(FILE_ID).getLongValue();
	}
	
	public String getFileName() {
		return getFieledById(FILE_ID).getLongHexString();
	}
	
	private PacSatField getFieledById(int id) {
		for (PacSatField field : fields) {
			if (field.id == id) return field;
		}
		return null;
	}

	public void setState(int state) {
		this.state = state;
	}
	
	public String[] getTableFields() {
		String[] fields = new String[MAX_TABLE_FIELDS];

		fields[0] = getFieledById(FILE_ID).getLongHexString();
		fields[1] = states[state];
		if (getFieledById(DESTINATION) != null)
			fields[2] = ""+getFieledById(DESTINATION).getStringValue();
		else
			fields[2] = "";// toCallsign;
		if (getFieledById(SOURCE) != null)
			fields[3] = ""+getFieledById(SOURCE).getStringValue();
		else
			fields[3] = "";//fromCallsign;
		if (getFieledById(UPLOAD_TIME) != null)
			fields[4] = ""+getFieledById(UPLOAD_TIME).getDateString();
		else
			fields[4] = "";
		fields[5] = ""+getFieledById(FILE_SIZE).getLongString();
		int percent = (int) (100 * downloadedBytes/(double)getFieledById(FILE_SIZE).getLongValue());
		fields[6] = "" + percent; 
		if (getFieledById(TITLE) != null)
			fields[7] = getFieledById(TITLE).getStringValue();
		else
			fields[7] = getFieledById(FILE_NAME).getStringValue() +'.' + getFieledById(FILE_EXT).getStringValue();
		if (getFieledById(KEYWORDS) != null)
			fields[8] = getFieledById(KEYWORDS).getStringValue();
		else
			fields[8] = "";
		
		//for (String s : fields)
		//	if (s == null)
		//		s = ""; // re-init fields to avoid any NULL pointer exceptions
		
		return fields;
	}
	
	public String toString() {
		String s = "";
		s = s + " FILE: " + getFieledById(FILE_ID).getLongString();
		s = s + " NAME: " + getFieledById(FILE_NAME).getStringValue() +'.' + getFieledById(FILE_EXT).getStringValue();
		s = s + " FLEN: " + getFieledById(FILE_SIZE).getLongString();
		s = s + " CR: " + getFieledById(CREATE_TIME).getDateValue();
		s = s + " MOD: " + getFieledById(LAST_MOD_TIME).getDateValue();
		if (getFieledById(UPLOAD_TIME) != null)
		s = s + " UP: " + getFieledById(UPLOAD_TIME).getDateValue();
		s = s + " SEU: " + getFieledById(SEU_FLAG).getLongString();
		s = s + " TYPE: " + getFieledById(FILE_TYPE).getLongString();
		s = s + " BCRC: " + getFieledById(BODY_CHECKSUM).getLongString();
		s = s + " HCRC: " + getFieledById(HEADER_CHECKSUM).getLongString();
		
		return s;
	}

	@Override
	public int compareTo(PacSatFileHeader p) {
		if (getFileId() == p.getFileId())
			return 0;
		if (getFileId() < p.getFileId())
			return -1;
		if (getFileId() > p.getFileId())
			return 1;
		return 0;
	}
}
