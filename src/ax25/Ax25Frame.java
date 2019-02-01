package ax25;
import java.util.Arrays;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import pacSat.frames.FrameException;
import pacSat.frames.StatusFrame;


public class Ax25Frame extends Ax25Primitive{
	public static final int PID_BROADCAST = 0xbb;
	public static final int PID_DIR_BROADCAST = 0xbd;
	public static final int PID_NO_PROTOCOL = 0xf0;
	
	public static final int TYPE_S = 0x1;
	public static final int TYPE_I = 0x0;
	
	// U Frame Types
	public static final int U_CONTROL_MASK = 0b11101111;
	public static final int TYPE_U_SABME = 0b01101111;
	public static final int TYPE_U_SABM   = 0b00101111;
	public static final int TYPE_U_DISCONNECT               = 0b01000011;
	public static final int TYPE_U_DISCONNECT_MODE          = 0b00001111;
	public static final int TYPE_UA              			= 0b01100011;
	public static final int TYPE_U_FRAME_REJECT             = 0b10000111;
	public static final int TYPE_UI            				= 0b00000011;
	public static final int TYPE_U_EXCH_ID     				= 0b10101111;
	public static final int TYPE_U_TEST     				= 0b11100011;
	
	// S Frame Types - based on SS - supervisory bits
	public static final int TYPE_S_RECEIVE_READY = 0b00;
	public static final int TYPE_S_RECEIVE_NOT_READY = 0b01;
	public static final int TYPE_S_REJECT = 0b10;
	public static final int TYPE_S_SELECTIVE_REJECT = 0b11;
	
	//public static final int C_BIT     				= 0b10000000; // If imlemented
	public static final int COMMAND = 1;
	public static final int RESPONSE = 0;
	public static final int RR_BITS     			= 0b01100000; // Not imlemented
	
	public String toCallsign;
	public String fromCallsign;
	public String viaCallsign;
	int controlByte;
	public int pid;
	int[] bytes;
	int[] data;
	int type = TYPE_UI;
	int C; // the Command Response bit
	int PF; // the poll/final bit (P/F)
	int NR; // the Receive  Sequence Number N(R) number of the frame.  Contains the last valid I frame received, set to VR, acknowledging all frames to NR-1
	int NS; // the Send Sequence Number N(S) number of the frame.  Just prior to sending it is updated to equal VS
	int SS; // the supervisory bits of an S frame
	
	/**
	 * For Frames that have PID and Info - I and UI
	 * @param fromCallsign
	 * @param toCallsign
	 * @param controlByte
	 * @param pid
	 * @param data
	 */
	public Ax25Frame(String fromCallsign, String toCallsign, int controlByte, int command, int pid, int[] data) {
		this.fromCallsign = fromCallsign;
		this.toCallsign = toCallsign;
		this.pid = pid;
		this.data = data;
		this.controlByte = controlByte;
		this.C = command & 0b1;
		PF = (controlByte >> 4) & 0x1;
		if ((controlByte & 0b1) == 0) {  // bit 0 = 0 if its an I frame
			type = TYPE_I;
		} else if ((controlByte & 0b11) == 0b11) { // bit 0 and 1 both 1 if its an U frame
			type = controlByte & U_CONTROL_MASK;
		} else if ((controlByte & 0b11) == 0b01) { // bit 1 = 1 and bit 0 = 0 if its an S frame
			type = TYPE_S;
		} else {
			type = 0xff;
		}
		int command2 = 0;
		if (command == 0) command2 = 1;
		int[] byto = encodeCall(toCallsign, false, command); // command bit goes in dest
		int[] byfrom = encodeCall(fromCallsign,true,command2); // opposite bit goes in source
		int j = 0;
		int dataLen = 0;
			if (data != null)
				dataLen = data.length;
			bytes = new int[16 + dataLen];
			for (int i : byto)
				bytes[j++] = i;
			for (int i : byfrom)
				bytes[j++] = i;
			bytes[j++] = controlByte;
			bytes[j++] = pid;
			if (data != null)
				for (int i : data)
					bytes[j++] = i;
	}

	/**
	 * For Frames that have no PID and INFO - S and U
	 * @param fromCallsign
	 * @param toCallsign
	 * @param controlByte
	 */
	public Ax25Frame(String fromCallsign, String toCallsign, int controlByte, int command) {
		this.fromCallsign = fromCallsign;
		this.toCallsign = toCallsign;
		this.controlByte = controlByte;
		this.C = command & 0b1;
		int command2 = 0;
		if (command == 0) command2 = 1;
		int[] byto = encodeCall(toCallsign, false, command);
		int[] byfrom = encodeCall(fromCallsign,true,command2);
		int j = 0;
		PF = (controlByte >> 4) & 0x1;
		if ((controlByte & 0b1) == 0) {  // bit 0 = 0 if its an I frame
			type = TYPE_I; // though this would be an error
		} else if ((controlByte & 0b11) == 0b11) { // bit 0 and 1 both 1 if its an U frame
			type = controlByte & U_CONTROL_MASK;
		} else if ((controlByte & 0b11) == 0b01) { // bit 1 = 1 and bit 0 = 0 if its an S frame
			type = TYPE_S;
		} else {
			type = 0xff;
		}
		bytes = new int[15];
		for (int i : byto)
			bytes[j++] = i;
		for (int i : byfrom)
			bytes[j++] = i;
		bytes[j++] = controlByte;
	}

