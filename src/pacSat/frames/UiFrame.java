package pacSat.frames;
import java.util.Arrays;

import common.Spacecraft;


public class UiFrame {
	public static final int PID_BROADCAST = 0xbb;
	public static final int PID_DIR_BROADCAST = 0xbd;
	public static final int PID_NO_PROTOCOL = 0xf0;
	
	public String toCallsign;
	public String fromCallsign;
	int controlByte;
	int pid;
	int[] bytes;
	int[] data;
	
	public UiFrame(String fromCallsign, String toCallsign, int pid, int[] data) {
		this.fromCallsign = fromCallsign;
		this.toCallsign = toCallsign;
		this.pid = pid;
		this.data = data;
		controlByte = 0x20 | 0x03;  // UI frame is 11 in the bottom 2 bits
		int[] byfrom = encodeCall(fromCallsign);
		int[] byto = encodeCall(toCallsign);
		int j = 0;
		bytes = new int[16 + data.length];
		for (int i : byto)
			bytes[j++] = i;
		for (int i : byfrom)
			bytes[j++] = i;
		bytes[j++] = controlByte;
		bytes[j++] = pid;
		for (int i : data)
			bytes[j++] = i;
	}
	
	/**
	 * Constructor that takes a KISS frame and decomposes it into
	 * @param ui
	 * @throws FrameException
	 */
	public UiFrame(KissFrame kiss) throws FrameException {
		if (kiss.length < 17) throw new FrameException("Not enough bytes for a valid Ui Frame");
		int[] bytes = kiss.getDataBytes();
		// First bytes are header
		int[] callbytes = Arrays.copyOfRange(bytes, 0, 7);
		int[] callbytes2 = Arrays.copyOfRange(bytes, 7, 14);
		toCallsign = getcall(callbytes);
		fromCallsign = getcall(callbytes2);
		
		// VIA
		if (bytes[14]==0x00) { // digipeat
			System.out.println("ERROR: Digipeat not supported");
			return;
		} 
		
		controlByte = bytes[14];
		int UI = controlByte & 0x03; // last two bits of control inticate type 01 = I 10 = S 11 = U
		if (UI == 0x03) {
			pid = bytes[15];
			// All but last two are data
			data = Arrays.copyOfRange(bytes, 16, bytes.length-1);
		} else {
			throw new FrameException("ERROR: NON UI frame not supported");
		}
	}
	
	public int[] getBytes() {
		return bytes;
	}
	
	public int[] getDataBytes() {
		return data;
	}
			
	public boolean isBroadcastFileFrame() {
		if (data == null) return false;
		if (toCallsign.startsWith("QST"))
			if (data.length > 15 && (pid & 0xff) == PID_BROADCAST) return true;
		return false;
	}
	
	public boolean isDirectoryBroadcastFrame() {
		if (data == null) return false;
		if (toCallsign.startsWith("QST"))
			if (data.length > 15 && (pid & 0xff) == PID_DIR_BROADCAST) return true;
		return false;
	}
	
	public boolean isStatusFrame() {
		if ((pid & 0xff) == PID_NO_PROTOCOL) {
			if (toCallsign.startsWith(StatusFrame.PBLIST)) return true;
			if (toCallsign.startsWith(StatusFrame.PBFULL)) return true;
			if (toCallsign.startsWith(StatusFrame.PBSHUT)) return true;
			if (toCallsign.startsWith(StatusFrame.BBSTAT)) return true;
			return false;
		} 
		return false;
	}
	
	public boolean isResponseFrame() {
		if ((pid & 0xff) == PID_BROADCAST) {
			if (!toCallsign.startsWith("QST")) return true;
		}
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
	
	public static int[] encodeCall(String callsign) {
		String call = callsign.split("-")[0];
		int ssid=0;
		try {
		ssid = Integer.parseInt(callsign.split("-")[1]);
		} catch (Exception e) {
			// leave it at zero, likely missing dash
		}
		int[] by = new int[7];
		
		for (int i=0; i<6 && i < call.length(); i++)
			by[i] = (call.charAt(i) & 0x7f) << 1;

		by[6] = (ssid << 1) & 0x1E  ;
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
		s = s + "From:" + fromCallsign + " to " + toCallsign;
		s = s + " Ctrl: " + Integer.toHexString(controlByte);
		s = s + " PID: " + Integer.toHexString(pid & 0xff) + " ";
		return s;
	}
	
	public String toString() {
		String s = headerString();
		if (isBroadcastFileFrame()) s = s + "FILE> ";
		if (data != null) {
			s = s + makeString(data);
		}
		return s;
	}
	
	// Test routine

	public static final void main(String[] argc) throws FrameException {
			int[] by = UiFrame.encodeCall("G0KLA-4");
					
			for (int b : by)
				System.out.print((char)b);
			System.out.println("");
			
			String call = UiFrame.getcall(by);
			System.out.println(call);
		}
}
