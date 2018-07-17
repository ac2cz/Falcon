package pacSat.frames;
import java.util.Arrays;

import common.Spacecraft;


public class Ax25Frame {
	public static final int PID_BROADCAST = 0xbb;
	public static final int PID_DIR_BROADCAST = 0xbd;
	public static final int PID_NO_PROTOCOL = 0xf0;
	
	public static final int TYPE_S = 0x1;
	public static final int TYPE_I = 0x0;
	
	// U Frame Types
	public static final int U_CONTROL_MASK = 0b11101111;
	public static final int TYPE_U_SET_ASYNC_BALANCE_MODE_E = 0b01101111;
	public static final int TYPE_U_SET_ASYNC_BALANCE_MODE   = 0b00101111;
	public static final int TYPE_U_DISCONNECT               = 0b01000011;
	public static final int TYPE_U_DISCONNECT_MODE          = 0b00001111;
	public static final int TYPE_U_ACKNOWLEDGE              = 0b01100011;
	public static final int TYPE_U_FRAME_REJECT             = 0b10000111;
	public static final int TYPE_UI            				= 0b00000011;
	public static final int TYPE_U_EXCH_ID     				= 0b10101111;
	public static final int TYPE_U_TEST     				= 0b11100011;
	
	// S Frame Types - based on SS - supervisory bits
	public static final int TYPE_S_RECEIVE_READY = 0b00;
	public static final int TYPE_S_RECEIVE_NOT_READY = 0b01;
	public static final int TYPE_S_REJECT = 0b10;
	public static final int TYPE_S_SELECTIVE_REJECT = 0b11;
	
	public static final int C_BIT     				= 0b10000000; // If imlemented
	public static final int RR_BITS     			= 0b01100000; // Not imlemented
	
	public String toCallsign;
	public String fromCallsign;
	public String viaCallsign;
	int controlByte;
	int pid;
	int[] bytes;
	int[] data;
	int type = TYPE_UI;
	int NR; // the N(R) number of the frame
	int NS; // the N(S) number of the frame
	int SS; // the supervisory bits of an S frame
	
	public Ax25Frame(String fromCallsign, String toCallsign, int pid, int[] data) {
		this.fromCallsign = fromCallsign;
		this.toCallsign = toCallsign;
		this.pid = pid;
		this.data = data;
		controlByte = TYPE_UI;
		int[] byto = encodeCall(toCallsign, false, C_BIT);
		int j = 0;
		bytes = new int[16 + data.length];
		for (int i : byto)
			bytes[j++] = i;
		int[] byfrom = encodeCall(fromCallsign,true,0);
		for (int i : byfrom)
			bytes[j++] = i;
		bytes[j++] = controlByte;
		bytes[j++] = pid;
		for (int i : data)
			bytes[j++] = i;
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
		
//		if (bytes.length < 15) {
//			String d = "";
//			d = d + "From:" + fromCallsign + " to " + toCallsign;
//			for (int i : bytes)
//				d = d + " " + Integer.toHexString(i);
//			throw new FrameException("Not enough bytes for a valid I or UI Frame" + d);
//		}
		
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
		
		controlByte = bytes[14+v]; 
		if ((controlByte & 0b1) == 0) {  // bit 0 = 0 if its an I frame
			type = TYPE_I;
			NR = (controlByte >> 5) & 0b111;
			NS = (controlByte >> 1) & 0b111;
			pid = bytes[15+v];
			data = Arrays.copyOfRange(bytes, 16+v, bytes.length);
//			String d = "";
//			d = d + "From:" + fromCallsign + " to " + toCallsign;
//			for (int i : bytes)
//				d = d + " " + Integer.toHexString(i);
//			throw new FrameException("I FRAME: " + headerString() + d);
		} else if ((controlByte & 0b11) == 0b11) { // bit 0 and 1 both 1 if its an U frame
			type = controlByte & U_CONTROL_MASK;
			if (type != TYPE_UI) {
				// All other U frames are a command/response and have no INFO/DATA
				return;
				//throw new FrameException("U FRAME: " + headerString() + controlByte);
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
			NR = (controlByte >> 5) & 0b111;
			SS = (controlByte >> 2) & 0b11;
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
			
	public String getTypeString() {
		switch(type) {
			case TYPE_UI: return "UI";
			case TYPE_S: return "S";
			case TYPE_I: return "I";
			case TYPE_U_SET_ASYNC_BALANCE_MODE_E: return "U-SABME";
			case TYPE_U_SET_ASYNC_BALANCE_MODE: return "U-SABM";
			case TYPE_U_DISCONNECT: return "U-DISC";
			case TYPE_U_DISCONNECT_MODE: return "U-DM";
			case TYPE_U_ACKNOWLEDGE: return "UA";
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
			if (!toCallsign.startsWith("QST")) return true;
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

	public boolean isIFrame() {
		if (type == TYPE_I) return true;
		return false;
	}

	public boolean isSFrame() {
		if (type == TYPE_S) return true;
		return false;
	}
	
	public boolean isUFrame() {
		if (type == TYPE_U_SET_ASYNC_BALANCE_MODE_E) return true;
		if (type == TYPE_U_SET_ASYNC_BALANCE_MODE) return true;
		if (type == TYPE_U_DISCONNECT) return true;
		if (type == TYPE_U_DISCONNECT_MODE) return true;
		if (type == TYPE_U_ACKNOWLEDGE) return true;
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
	
	public static int[] encodeCall(String callsign, boolean finalCall, int commandBits) {
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
		by[6] = by[6] | commandBits | RR_BITS;
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
		s = s + " Type: " + getTypeString();
		if (type == TYPE_UI)
			s = s + " PID: " + Integer.toHexString(pid & 0xff) + " ";
		else if (type == TYPE_S) {
			s = s + " NR: " + Integer.toHexString(NR & 0xf) + " ";
			s = s + " SS: " + getSTypeString() + " ";
		} else {
			s = s + " NR: " + Integer.toHexString(NR & 0xf) + " ";
			s = s + " NS: " + Integer.toHexString(NS & 0xf) + " ";
			if (data != null)
				for (int i : data)
					s = s + " " + Integer.toHexString(i);
		}
		
		return s;
	}
	
	public String toString() {
		String s = headerString();
		if (isBroadcastFileFrame()) s = s + "FILE> ";
		if (data != null) {
			s = s + " " + '"' + makeString(data) + '"';
		}
		return s;
	}
	
	
	
	// Test routine

//	public static final void main(String[] argc) throws FrameException {
//			int[] by = Ax25Frame.encodeCall("G0KLA-4", false, C_BIT); // set the two command bits because WISP does
//					
//			for (int b : by)
//				System.out.print((char)b);
//			System.out.println("");
//			
//			
////			int[] by = {0x82, 0x86, 0x64, 0x86, 0xB4, 0x40, 0x60};
////			int[] by = {0xa0, 0x8c, 0xa6, 0x66, 0x40, 0x40, 0xf9};
//			for (int b : by) {
//				boolean[] bits = KissFrame.intToBin8(b);
//				for (boolean bit : bits)
//					System.out.print(bit ? 1 : 0);
//				System.out.println(" " + Integer.toHexString(b));
//			}
//			String call = Ax25Frame.getcall(by);
//			System.out.println(call);
//			System.out.println(isFinalCall(by));
//			
//			boolean[] bits = KissFrame.intToBin8(0x73);
//			for (boolean bit : bits)
//				System.out.print(bit ? 1 : 0);
//			System.out.println(" " + Integer.toHexString(0xc1));
//		}
}