	/**
	 * Constructor that takes a KISS frame and decomposes it into either an I frame or a UI frame
	 * @param ui
	 * @throws FrameException
	 */
	public Ax25Frame(KissFrame kiss) throws FrameException {
		bytes = kiss.getDataBytes();

		if (bytes.length < 15) {
			throw new FrameException("Not enough bytes for a valid S, I or UI Frame" );
		}

		// First bytes are header
		int[] callbytes = Arrays.copyOfRange(bytes, 0, 7);
		int[] callbytes2 = Arrays.copyOfRange(bytes, 7, 14);
		toCallsign = getcall(callbytes);
		fromCallsign = getcall(callbytes2);
		int destCbit = bytes[6] >> 7;
		int sourceCbit = bytes[13] >> 7;
		if (destCbit != sourceCbit) {
			// then we are version 2.x, which is what we expect
			if (destCbit == 1)
				C = 1;
			else
				C = 0;
		} else {
			C = destCbit; // Unclear if this is right, but we get this for the UI frames
			//Log.println("Pre 2.x command bit format" + this);
			//throw new FrameException("Pre 2.x command bit format");
		}
				
		int v=0;
		// VIA, we have more callsigns
		if (!isFinalCall(callbytes2)) { // digipeat
			int[] callbytes3 = Arrays.copyOfRange(bytes, 14, 21);
			viaCallsign = getcall(callbytes3);
			v=7;
			if (!isFinalCall(callbytes3)) { // 2nd digipeat
				throw new FrameException(">>>>>>>>>>> more than one VIA not supported: " + headerString() );
			}
		} 
		if (bytes.length < 15+v) {
			throw new FrameException("Not enough bytes for a valid S, I or UI Frame: " + bytes.length );
		}
		controlByte = bytes[14+v]; 
		if ((controlByte & 0b1) == 0) {  // bit 0 = 0 if its an I frame
			if (bytes.length < 16+v) {
				throw new FrameException("Not enough bytes for a valid I Frame: " + bytes.length + " " + toString());
			}
			type = TYPE_I;
			NR = (controlByte >> 5) & 0b111;
			NS = (controlByte >> 1) & 0b111;
			PF = (controlByte >> 4) & 0b1;
			pid = bytes[15+v];
			data = Arrays.copyOfRange(bytes, 16+v, bytes.length);
//			String d = "";
//			d = d + "From:" + fromCallsign + " to " + toCallsign;
//			for (int i : bytes)
//				d = d + " " + Integer.toHexString(i);
//			throw new FrameException("I FRAME: " + headerString() + d);
		} else if ((controlByte & 0b11) == 0b11) { // bit 0 and 1 both 1 if its an U frame
			type = controlByte & U_CONTROL_MASK;
			PF = (controlByte >> 4) & 0b1;
			if (type != TYPE_UI) {
				// All other U frames are a command/response and have no INFO/DATA
				return;
			}
			if (bytes.length < 16) {
			
				String d = "";
				d = d + "From:" + fromCallsign + " to " + toCallsign;
				for (int i : bytes)
					d = d + " " + Integer.toHexString(i);
				throw new FrameException("Not enough bytes for a valid UI Frame" + d);
			} 

			pid = bytes[15];
			data = Arrays.copyOfRange(bytes, 16+v, bytes.length);
		} else if ((controlByte & 0b11) == 0b01) { // bit 1 = 1 and bit 0 = 0 if its an S frame
			type = TYPE_S;
			// for an S frame we need to know if it is a command or a response.  
			// This is encoded in the address filed in MSB of SSID field
			// In the older protocol both bits are the same, in the newer protocol:
			// It is a command if the C bit is set for the Destination
			// It is a response if the C bit is set for the Source
			NR = (controlByte >> 5) & 0b111;
			SS = (controlByte >> 2) & 0b11;
			PF = (controlByte >> 4) & 0b1;
			return;
			//throw new FrameException("SUPERVISORY FRAME: " + headerString() + SS);
		} else {
			type = 0xff;
			String d = "";
			for (int i : bytes)
				d = d + " " + Integer.toHexString(i);
			throw new FrameException("ERROR: Frame type not supported: " + headerString() + d);
		}
	}
	
