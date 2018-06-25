package pacSat.frames;

public abstract class PacSatFrame {

	public static final int PSF_STATUS_PBLIST = 0;
	public static final int PSF_STATUS_PBFULL = 1;
	public static final int PSF_STATUS_PBSHUT = 2;
	public static final int PSF_STATUS_BBSTAT = 3;
	public static final int PSF_STATUS_STATUS = 4;
	
	public static final int PSF_RESPONSE_OK = 10;
	public static final int PSF_RESPONSE_ERROR = 11;

	public static final int PSF_BROADCAST_DIR = 20;
	public static final int PSF_BROADCAST_FILE = 21;
	
	public static final int PSF_REQ_DIR = 30;
	public static final int PSF_REQ_FILE = 31;
	
	public int frameType;
	
	public abstract int[] getBytes();
	public abstract String toString();
}
