package fileStore;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import gui.FileHeaderTableModel;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.FrameException;

public class PacSatFileHeader implements Comparable<PacSatFileHeader>, Serializable{

	private static final long serialVersionUID = 1L;
	public static final int TAG1 = 0xaa;
	public static final int TAG2 = 0x55;
	public static final int PRI_N = 9; // pririty N
	
	int[] rawBytes;
	ArrayList<PacSatField> fields;
	
	// Mandatory Header
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
	
	// Extended Header
	public static final int SOURCE = 0x10;
	public static final int AX25_UPLOADER = 0x11;
	public static final int UPLOAD_TIME = 0x12;
	public static final int DOWNLOAD_COUNT = 0x13;	
	public static final int DESTINATION = 0x14;
	public static final int AX25_DOWNLOADER = 0x15;
	public static final int DOWNLOAD_TIME = 0x16;
	public static final int EXPIRE_TIME = 0x17;
	public static final int PRIORITY = 0x18;
	
	// Optional Header
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
	public static final int MISSING = 4;
	public static final int QUE = 5;
	public static final int SENT = 6;
	public static final int REJ = 7;
	
	public static final String[] states = {"","PART","NEW", "MSG", "GONE", "QUE", "SENT", "FAIL"};
	public static final String[] userTypeStrings = {"Select Type", "ASCII", "JPG" };
	public static final int[] userTypes = {-999, 0, 16};
	private static final int[] types = {-999, 0,2,3,6,7,8,9,12,13,14,15,16,17,18,19,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,221,222,255};
	public static final String[] typeStrings = {"Select Type", "ASCII", "BBS","WOD", "EXE", "COM", "NASA KEPS", "AMSAT KEPS", "BINARY", "MULTIPLE ASCII", "GIF", "PCX",
			"JPG", "CONFIRM", "SAT GATE", "INET", "Config Uploaded", "Activity Log", "Broadcast Log", "WOD Log", "ADCS Log", "TDE Log", "SCTE Log",
			"Transputer Log", "SEU Log", "CPE", "Battery Charge Log", "Image", "SPL Log", "PCT Log", "PCT Command Log", "QL Image", "CCD Image",
			"CPE Result", "Undefined"};
		
	long timeOld, timeNew;
	
	long downloadedBytes = 0;
	
	Date dateDownloaded = new Date();
	
	// META DATA that is not in the actual PFH
	public int userDownLoadPriority = 0;
	
	static final int MAX_TITLE_LENGTH = 80;
	static final int MAX_KEYWORDS_LENGTH = 50;
	static final int MAX_FILENAME_LENGTH = 80;
	
