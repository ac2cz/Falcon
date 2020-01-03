package pacSat.frames;

import java.io.File;

import fileStore.PacSatFile;

public class PacSatEvent extends PacSatPrimative {
	public static final int UL_CONNECTED = 0;
	public static final int UL_DISCONNECTED = 1;
	public static final int UL_REQUEST_UPLOAD = 2;
	public static final int UL_DATA = 3;
	public static final int UL_DATA_END = 4;
	public static final int UL_DATALINK_TERMINATED = 5;
	public static final int UL_GO_RESP = 6;
	public static final int UL_ERROR_RESP = 7;
	public static final int UL_ACK_RESP = 8;
	public static final int UL_NAK_RESP = 9;
	public static final int UL_TIMER_T3_EXPIRY = 10;
	
	public static final String types[] = {
			"UL_CONNECT",
			"UL_DISCONNECTED",
			"UL_REQUEST_UPLOAD",
			"UL_DATA",
			"UL_DATA_END",
			"UL_DATALINK_TERMINATED",
			"UL_GO_RESP",
			"UL_ERROR_RESP",
			"UL_ACK_RESP",
			"UL_NAK_RESP"
		};
	
	public int type;
	public PacSatFile psf;
	public long continueFileNumber;
	public long fileLength;
	public int[] bytes;
	
	public PacSatEvent(int type) {
		this.type = type;
	}

	public PacSatEvent(PacSatFile pacSatFile) {
		this.psf = pacSatFile;
		type = UL_REQUEST_UPLOAD;
	}
	
	public PacSatEvent(int[] bytes, long offset, long fileSize) {
		this.bytes = bytes;
		continueFileNumber = offset;
		fileLength = fileSize;
		type = UL_DATA;
	}

	@Override
	public String toString() {
		String s = "";
		if (type == UL_DATA) 
			s = s + types[type] + " Offset: " + continueFileNumber + " " + String.format("%.2f",(float)(100*continueFileNumber/(float)fileLength)) + "%";
		else
			s = s + types[type];
		return s;
	}

}


