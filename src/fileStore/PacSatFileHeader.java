package fileStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import gui.FileHeaderTableModel;

public class PacSatFileHeader implements Comparable<PacSatFileHeader>, Serializable{

	private static final long serialVersionUID = 1L;
	public static final int TAG1 = 0xaa;
	public static final int TAG2 = 0x55;
	
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
	
	public static final String[] compressions = {"None","PKARC","PKZIP"};
	
	int state;
	public static final int NONE = 0;
	public static final int PARTIAL = 1;
	public static final int NEWMSG = 2;
	public static final int MSG = 3;
	public static final String[] states = {"","PART","NEW", "MSG"};
	
	public static Map<Integer, String> types = new HashMap<Integer, String>();
	
	static {
		// Init the type map
		types.put(0, "ASCII");
		types.put(2, "BBS");
		types.put(3, "WOD");
		types.put(6, "EXE");
		types.put(7, "COM");
		types.put(8, "NASA KEPS");
		types.put(9, "AMSAT KEPS");
		types.put(12, "BINARY");
		types.put(13, "MULTIPLE ASCII");
		types.put(14, "GIF");
		types.put(15, "PCX");
		types.put(16, "JPG");
		types.put(17, "CONFIRM");
		types.put(18, "SAT GATE");
		types.put(19, "INET");
		types.put(200, "Config Uploaded");
		types.put(201, "Activity log");
		types.put(202, "Broadcast Log");
		types.put(203, "WOD Log");
		types.put(204, "ADCS Log");
		types.put(205, "TDE data");
		types.put(206, "SCTE data");
		types.put(207, "Transputer Log");
		types.put(208, "SEU Log");
		types.put(209, "CPE");
		types.put(210, "Battery Charge Log");
		types.put(211, "Image");
		types.put(212, "SPL Log");
		types.put(213, "PCT Log");
		types.put(214, "PCT Command Log");
		types.put(215, "QL Image");
		types.put(221, "CCD Image");
		types.put(222, "CPE Results");
		types.put(255, "Undefined");
	}

	long downloadedBytes = 0;
	
	Date dateDownloaded = new Date();
	public int userDownLoadPriority = 0;
	
	public PacSatFileHeader(int[] bytes) throws MalformedPfhException {
		rawBytes = bytes;
		fields = new ArrayList<PacSatField>();
		
		int check1 = bytes[0];
		int check2 = bytes[1];
		if (check1 != TAG1) throw new MalformedPfhException("Missing "+Integer.toHexString(TAG1));
		if (check2 != TAG2) throw new MalformedPfhException("Missing "+Integer.toHexString(TAG2));
		
		boolean readingHeader = true;
		int p; // points to current bytes we are processing
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
		return getFieldById(FILE_ID).getLongValue();
	}
	
	public String getFileName() {
		return getFieldById(FILE_ID).getLongHexString();
	}
	
	public Date getUploadTime() {
		PacSatField dateField = null;
		dateField = getFieldById(PacSatFileHeader.UPLOAD_TIME);
		Date lastDate = null;
		if (dateField != null)
			lastDate = dateField.getDateValue();
		return lastDate;
	}
	
	public String getTypeString() {
		PacSatField typeField = null;
		typeField = getFieldById(PacSatFileHeader.FILE_TYPE);
		String s = "";
		if (typeField != null) {
			int t = (int) typeField.getLongValue();
			s = types.get(t);
			if (s == null) return "Unknown";
		}
		return s;
	}
	
	public PacSatField getFieldById(int id) {
		for (PacSatField field : fields) {
			if (field.id == id) return field;
		}
		return null;
	}

	public void setState(int state) {
		this.state = state;
	}
	
	public String[] getTableFields() {
		String[] fields = new String[FileHeaderTableModel.MAX_TABLE_FIELDS];

		fields[0] = getFieldById(FILE_ID).getLongHexString();
		if (userDownLoadPriority > 0) 
			fields[1] = Integer.toString(userDownLoadPriority);
		else
			fields[1] = "";
		fields[2] = states[state];
		if (getFieldById(DESTINATION) != null)
			fields[3] = ""+getFieldById(DESTINATION).getStringValue();
		else
			fields[3] = "";// toCallsign;
		if (getFieldById(SOURCE) != null)
			fields[4] = ""+getFieldById(SOURCE).getStringValue();
		else
			fields[4] = "";//fromCallsign;
		if (getFieldById(UPLOAD_TIME) != null)
			fields[5] = ""+getFieldById(UPLOAD_TIME).getDateString();
		else
			fields[5] = "";
		fields[6] = ""+getFieldById(FILE_SIZE).getLongString();
		int percent = (int) (100 * downloadedBytes/(double)getFieldById(FILE_SIZE).getLongValue());
		fields[7] = "" + percent; 
		if (getFieldById(TITLE) != null)
			fields[8] = getFieldById(TITLE).getStringValue();
		else if (getFieldById(FILE_NAME) != null && getFieldById(FILE_EXT) != null)
			fields[8] = getFieldById(FILE_NAME).getStringValue() +'.' + getFieldById(FILE_EXT).getStringValue();
		
		fields[9] = this.getTypeString();
		
		if (getFieldById(KEYWORDS) != null)
			fields[10] = getFieldById(KEYWORDS).getStringValue();
		else
			fields[10] = "";
		
		return fields;
	}
	
	public String toString() {
		String s = "";
		if (getFieldById(FILE_ID) != null)
		s = s + " FILE: " + getFieldById(FILE_ID).getLongHexString();
		if (getFieldById(FILE_NAME) != null)
		s = s + " NAME: " + getFieldById(FILE_NAME).getStringValue() +'.' + getFieldById(FILE_EXT).getStringValue();
		if (getFieldById(FILE_SIZE) != null)
		s = s + " FLEN: " + getFieldById(FILE_SIZE).getLongString();
		if (getFieldById(CREATE_TIME) != null)
		s = s + " CR: " + getFieldById(CREATE_TIME).getDateValue();
		if (getFieldById(LAST_MOD_TIME) != null)		
		s = s + " MOD: " + getFieldById(LAST_MOD_TIME).getDateValue();
		if (getFieldById(UPLOAD_TIME) != null)
		s = s + " UP: " + getFieldById(UPLOAD_TIME).getDateValue();
		if (getFieldById(SEU_FLAG) != null)
		s = s + " SEU: " + getFieldById(SEU_FLAG).getLongString();
		if (getFieldById(FILE_TYPE) != null)
		s = s + " TYPE: " + getFieldById(FILE_TYPE).getLongString();
		if (getFieldById(BODY_CHECKSUM) != null)
		s = s + " BCRC: " + getFieldById(BODY_CHECKSUM).getLongString();
		if (getFieldById(HEADER_CHECKSUM) != null)
			s = s + " HCRC: " + getFieldById(HEADER_CHECKSUM).getLongString();
		
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