	static final int[] eight0 = {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
	static final int[] four0 = {0x00,0x00,0x00,0x00};
	static final int[] two0 = {0x00,0x00};
	
	public PacSatFileHeader(String from, String to, long fileBodyLength, short bodyChecksum, int type, int compressionType, String title, String keywords, String userFileName) {
		fields = new ArrayList<PacSatField>();
		
		createMandatoryHeader(bodyChecksum, type);
		createExtendedHeader(from, to, fileBodyLength, type);
		createOptionalHeader(fileBodyLength, compressionType, title, keywords, userFileName);
		
		// Now we calculate the lengths, update the checksums, set the values and generate the bytes
		int headerLength = 0;
		for (PacSatField f : fields)
			headerLength += f.getBytes().length;
		rawBytes = new int[5+headerLength];
		PacSatField bodyOffset = getFieldById(BODY_OFFSET);
		PacSatField newBodyOffset = new PacSatField(rawBytes.length, BODY_OFFSET);
		bodyOffset.copyFrom(newBodyOffset);
		
		PacSatField fileSize = getFieldById(FILE_SIZE);
		PacSatField newFileSize = new PacSatField(rawBytes.length+fileBodyLength, FILE_SIZE);
		fileSize.copyFrom(newFileSize);
		
		generateBytes();
	}
		
	private void generateBytes() {
		rawBytes[0] = TAG1;
		rawBytes[1] = TAG2;
		
		// First make sure the checksum is zero.  Needs to be zero for the calculation to work.
		PacSatField headerChecksum = getFieldById(HEADER_CHECKSUM);
		short zero = 0;
		PacSatField newHeaderChecksum = new PacSatField(zero, HEADER_CHECKSUM);
		headerChecksum.copyFrom(newHeaderChecksum);
		
		// Then grab all the bytes
		int h = 2;
		for (PacSatField f : fields) {
			int[] by = f.getBytes();
			for (int b : by)
				rawBytes[h++] = b;
		}
		
		// Calculate the checksum for the header
		short bodyCS = checksum(rawBytes);

		// Now put the new checksum in the header again
		PacSatField headerChecksum2 = getFieldById(HEADER_CHECKSUM);
		PacSatField newHeaderChecksum2 = new PacSatField(bodyCS, HEADER_CHECKSUM);
		headerChecksum2.copyFrom(newHeaderChecksum2);

		// generate the bytes again now we have all the checksums
		h = 2;
		for (PacSatField f : fields) {
			int[] by = f.getBytes();
			for (int b : by)
				rawBytes[h++] = b;
		}
		// Terminate the header
		rawBytes[h++] = 0x00;
		rawBytes[h++] = 0x00;
		rawBytes[h] = 0x00;
	}
	
	/**
	 * Copy the meta-data from an existing PFH, such as when we are updating a directory entry
	 * @param pfh
	 */
	public void copyMetaData(PacSatFileHeader pfh) {
		state = pfh.state;
		userDownLoadPriority = pfh.userDownLoadPriority;
	}
	
	public static short checksum(int[] bytes) {
		short cs = 0;
	 	for(int b : bytes)
	 		cs += b;
	 	return cs;
	}
	
	public static short checksum(byte[] bytes) {
		short cs = 0;
	 	for(byte b : bytes)
	 		cs += b & 0xff;
	 	return cs;
	}
	
	private void createMandatoryHeader(short bodyCS, int type) {
		// MANDATORY HEADER
		PacSatField fileNum = new PacSatField(four0, FILE_ID);
		fields.add(fileNum);
		
		PacSatField fileName = new PacSatField("        ", FILE_NAME);
		fields.add(fileName);

		PacSatField fileExt = new PacSatField("   ", FILE_EXT);
		fields.add(fileExt);

		PacSatField fileSize = new PacSatField(four0, FILE_SIZE);
		fields.add(fileSize);

		PacSatField createTime = new PacSatField(new Date(), CREATE_TIME);
		fields.add(createTime);

		PacSatField lastModTime = new PacSatField(new Date(), LAST_MOD_TIME);
		fields.add(lastModTime);
		
		PacSatField seu = new PacSatField((byte)0x00, SEU_FLAG);
		fields.add(seu);

		PacSatField fileType = new PacSatField((byte)type, FILE_TYPE);
		fields.add(fileType);

		PacSatField bodyChecksum = new PacSatField(bodyCS, BODY_CHECKSUM);
		fields.add(bodyChecksum);

		PacSatField headerChecksum = new PacSatField(two0, HEADER_CHECKSUM);
		fields.add(headerChecksum);

		PacSatField bodyOffset = new PacSatField(two0, BODY_OFFSET);
		fields.add(bodyOffset);

	}

	private void createExtendedHeader(String from, String to, long fileBodyLength, int type) {
		// EXTENDED HEADER - All user message files have an extended header
		PacSatField source = new PacSatField(from, SOURCE);
		fields.add(source);

		int[] wispax25_uploader = {0x00,0x00,0x00,0x00,0x5A,0x00}; // instead of "from"
		PacSatField ax25uploader = new PacSatField(wispax25_uploader, AX25_UPLOADER);
		fields.add(ax25uploader);
		
		PacSatField uploadTime = new PacSatField(four0, UPLOAD_TIME);
		fields.add(uploadTime);

		PacSatField downloadCount = new PacSatField((byte)0x00, DOWNLOAD_COUNT);
		fields.add(downloadCount);
		
		PacSatField destination = new PacSatField(to, DESTINATION);
		fields.add(destination);

		// instead of "      " use wisp data..
		PacSatField ax25downloader = new PacSatField(wispax25_uploader, AX25_DOWNLOADER);
		fields.add(ax25downloader);

		PacSatField downloadTime = new PacSatField(four0, DOWNLOAD_TIME);
		fields.add(downloadTime);

		PacSatField expireTime = new PacSatField(four0, EXPIRE_TIME);
		fields.add(expireTime);

		PacSatField priority = new PacSatField((byte)0x00, PRIORITY);
		fields.add(priority);

	}

	private void createOptionalHeader(long fileBodyLength, int compressionType, String title, String keywords, String userFileName) {
		PacSatField compression = new PacSatField((byte)compressionType, COMPRESSION_TYPE);
		fields.add(compression);

		// Need to truncate title and keywords to a sensible length
		if (title.length() > MAX_TITLE_LENGTH)
			title = title.substring(0,MAX_TITLE_LENGTH);
		PacSatField titleField = new PacSatField(title, TITLE);
		fields.add(titleField);

		if (keywords != null && !keywords.equalsIgnoreCase("")) {
			if (keywords.length() > MAX_KEYWORDS_LENGTH)
				keywords = keywords.substring(0,MAX_KEYWORDS_LENGTH);
			PacSatField keywordsField = new PacSatField(keywords, KEYWORDS);
			fields.add(keywordsField);
		}
		
		// File Description would be included if FileType set to FF
		// Compression Description included if compression type set to FF
		
		if (userFileName != null && !userFileName.equalsIgnoreCase("")) {
			if (userFileName.length() > MAX_FILENAME_LENGTH)
				userFileName = userFileName.substring(0,MAX_KEYWORDS_LENGTH);
			PacSatField userFileNameField = new PacSatField(userFileName, USER_FILE_NAME);
			fields.add(userFileNameField);
		}
		// 2a - 6 bytes - WISP Version??
		// 2e - 8 bytes - What is this?
		// 2f - 8 bytes - What is this?
		// Hack to see if we can get these messages to load into WISP Message Editor
		PacSatField wisp2A = new PacSatField("PGS0.06", 0x2A);
		fields.add(wisp2A);
		PacSatField wisp2E = new PacSatField(eight0, 0x2E);
		fields.add(wisp2E);
		PacSatField wisp2F = new PacSatField(eight0, 0x2F);
		fields.add(wisp2F);
	}
	
	public PacSatFileHeader(long fileId, long told, long tnew, int[] bytes) throws MalformedPfhException {
		timeOld = told;
		timeNew = tnew;
		rawBytes = bytes;
		fields = new ArrayList<PacSatField>();

		if (bytes.length < 2) throw new MalformedPfhException("Missing PFH.  Not enough bytes for fileId: "+Long.toHexString(fileId));
		int check1 = bytes[0];
		int check2 = bytes[1];
//		int check3 = bytes[2];
//		if (check1 == 0x00 && check2 == 0x00 && check3 == 0x00) {
//			// Empty PFH
//			return;
//		}
		if (check1 != TAG1) throw new MalformedPfhException("Missing "+Integer.toHexString(TAG1) +" for fileId: "+Long.toHexString(fileId));
		if (check2 != TAG2) throw new MalformedPfhException("Missing "+Integer.toHexString(TAG2)+" for fileId: "+Long.toHexString(fileId));

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

	public PacSatFileHeader(RandomAccessFile fileOnDisk) throws MalformedPfhException, IOException {
		fields = new ArrayList<PacSatField>();
		int[] bytes = new int[(int) fileOnDisk.length()];
		int p=0;
		boolean readingBytes = true;
		while (readingBytes) {
			try {
				int i = fileOnDisk.readUnsignedByte();
				bytes[p++] = i;
			} catch (EOFException e) {
				readingBytes = false;
			}
		}

		rawBytes = bytes;
		if (bytes.length < 5) throw new MalformedPfhException("Missing PFH");
		int check1 = bytes[0];
		int check2 = bytes[1];
		if (check1 != TAG1) throw new MalformedPfhException("Missing "+Integer.toHexString(TAG1));
		if (check2 != TAG2) throw new MalformedPfhException("Missing "+Integer.toHexString(TAG2));

		boolean readingHeader = true;
		int headerLength = 0;
		p = 2; // our byte position in the header
		// Header fields follow in the format ID, LEN, DATA
		while (readingHeader) {
			PacSatField field = new PacSatField(p,bytes);
			p = p + field.length + 3;
			if (field.isNull()) {
				readingHeader = false;
				headerLength = p;
			} else
				fields.add(field);
		}
		rawBytes = Arrays.copyOfRange(bytes, 0, p);
		
//		// recheck the checksum - debug only
//		PacSatField headerChecksum = getFieldById(HEADER_CHECKSUM);
//		short checkPfh = (short) headerChecksum.getLongValue();
//		short zero = 0;
//		PacSatField newHeaderChecksum = new PacSatField(zero, HEADER_CHECKSUM);
//		headerChecksum.copyFrom(newHeaderChecksum);
//		generateBytes();
//		
//		PacSatField headerChecksum2 = getFieldById(HEADER_CHECKSUM);
//		short bodyCS = (short) headerChecksum2.getLongValue();
//		
//		Log.infoDialog("CHECKSUMS", "PFH: " + headerChecksum.getLongValue() + "\nCALC: "+ bodyCS);
		
	} 

	
	public long getFileId() {
		return getFieldById(FILE_ID).getLongValue();
	}
	
	public String getFileName() {
		return getFieldById(FILE_ID).getLongHexString();
	}
	
	public Date getDate(int id) {
		PacSatField dateField = null;
		dateField = getFieldById(id);
		Date lastDate = null;
		if (dateField != null)
			lastDate = dateField.getDateValue();
		return lastDate;
	}

	public static int getUserTypeIndexByString(String str) {
		for (int i=0; i < userTypes.length; i++) {
			if (userTypeStrings[i].equalsIgnoreCase(str))
				return i;
		}
		return 0;
	}

	public static int getTypeIndexByString(String str) {
		for (int i=0; i < types.length; i++) {
			if (typeStrings[i].equalsIgnoreCase(str))
				return i;
		}
		return 0;
	}

	public static int getTypeIdByString(String str) {
		for (int i=0; i < types.length; i++) {
			if (typeStrings[i].equalsIgnoreCase(str))
				return types[i];
		}
		return 0;
	}

	public static String getTypeStringByIndex(int id) {
		return typeStrings[id];
	}

	public static String getTypeStringById(int id) {
		for (int i=0; i < types.length; i++) {
			if (types[i] == id)
				return typeStrings[i];
		}
		return "";
	}
	
	public int[] getBytes() {
		return rawBytes;
	}

	public int getType() {
		PacSatField typeField = null;
		typeField = getFieldById(PacSatFileHeader.FILE_TYPE);
		return (int) typeField.getLongValue();
	}
	
	public String getTypeString() {
		PacSatField typeField = null;
		typeField = getFieldById(PacSatFileHeader.FILE_TYPE);
		String s = "";
		if (typeField != null) {
			int t = (int) typeField.getLongValue();
			s = getTypeStringById(t);
			if (s == null) return "Unknown";
		}
		return s;
	}
	
	public String getFieldString(int fieldId) {
		PacSatField field = null;
		field = getFieldById(fieldId);
		String s = "";
		if (field != null) {
			s = field.getStringValue();
			if (s==null)
				s= "";
		}
		return s;
	}
	
	public String getDateString(int fieldId) {
		PacSatField field = null;
		field = getFieldById(fieldId);
		String s = "";
		if (field != null) {
			s = field.getDateString();
			if (s==null)
				s= "";
		}
		return s;
	}

	public PacSatField getFieldById(int id) {
		for (PacSatField field : fields) {
			if (field.id == id) return field;
		}
		return null;
	}
	
	public void setFileId(long fileid) {
		PacSatField fileNum = new PacSatField(fileid, FILE_ID);
		for (PacSatField field : fields) {
			if (field.id == FILE_ID) {
				field.copyFrom(fileNum);
				break;
			}
		}
		generateBytes();
	}

	public int getState() {
		return this.state;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public String[] getTableFields() {
		String[] fields = new String[FileHeaderTableModel.MAX_TABLE_FIELDS];

		fields[0] = getFieldById(FILE_ID).getLongHexString();
		if (userDownLoadPriority == Directory.PRI_NEVER) { 
			fields[1] = "N";
		} else if (userDownLoadPriority > Directory.PRI_NONE) { 
			fields[1] = Integer.toString(userDownLoadPriority);
		} else
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
		
		fields[5] = "" + PacSatField.getDateString(new Date(timeOld*1000));
		
		if (getFieldById(UPLOAD_TIME) != null)
			fields[6] = ""+getFieldById(UPLOAD_TIME).getDateString();
		else
			fields[6] = "";
		fields[7] = "" +  PacSatField.getDateString(new Date(timeNew*1000));
		
		fields[8] = ""+getFieldById(FILE_SIZE).getLongString();
		int percent = (int) (100 * downloadedBytes/(double)getFieldById(FILE_SIZE).getLongValue());
		fields[9] = "" + percent; 
		if (getFieldById(TITLE) != null)
			fields[10] = getFieldById(TITLE).getStringValue();
		else if (getFieldById(FILE_NAME) != null && getFieldById(FILE_EXT) != null)
			fields[10] = getFieldById(FILE_NAME).getStringValue() +'.' + getFieldById(FILE_EXT).getStringValue();
		
		fields[11] = this.getTypeString();
		
		if (getFieldById(KEYWORDS) != null)
			fields[12] = getFieldById(KEYWORDS).getStringValue();
		else
			fields[12] = "";

		if (getFieldById(USER_FILE_NAME) != null)
			fields[13] = getFieldById(USER_FILE_NAME).getStringValue();
		else
			fields[13] = "";

		return fields;
	}
	
	public String toFullString() {
		String s = "";
		for (PacSatField f : fields) {
			s = s + f + "\n";
		}
		return s;
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
		// If the fileid is the same, then these are the same PFH
		PacSatField id = getFieldById(FILE_ID);
		if (id == null) return -1;
		PacSatField pid = p.getFieldById(FILE_ID);
		if (pid == null) return -1;
		if (id.getLongValue() == pid.getLongValue()) return 0;
		
		// Otherwise we order by uploadtime
		PacSatField uploadTimeField = getFieldById(UPLOAD_TIME);
		if (uploadTimeField == null) return -1;
		long uploadTime = uploadTimeField.getLongValue();
		PacSatField pUploadTimeField = p.getFieldById(UPLOAD_TIME);
		if (pUploadTimeField == null) return 1;
		long pUploadTime = pUploadTimeField.getLongValue();
		if (uploadTime == pUploadTime)
			return 0;
		if (uploadTime < pUploadTime)
			return -1;
		if (uploadTime > pUploadTime)
			return 1;
		return 0;
	}
	
	public static void main(String[] args) throws IOException, FrameException, MalformedPfhException {
		Config.init("Test");
		KissFrame kiss = new KissFrame();
		int[] by1= {0xC0,0x00,0xA2,0xA6,0xA8,0x40,0x40,0x40,0x02,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x17,0x03,0xBD,0x20,0xD3,0x14,0x00,0x00,0x00,0x00,0x00,0x00,0x45,0x22,0x20,0x5C,0x5C,0x25,0x20,0x5C,0xAA,0x55,0x01,0x00,0x04,0xD3,0x14,0x00,0x00,0x02,0x00,0x08,0x41,0x4C,0x31,0x31,0x31,0x32,0x32,0x33,0x03,0x00,0x03,0x20,0x20,0x20,0x04,0x00,0x04,0x5C,0x0A,0x00,0x00,0x05,0x00,0x04,0x46,0xD2,0x1E,0x5C,0x06,0x00,0x04,0x45,0x22,0x20,0x5C,0x12,0x00,0x04,0x45,0x22,0x20,0x5C,0x07,0x00,0x01,0x00,0x08,0x00,0x01,0xC9,0x09,0x00,0x02,0xE6,0xAC,0x0A,0x00,0x02,0xE1,0x0A,0x0B,0x00,0x02,0x50,0x00,0x00,0x00,0x00,0x7C,0x59,0xC0};
		int[] by = {0xC0,0x00,0xA2,0xA6,0xA8,0x40,0x40,0x40,0x02,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x17,0x03,0xBD,0x20,0xDF,0x14,0x00,0x00,0xEC,0x00,0x00,0x00,0x88,0xA8,0x1F,0x5C,0x1C,0xBC,0x1F,0x5C,0x00,0x00,0x00,0x21,0x1D,0xC0};
		for (int b : by)
			kiss.add(b);
		Ax25Frame ax = new Ax25Frame(kiss);
		System.out.println(ax);
		BroadcastDirFrame bf = new BroadcastDirFrame(ax);
		System.out.println(bf);
		
		/*
		PacSatFileHeader pfh = new PacSatFileHeader("G0KLA", "ALL", 100, (short)0, 0, 0, "Re: The quick brown fox and the lazy dogs", "DOGS FOX", "JUMPING.FOX");
		//pfh.setFileId(0x0000);
		RandomAccessFile fileOnDisk = null;
		try {
			fileOnDisk = new RandomAccessFile("test.pfh", "rw"); // opens file and creates if needed
			//fileOnDisk.seek(0);
			for (int i : pfh.rawBytes)
				fileOnDisk.write(i);
			System.out.println("Wrote: test.pfh");
		} finally {
			fileOnDisk.close();
		}
		*/
	}
}
