package pacSat.frames;
import java.util.Arrays;
import java.util.Date;

import ax25.Ax25Frame;
import ax25.KissFrame;
import fileStore.HoleLimits;
import fileStore.MalformedPfhException;
import fileStore.PacSatFileHeader;
import pacSat.Crc16;


public class BroadcastDirFrame extends PacSatFrame implements HoleLimits {
	Ax25Frame uiFrame;
	int flags;
	public static final int TYPE_BITS =     0b00000011;
	public static final int E_BIT =     0b00100000; // Last byte of frame is last byte of file
	public static final int VV_BIT =    0b00001100;
	public static final int NEWEST_FILE_BIT =  0b01000000;
	public static final int ZERO_BIT =  0b01110010;
	
	public long fileId;   // all frames with this number belong to the same file
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
	
	//boolean lastByteOfFile = false;
	boolean newestFile = false;
	
	public BroadcastDirFrame(Ax25Frame ui) throws MalformedPfhException {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		flags = bytes[0];
		
		if ((flags & NEWEST_FILE_BIT) == NEWEST_FILE_BIT) newestFile = true;
		int type_bits = flags & TYPE_BITS; // should alsways be zero
		
		int[] by = {bytes[1],bytes[2],bytes[3],bytes[4]};
		fileId = KissFrame.getLongFromBytes(by);
			
		int[] by2 = {bytes[5],bytes[6],bytes[7],bytes[8]};
		offset = KissFrame.getLongFromBytes(by2);

		int[] by3 = {bytes[9],bytes[10],bytes[11],bytes[12]};
		timeOld = KissFrame.getLongFromBytes(by3);
		oldDate = new Date(timeOld*1000);
		
		int[] by4 = {bytes[13],bytes[14],bytes[15],bytes[16]};
		timeNew = KissFrame.getLongFromBytes(by4);
		newDate = new Date(timeNew*1000);
		
		data = Arrays.copyOfRange(bytes, 17, bytes.length-2);
		if (offset == 0 && hasEndOfFile()) // we have the whole pfh, so can init it here for convenience
			pfh = new PacSatFileHeader(fileId, timeOld, timeNew, data);
		int[] by5 = {bytes[bytes.length-2],bytes[bytes.length-1]};
		crc = KissFrame.getIntFromBytes(by5);
	}
	
	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	public int[] getDataBytes() {
		return data;
	}
	
	public long getFirst() {
		return timeOld;
	}
	
	public long getLast() {
		return timeNew;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public boolean hasEndOfFile() {
		if ((flags & E_BIT) == E_BIT)
			return true;
		return false;
	}
	public String toLongString() {
		String s = toString();
		//int actCrc = Crc16.calc_xmodem(bytes);
		//int actCrc = (int) CRC.calculateCRC(CRC.Parameters.XMODEM, uiFrame.getBytes());
		//s = s + (" act=" + Integer.toHexString(actCrc & 0xffff)); 
		
	    s = s + " PFH> " +pfh.toString();
		return s;
	}
	public String toString() {
		String s = uiFrame.headerString();
		s = s + "FLG: " + Integer.toHexString(flags & 0xff);
		s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
		s = s + " TYPE: " + Integer.toHexString(fileType & 0xff);
		s = s + " OFF: " + Long.toHexString(offset & 0xffffff);
		if ((flags & NEWEST_FILE_BIT) == NEWEST_FILE_BIT) 
			s = s + " N";
		if (hasEndOfFile()) 
			s = s + " E";
		s = s + " OLD: " + oldDate;
		s = s + " NEW: " + newDate;
		s = s + " CRC: " + Integer.toHexString(crc & 0xffff);
		return s;
	}

}
