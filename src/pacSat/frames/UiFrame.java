package pacSat.frames;
import java.util.Arrays;


public class UiFrame {
	public static final int PID_BROADCAST = 0xbb;
	public static final int PID_DIR_BROADCAST = 0xbd;
	
	int[] data;
	int controlByte;
	int pid;
	public String toCallsign;
	public String fromCallsign;
	
	public UiFrame(KissFrame ui) throws FrameException {
		if (ui.length < 17) throw new FrameException("Not enough bytes for a valid Ui Frame");
		int[] bytes = ui.getBytes();
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
		return data;
	}
			
	public boolean isBroadcastFileFrame() {
		if (data == null) return false;
		//if (fromCallsign.startsWith("QST"))
			if (data.length > 15 && (pid & 0xff) == PID_BROADCAST) return true;
		return false;
	}
	
	public boolean isDirectoryBroadcastFrame() {
		if (data == null) return false;
		if (data.length > 15 && (pid & 0xff) == PID_DIR_BROADCAST) return true;
		return false;
	}
	
	private String getcall(int[] framebuf)
	{
	int	i, framebufp = 0, cbpp;
	int	ssid;
	String	cbp = "";

	for (i=0; i<6; i++)		/* start by filling in the basic characters */
	    if (framebuf[framebufp] == 0x40)
	    	;//skip??
	    else
	    	cbp = cbp + (char)((framebuf[i] >> 1) & 0x7F);	/* unshift to ASCII */

	/* tack on the SSID if it isn't zero */
	if ((ssid = (framebuf[framebufp] & 0x1E) >> 1) > 0)
	    cbp = cbp + "-" + ssid;
	else
		cbp = cbp + ' ';

	return cbp;
	}
	
	public static boolean isPrintableChar( char c ) {
	    if (c >=0x20 && c <= 0x7f) return true;
	    return false;
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
			for (int b : data) {
				char ch = (char) b;
				if (isPrintableChar(ch))
					s = s + ch;
				else
					s = s + ".";
			}
		}
		return s;
	}
}