	public boolean isCommandFrame() {
		if (C == 1) return true;
		return false;
	}
	
	public int[] getBytes() {
		return bytes;
	}
	
	public int[] getDataBytes() {
		return data;
	}
	
	public String getSTypeString() {
		switch(SS) {
			case TYPE_S_RECEIVE_READY: return "RR";
			case TYPE_S_RECEIVE_NOT_READY: return "RNR";
			case TYPE_S_REJECT: return "REJ";
			case TYPE_S_SELECTIVE_REJECT: return "SREJ";
			
			default:
			    return "UNK";		
		}
	}
			
	public static String getTypeString(int type) {
		switch(type) {
			case TYPE_UI: return "UI";
			case TYPE_S: return "S";
			case TYPE_I: return "I";
			case TYPE_U_SABME: return "U-SABME";
			case TYPE_U_SABM: return "U-SABM";
			case TYPE_U_DISCONNECT: return "U-DISC";
			case TYPE_U_DISCONNECT_MODE: return "U-DM";
			case TYPE_UA: return "UA";
			case TYPE_U_FRAME_REJECT: return "U-FRMR";
			case TYPE_U_EXCH_ID: return "U-XID";
			case TYPE_U_TEST: return "U-TEST";
			default:
			    return "UNK";		
		}
	}
	public boolean isBroadcastFileFrame() {
		if (data == null) return false;
		if (type != TYPE_UI) return false;
		if (toCallsign.startsWith("QST"))
			if ((pid & 0xff) == PID_BROADCAST) return true;
		return false;
	}
	
	public boolean isDirectoryBroadcastFrame() {
		if (data == null) return false;
		if (type != TYPE_UI) return false;
		if (toCallsign.startsWith("QST"))
			if ((pid & 0xff) == PID_DIR_BROADCAST) return true;
		return false;
	}
	
	public boolean isStatusFrame() {
		if (data == null) return false;
		if (type != TYPE_UI) return false;
		if ((pid & 0xff) == PID_NO_PROTOCOL) {
			if (toCallsign.startsWith(StatusFrame.PBLIST)) return true;
			if (toCallsign.startsWith(StatusFrame.PBFULL)) return true;
			if (toCallsign.startsWith(StatusFrame.PBSHUT)) return true;
			if (toCallsign.startsWith(StatusFrame.BBSTAT)) return true;
			if (toCallsign.startsWith(StatusFrame.BSTAT)) return true;
			return false;
		} 
		return false;
	}
	
	public boolean isResponseFrame() {
		if (data == null) return false;
		if (type != TYPE_UI) return false;
		if ((pid & 0xff) == PID_BROADCAST) {
			if (fromCallsign.startsWith(Config.get(Config.CALLSIGN))) return false;  // we don't send response frames must be a request bb
			if (!toCallsign.startsWith("QST")) return true;
		}
		return false;
	}

	public boolean isTlmFrame() {
		if (data == null) return false;
		if (type != TYPE_UI) return false;
		if ((pid & 0xff) == PID_NO_PROTOCOL) {
			if (toCallsign.startsWith("TLMI-1")) return true;
			if (toCallsign.startsWith("TLM2-1")) return true;
		}
		return false;
	}

	
	public boolean isLstatFrame() {
		if (type != TYPE_UI) return false;
		if ((pid & 0xff) == PID_NO_PROTOCOL) {
			if (toCallsign.startsWith("LSTAT")) return true;
		}
		return false;
	}

	public boolean isTimeFrame() {
		if (type != TYPE_UI) return false;
		if ((pid & 0xff) == PID_NO_PROTOCOL) {
			if (toCallsign.startsWith("TIME-1")) return true;
		}
		return false;
	}

	
	public boolean isIFrame() {
		if (type == TYPE_I) return true;
		return false;
	}

	public boolean isSFrame() {
		if (type == TYPE_S) return true;
		return false;
	}
	
	public boolean isUFrame() {
		if (type == TYPE_U_SABME) return true;
		if (type == TYPE_U_SABM) return true;
		if (type == TYPE_U_DISCONNECT) return true;
		if (type == TYPE_U_DISCONNECT_MODE) return true;
		if (type == TYPE_UA) return true;
		if (type == TYPE_U_FRAME_REJECT) return true;
		if (type == TYPE_U_EXCH_ID) return true;
		if (type == TYPE_U_TEST) return true;
		return false;
	}

	public static boolean isFinalCall(int[] framebuf) {
		if ((framebuf[6] & 0x01) == 1) return true;
		return false;
	}
	
