package pacSat.frames;

public abstract class PacSatFrame extends PacSatPrimative {

	// Downlink frame types
	public static final int PSF_STATUS_PBLIST = 0;
	public static final int PSF_STATUS_PBFULL = 1;
	public static final int PSF_STATUS_PBSHUT = 2;
	public static final int PSF_STATUS_BBSTAT = 3;
	public static final int PSF_STATUS_BYTE = 4;
	
	
	public static final int PSF_RESPONSE_OK = 10;
	public static final int PSF_RESPONSE_ERROR = 11;
	public static final int PSF_RESPONSE_OK_OTHER = 15;  // An OK response to another callsign - we can ignore

	public static final int PSF_BROADCAST_DIR = 20;
	public static final int PSF_BROADCAST_FILE = 21;
	
	public static final int PSF_REQ_DIR = 30;
	public static final int PSF_REQ_FILE = 31;
	
	
	// Uplink frame types
	public static final int PSF_UL_DATA = 100;
	
	public static final int PSF_DATA_END = 101;
	public static final int PSF_LOGIN_RESP = 102;
	public static final int PSF_UPLOAD_CMD = 103;
	public static final int PSF_UL_GO_RESP = 104;
	public static final int PSF_UL_ERROR_RESP = 105;
	public static final int PSF_UL_ACK_RESP = 106;
	public static final int PSF_UL_NAK_RESP = 107;
	public static final int DOWNLOAD_CMD = 108;
	public static final int DL_ERROR_RESP = 109;
	public static final int DL_ABORTED_RESP = 110;
	public static final int DL_COMPLETED_RESP = 111;
	public static final int DL_ACK_CMD = 112;
	public static final int DL_NAK_CMD = 113;
	public static final int DIR_SHORT_CMD = 114;
	public static final int DIR_LONG_CMD = 115;
	public static final int SELECT_CMD = 116;
	public static final int SELECT_RESP = 117;
	
	// AX25 U and S frames - should not be here because we should not be dealing with them in UL or DL state machine
	public static final int TYPE_U_SABM = 1000;
	public static final int TYPE_UA = 1001;
	public static final int TYPE_U_DISC = 1002;
	
	public int frameType;
	
	public abstract int[] getBytes();
	public abstract String toString();
}
