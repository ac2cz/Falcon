package pacSat.frames;
import java.util.Arrays;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import fileStore.HoleLimits;
import fileStore.MalformedPfhException;
import fileStore.PacSatFileHeader;
import pacSat.Crc16;


public class BroadcastFileFrame extends BroadCastFrame {
	Ax25Frame uiFrame;
	int flags;
	public static final int L_BIT =     0b00000001;
	public static final int E_BIT =     0b00000010; // Last byte of frame is last byte of file, but always seems to be set!
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
	
	public BroadcastFileFrame(Ax25Frame ui) throws MalformedPfhException {
		frameType = PacSatFrame.PSF_BROADCAST_FILE;
		uiFrame = ui;
		bytes = ui.getDataBytes();
		flags = bytes[0];
		int[] by = {bytes[1],bytes[2],bytes[3],bytes[4]};
		fileId = KissFrame.getLongFromBytes(by);
		fileType  = bytes[5];
		int[] by2 = {bytes[6],bytes[7],bytes[8]};
		offset = KissFrame.getLongFromBytes(by2);
		if ((flags & L_BIT) == L_BIT) {
			// extended header
			System.err.println("LENGTH SET - CAN THAT BE RIGHT?");
			int[] by3 = {bytes[9],bytes[10]};
			length = KissFrame.getIntFromBytes(by3);
			data = Arrays.copyOfRange(bytes, 11, bytes.length-2);
		} else {
			data = Arrays.copyOfRange(bytes, 9, bytes.length-2);
		}

		if ((flags & E_BIT) == E_BIT) lastByteOfFile = true;
		int[] by4 = {bytes[bytes.length-2],bytes[bytes.length-1]};
		crc = KissFrame.getIntFromBytes(by4);
		if (!Crc16.goodCrc(bytes)) {
			//System.err.println("BAD CRC");
			throw new MalformedPfhException("Bad CRC for File ID: " +Long.toHexString(fileId));
		}
	}
	
	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	public long getFirst() {
		return offset;
	}
	
	public long getLast() {
		return offset + data.length - 1;
	}
	
	/**
	 * Return true if we start with the PFH bytes
	 * @return
	 */
	public boolean mayContainPFH() {
		int check1 = data[0];
		int check2 = data[1];
		
		if (data[0] == PacSatFileHeader.TAG2 
				&& data[1] == 0x01
				&& data[2] == 0x00
				&& data[3] == 0x04) return true; // looks like MIR-SAT-1 PFH header
		
		if (check1 == PacSatFileHeader.TAG1 || check1 == PacSatFileHeader.TAG1_ALT) {
			if (check2 == PacSatFileHeader.TAG2) return true;
		}

		return false;
	}
	
	public boolean hasLastByteOfFile() {
		return lastByteOfFile;
	}
	
	public String toString() {
		String s = "FILE> ";
//		if (Config.getBoolean(Config.DEBUG_DOWNLINK))
			s = s + uiFrame.headerString();
		s = s + "FLG: " + Integer.toHexString(flags & 0xff);
		s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
		s = s + " TYPE: " + Integer.toHexString(fileType & 0xff);
		s = s + " OFF: " + Long.toHexString(offset & 0xffffff);
		if ((flags & L_BIT) == L_BIT) 
			s = s + " LEN: " + Integer.toHexString(length & 0xffff);
		s = s + " CRC: " + Integer.toHexString(crc & 0xffff);
		//int actCrc = Crc16.calc(uiFrame.getDataBytes());
		//int actCrc = (int) CRC.calculateCRC(CRC.Parameters.XMODEM, uiFrame.getBytes());
	    //s = s + (" act=" + Integer.toHexString(actCrc & 0xffff)); 
		return s;
	}

//	private short calcCrc(int[] bytes) {
//	short crc;              /* global crc register.              */
//	crc = 0;                         /* clear crc register                */
//	for (all received data bytes)
//	    gen_crc(rx_byte);            /* crc the incoming data bytes       */
//	gen_crc(1st_crc_byte);           /* crc the incoming first crc byte   */
//	if(gen_crc(2nd_crc_byte))        /* crc the incoming second crc byte  */
//	 bad_crc();                      /* non-zero crc reg. is an error     */
//	else
//	 good_crc();                     /* zero crc is a good packet.        */
//	/* Function to take a byte and run it into the XMODEM crc generator.  */
//	/* Uses a global CRC register called ``crc''.                           */
//
//	gen_crc(data)
//	char data;
//	{
//	    int y;
//	    crc ^= data << 8;
//	    for(y=0; y < 8; y++)
//	        if( crc & 0x8000)
//	            crc = crc << 1 ^ 0x1021;
//	        else
//	            crc <<= 1;
//	    return crc;
//	}
//	}

	
	
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
