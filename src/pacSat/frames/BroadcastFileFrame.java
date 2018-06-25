package pacSat.frames;
import java.util.Arrays;

import pacSat.Crc16;
import passControl.PacSatEvent;


public class BroadcastFileFrame extends PacSatFrame {
	UiFrame uiFrame;
	int flags;
	public static final int L_BIT =     0b00000001;
	public static final int E_BIT =     0b00000010; // Last byte of frame is last byte of file
	public static final int VV_BIT =    0b00001100;
	public static final int ZERO_BIT =  0b01110010;
	
	public long fileId;   // all frames with this number belong to the same file
	int fileType;
	public long offset; // 24bit = byte number (position in the file) of the first byte in this frame
	            // byte 0 is the first byte of the pacsat header
	int length; // only present if L bit set
	public int[] data;
	int[] bytes;
	int crc;
	
	boolean lastByteOfFile = false;
	
	public BroadcastFileFrame(UiFrame ui) {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		flags = bytes[0];
		int[] by = {bytes[1],bytes[2],bytes[3],bytes[4]};
		fileId = KissFrame.getLongFromBytes(by);
		fileType  = bytes[5];
		int[] by2 = {bytes[6],bytes[7],bytes[8]};
		offset = KissFrame.getIntFromBytes(by2);
		if ((flags & L_BIT) == 1) {
			// extended header
			int[] by3 = {bytes[9],bytes[10]};
			length = KissFrame.getIntFromBytes(by3);
			data = Arrays.copyOfRange(bytes, 11, bytes.length-2);
		} else {
			data = Arrays.copyOfRange(bytes, 9, bytes.length-2);
		}

		if ((flags & E_BIT) == 1) lastByteOfFile = true;
		int[] by4 = {bytes[bytes.length-2],bytes[bytes.length-1]};
		crc = KissFrame.getIntFromBytes(by4);
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
		s = s + " CRC: " + Integer.toHexString(crc & 0xffff);
		int actCrc = Crc16.calc(uiFrame.getDataBytes());
		//int actCrc = (int) CRC.calculateCRC(CRC.Parameters.XMODEM, uiFrame.getBytes());
	    s = s + (" act=" + Integer.toHexString(actCrc & 0xffff)); 
		return s;
	}


	
	
//	public static final void main(String[] argc) {
//		int[] bytes = { 2, 39, 3, 0, 0, 16, 172, 6, 0, 84, 243, 32, 116, 32, 208 }; //-84, 6, 0
//		int[] by2 = {bytes[6],bytes[7],bytes[8]};
//		int offLow = KissFrame.getIntFromBytes(by2);
//		long offset = ((bytes[8] & 0xff) << 16) + offLow;
//		
//		System.out.println(((char)bytes[0]) + " " + offLow + " " + offset);
//		
////		byte b = (byte) 0xff;
////		int i = b & L_BIT;
////		System.out.println(Integer.toHexString(i));
//	}
	
}
