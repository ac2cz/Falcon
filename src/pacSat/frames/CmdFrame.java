package pacSat.frames;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ax25.Ax25Frame;
import ax25.KissFrame;

/**
 * 
 * @author chris
 *
 * All data is little endian
 *
 * Format of a command is:
 * int16 reset number
 * uint24 uptime / 2
 * uint8 address
 * uint8 special
 * uint8 nameSpace
 * uint16 command
 * uint16 arg0
 * uint16 arg1
 * uint16 arg2
 * uint16 arg3
 * uint8[32] authenticationVector
 * 
 * There are 18 bytes of data and 32 bytes for signature
 * 
 */
public class CmdFrame  extends PacSatFrame {
	public static final int CMD_TYPE_RAW_SOFTWARE = 1;
	public static final int SW_CMD_NS_SPACECRAFT_OPS = 1;

	public static final int SW_CMD_OPS_CLEAR_MINMAX = 6;
	public static final int SW_CMD_OPS_ENABLE_PB = 8;
	public static final int SW_CMD_OPS_ENABLE_UPLINK = 12;
	public static final int SW_CMD_OPS_SET_TIME = 14;
	public static final int SW_CMD_OPS_RESET = 21;
	public static final int SW_CMD_ADDRESS = 0x1A;

	
	Ax25Frame uiFrame;
	int reset;
	long uptime;
	int[] data;
	int msgType;
	int nameSpace;
	int cmd;
	int address;
	int special;
	int[] args;

	public CmdFrame(String fromCall, String toCall, int reset, long uptime, int nameSpace, int cmd, int[] args)  {
		this.reset = reset;
		this.uptime = uptime;
		frameType = PSF_COMMAND;

		msgType = CMD_TYPE_RAW_SOFTWARE;
		this.nameSpace = nameSpace;
		this.cmd = cmd;
		this.address = SW_CMD_ADDRESS;
		
		this.args = args;
		makeFrame(fromCall, toCall);
	}
	
	
	private void makeFrame(String fromCall, String toCall) {
		data = new int[18];
		int[] r = KissFrame.littleEndian2(reset);
		data[0] = r[0];
		data[1] = r[1];
		int[] u = KissFrame.littleEndian4(uptime);
		data[2] = u[0];
		data[3] = u[1];
		data[4] = u[2];
		
		data[5] = address;
		data[6] = special;
		data[7] = nameSpace;
		
		int[] c = KissFrame.littleEndian2(cmd);
		data[8] = c[0];
		data[9] = c[1];
		
		for (int i=0; i< args.length; i++) {
			int[] arg = KissFrame.littleEndian2(args[i]);
			data[10+2*i] = arg[0];
			data[10+2*i+1] = arg[1];
		}
		uiFrame = new Ax25Frame(fromCall, toCall, Ax25Frame.TYPE_UI, Ax25Frame.COMMAND, Ax25Frame.PID_COMMAND, data);
	}
	
	@Override
	public int[] getBytes() {
		return uiFrame.getBytes();
	}

	@Override
	public String toString() {
		return uiFrame.toString();
	}
	
	public static void main(String args[]) throws NoSuchAlgorithmException {
		System.out.println("CMD");
		MessageDigest md = MessageDigest.getInstance("SHA-256");

		//byte[] by = new byte[18];
		String s = "The quick brown fox jumps over the lazy dog";
		byte[] by = s.getBytes();
//		 try {
		     md.update(by);
		    byte[] d = md.digest();
		    System.out.println("Digest len:" + md.getDigestLength());
		    for (int i=0; i< md.getDigestLength(); i++) {
		    	System.out.print(" " + Integer.toHexString(d[i] & 0xFF));
		    }
//		 } catch (CloneNotSupportedException cnse) {
//		     throw new DigestException("couldn't make digest of partial content");
//		 }
	}
 }
