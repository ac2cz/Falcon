package pacSat;
import java.util.Arrays;


public class UiFrame {
	int[] data;
	int controlByte;
	int pid;
	int crc;
	String call;
	String call2;
	
	public UiFrame(KissFrame ui) throws FrameException {
		if (ui.length < 17) throw new FrameException("Not enough bytes for a valid Ui Frame");
		int[] bytes = ui.getBytes();
		// First bytes are header
		int[] callbytes = Arrays.copyOfRange(bytes, 0, 7);
		int[] callbytes2 = Arrays.copyOfRange(bytes, 7, 14);
		call = getcall(callbytes);
		call2 = getcall(callbytes2);
		
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
			data = Arrays.copyOfRange(bytes, 16, bytes.length);

			// Last two are the CRC check for UI
			int[] by = {bytes[bytes.length-2],bytes[bytes.length-1]};
			crc = KissFrame.getIntFromBytes(by);
		} else {
			throw new FrameException("ERROR: NON UI frame not supported");
		}
	}
	
	public int[] getBytes() {
		return data;
	}
			
	public boolean isBroadcastFrame() {
		if (data == null) return false;
		if (data.length > 15 && (pid & 0xff) == 0xbb) return true;
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
	
	public boolean isPrintableChar( char c ) {
	    if (c >=0x20 && c <= 0x7f) return true;
	    return false;
	}

	public String headerString() {
		String s = "";
		s = s + "From:" + call2 + " to " + call;
		s = s + " Ctrl: " + Integer.toHexString(controlByte);
		s = s + " PID: " + Integer.toHexString(pid & 0xff) + " ";
		return s;
	}
	
	public String toString() {
		String s = headerString();
		if (isBroadcastFrame()) s = s + "BROADCAST> ";
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