	public static String getcall(int[] framebuf) {
	int	i;
	int	ssid;
	String	cbp = "";

	for (i=0; i<6; i++)		/* start by filling in the basic characters */
	    if (framebuf[i] == 0x40 || framebuf[i] == 0x20 ) // @ or space
	    	;//skip??
	    else
	    	cbp = cbp + (char)((framebuf[i] >> 1) & 0x7F);	/* unshift to ASCII */

	/* tack on the SSID if it isn't zero */
	if ((ssid = (framebuf[6] & 0x1E) >> 1) > 0)
	    cbp = cbp + "-" + ssid;
	else
		cbp = cbp + ' ';

	return cbp;
	}
	
	public static int[] encodeCall(String callsign, boolean finalCall, int command) {
		String call = callsign.split("-")[0];
		int ssid=0;
		try {
		ssid = Integer.parseInt(callsign.split("-")[1]);
		} catch (Exception e) {
			// leave it at zero, likely missing dash
		}
		int[] by = new int[7];
		for (int i=0; i<7; i++)
			by[i] = 0x40; // init to spaces
		
		for (int i=0; i<6 && i < call.length(); i++)
			by[i] = (call.charAt(i) & 0x7f) << 1;

		by[6] = (ssid << 1) & 0x1E;
		command = (command & 0b1) << 7;
		by[6] = by[6] | command | RR_BITS;
		if (finalCall) 
			by[6] = by[6] | 0x01; // or in 1 in the lowest bit
		return by;
		
	}
	
	public static boolean isPrintableChar( char c ) {
	    if (c >=0x20 && c <= 0x7f) return true;
	    return false;
	}
	
	public static String makeString(int[] data) {
		String s = "";
		for (int b : data) {
			char ch = (char) b;
			if (isPrintableChar(ch))
				s = s + ch;
			else
				s = s + ".";
		}
		return s;
	}

	public String headerString() {
		String s = "";
		s = s + "From:" + fromCallsign;
		if (viaCallsign != null)
			s = s + " VIA " + viaCallsign;
		s = s + " to " + toCallsign;
		s = s + " Ctrl: " + Integer.toHexString(controlByte);
		s = s + " Type: " + getTypeString(type);
		if (C == 1)
			s = s + " Cmd";
		else 
			s = s + " Res";
		if (type == TYPE_UI)
			s = s + " PID: " + Integer.toHexString(pid & 0xff) + " ";
		else if (type == TYPE_S) {
			s = s + " PF: " + PF; 
			s = s + " NR: " + Integer.toHexString(NR & 0x1f) + " ";
			s = s + " SS: " + getSTypeString() + " ";
		} else { // type U or I
			s = s + " PF: " + PF; 
			s = s + " NR: " + Integer.toHexString(NR & 0xf) + " ";
			s = s + " NS: " + Integer.toHexString(NS & 0xf) + " ";
			if (data != null)
				for (int i : data)
					s = s + " " + Integer.toHexString(i);
		}
		
		return s;
	}
	
	public String toString() {
		String s = "";
		if (Config.getBoolean(Config.DEBUG_DOWNLINK))
			s = s + headerString();
		if (isBroadcastFileFrame()) s = s + "FILE> ";
		if (isDirectoryBroadcastFrame()) s = s + "DIR> ";
		if (data != null) {
			s = s + makeString(data);
		}
		return s;
	}
	
	
	
	// Test routine

	public static final void main(String[] argc) throws FrameException {
		Config.init("test");
			int[] by = Ax25Frame.encodeCall("G0KLA-4", false, 1); // set the two command bits because WISP does
					
			for (int b : by)
				System.out.print((char)b);
			System.out.println("");
			
//			int[] by = {0x82, 0x86, 0x64, 0x86, 0xB4, 0x40, 0x60};
//			int[] by = {0xa0, 0x8c, 0xa6, 0x66, 0x40, 0x40, 0xf9};
			for (int b : by) {
				boolean[] bits = KissFrame.intToBin8(b);
//				for (boolean bit : bits)
//					System.out.print(bit ? 1 : 0);
//				System.out.println(" " + Integer.toHexString(b));
			}
			String call = Ax25Frame.getcall(by);
//			System.out.println(call);
//			System.out.println(isFinalCall(by));
			
			boolean[] bits = KissFrame.intToBin8(0x73);
//			for (boolean bit : bits)
//				System.out.print(bit ? 1 : 0);
//			System.out.println(" " + Integer.toHexString(0xc1));
			
			int[] by1 = {0xC0, 0x00, 0x82, 0x86, 0x64, 0x86, 0xB4, 0x40, 0x60, 0xA0, 0x8C, 0xA6, 0x66, 0x40, 0x40, 0xF9, 0x63, 0xC0};
			KissFrame kissFrame = new KissFrame();
			for (int b : by1) {
				kissFrame.add(b);
			}
			Ax25Frame frame = new Ax25Frame(kissFrame);
//			System.out.println(frame);
			
			Sframe uf = new Sframe("G0KLA", "PFS-12", 7, 1, Ax25Frame.TYPE_S_RECEIVE_READY, 1);
			System.out.println(uf);
		}
}
