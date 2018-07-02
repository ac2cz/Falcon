package pacSat.frames;
import java.util.Arrays;
import java.util.Date;

import fileStore.MalformedPfhException;
import fileStore.PacSatFileHeader;
import pacSat.Crc16;
import passControl.PacSatEvent;


public class BroadcastDirFrame extends PacSatFrame {
	UiFrame uiFrame;
	int flags;
	public static final int L_BIT =     0b00000001;
	public static final int E_BIT =     0b00000010; // Last byte of frame is last byte of file
	public static final int VV_BIT =    0b00001100;
	public static final int ZERO_BIT =  0b01110010;
	
	long fileId;   // all frames with this number belong to the same file
	int fileType;
	long offset; // 24bit = byte number (position in the file) of the first byte in this frame
	            // byte 0 is the first byte of the pacsat header
	int offsetMsb;
	int length; // only present if L bit set
	long timeOld;
	Date oldDate;
	long timeNew;
	Date newDate;
	int[] bytes;
	int[] data;
	int crc;
	
	public PacSatFileHeader pfh;
	
	boolean lastByteOfFile = false;
	
	public BroadcastDirFrame(UiFrame ui) throws MalformedPfhException {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		flags = bytes[0];
		int[] by = {bytes[1],bytes[2],bytes[3],bytes[4]};
		fileId = KissFrame.getLongFromBytes(by);
			
		fileType  = bytes[5];
		int[] by2 = {bytes[6],bytes[7],bytes[8]};
		offset = KissFrame.getLongFromBytes(by2);
		int l=0;
		if ((flags & L_BIT) == 1) {
			// extended header
			int[] by3 = {bytes[9],bytes[10]};
			length = KissFrame.getIntFromBytes(by3);
			l=2;
		}

		if ((flags & E_BIT) == 1) lastByteOfFile = true;
		
		int[] by3 = {bytes[9+l],bytes[10+1],bytes[11+l],bytes[12+l]};
		timeOld = KissFrame.getLongFromBytes(by3);
		oldDate = new Date(timeOld*1000);
		
		int[] by4 = {bytes[13+l],bytes[14+l],bytes[15+l],bytes[16+l]};
		timeNew = KissFrame.getLongFromBytes(by4);
		newDate = new Date(timeNew*1000);
		
		data = Arrays.copyOfRange(bytes, 17+l, bytes.length-2);
		pfh = new PacSatFileHeader(data);
		int[] by5 = {bytes[bytes.length-2],bytes[bytes.length-1]};
		crc = KissFrame.getIntFromBytes(by5);
	}
	
	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	public String toString() {
		String s = uiFrame.headerString();
		s = s + "FLG: " + Integer.toHexString(flags & 0xff);
		s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
		s = s + " TYPE: " + Integer.toHexString(fileType & 0xff);
		s = s + " OFF: " + Long.toHexString(offset & 0xffffff);
		if ((flags & L_BIT) == 1) 
			s = s + " LEN: " + Integer.toHexString(length & 0xffff);
		s = s + " OLD: " + oldDate;
		s = s + " NEW: " + newDate;
		s = s + " CRC: " + Integer.toHexString(crc & 0xffff);
		int actCrc = Crc16.calc_xmodem(bytes);
		//int actCrc = (int) CRC.calculateCRC(CRC.Parameters.XMODEM, uiFrame.getBytes());
		s = s + (" act=" + Integer.toHexString(actCrc & 0xffff)); 
		
	    s = s + " PFH> " +pfh.toString();
		return s;
	}

}
