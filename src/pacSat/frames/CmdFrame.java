package pacSat.frames;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import common.Log;
import common.SpacecraftSettings;

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
	public static final int SW_CMD_OPS_FORMAT_FS = 9;
	public static final int SW_CMD_OPS_ENABLE_UPLINK = 12;
	public static final int SW_CMD_OPS_SET_TIME = 14;
	public static final int SW_CMD_OPS_RESET = 21;
	public static final int SW_CMD_ADDRESS = 0x1A;
	
	public static final int SW_CMD_STOP_SENDING = 99999;
	
	Ax25Frame uiFrame;
	int reset;
	long uptime;
	int[] data;
	byte[] hashVector;
	int msgType;
	int nameSpace;
	int cmd;
	int address;
	int special;
	int[] args;
	boolean useResetUptime = false;

	public CmdFrame(String fromCall, String toCall, long dateTime, int nameSpace, int cmd, int[] args, byte[] key)  {
		this.uptime = dateTime;
		useResetUptime = false;
		frameType = PSF_COMMAND;
		msgType = CMD_TYPE_RAW_SOFTWARE;
		this.nameSpace = nameSpace;
		this.cmd = cmd;
		this.address = SW_CMD_ADDRESS;
		this.args = new int[4];
		for (int i=0; i<4; i++)
		    this.args[i] = args[i];
		makeFrame(fromCall, toCall, key);
	}
	
	public CmdFrame(String fromCall, String toCall, int reset, long uptime, int nameSpace, int cmd, int[] args, byte[] key)  {
		this.reset = reset;
		this.uptime = uptime;
		useResetUptime = true;
		frameType = PSF_COMMAND;

		msgType = CMD_TYPE_RAW_SOFTWARE;
		this.nameSpace = nameSpace;
		this.cmd = cmd;
		this.address = SW_CMD_ADDRESS;
		this.args = new int[4];
		for (int i=0; i<4; i++)
		    this.args[i] = args[i];
		makeFrame(fromCall, toCall, key);
	}
	
	public CmdFrame() {
		frameType = PSF_COMMAND_STOP;
		msgType = CMD_TYPE_RAW_SOFTWARE;
		cmd = SW_CMD_STOP_SENDING;
	}
	
	private void makeFrame(String fromCall, String toCall, byte[] key) {
		data = new int[18+32];
		if (useResetUptime) {
					int[] r = KissFrame.littleEndian2(reset);
					data[0] = r[0];
					data[1] = r[1];
			int[] u = KissFrame.littleEndian4(uptime);
			data[2] = u[0];
			data[3] = u[1];
			data[4] = u[2];
		} else {
			int[] u = KissFrame.littleEndian4(uptime);
			data[0] = u[0];
			data[1] = u[1];
			data[2] = u[2];
			data[3] = u[3];
		}
		
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
		
		try {
			calcHashVector(key);
			for (int i=0; i< 32; i++) {
				data[18 + i] = hashVector[i] & 0xFF;
			}
			uiFrame = new Ax25Frame(fromCall, toCall, Ax25Frame.TYPE_UI, Ax25Frame.COMMAND, Ax25Frame.PID_COMMAND, data);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void calcHashVector(byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {

		byte[] by = new byte[18];
		for (int i=0; i<18; i++ ) {
			by[i] = (byte)data[i];
		}

		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(secretKeySpec);

		hashVector = mac.doFinal(by);

	}
	
	@Override
	public int[] getBytes() {
		return uiFrame.getBytes();
	}

	@Override
	public String toString() {
		if (uiFrame == null)
			return "CmdStop";
		String s = "";
		for (int i=0; i<uiFrame.getBytes().length; i++) {
			s = s+Integer.toHexString(uiFrame.getBytes()[i]) + " ";
		}
		return s;
		//return uiFrame.toString();
	}
	
	public static void main(String args[]) throws NoSuchAlgorithmException {
		System.out.println("CMD");

		int[] arg1 = {1,0,0,0};

		String encodedKey = "ABCDEFG";
		System.out.println("KEY: " + encodedKey);
		
		 byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
		 for (int j=0; j< decodedKey.length; j++) {
		    	System.out.print(" " + Integer.toHexString(decodedKey[j] & 0xFF));
		    }
		 System.out.println();

			CmdFrame cmdFrame = new CmdFrame("VE2TCP", "VE2TCP-11",
					0xABCD, 0xDEADBE, CmdFrame.SW_CMD_NS_SPACECRAFT_OPS, CmdFrame.SW_CMD_OPS_ENABLE_PB, arg1, decodedKey);
		 
		byte[] by = new byte[18];
		for (int i=0; i<18; i++ ) {
			by[i] = (byte)cmdFrame.data[i];
		}
		
		 SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, "HmacSHA256");
		    Mac mac = Mac.getInstance("HmacSHA256");
		    try {
				mac.init(secretKeySpec);
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    byte[] d;
		    d = mac.doFinal(by);
		    
		
		    System.out.println("HASH:" + mac.getMacLength());
		    for (int j=0; j< mac.getMacLength(); j++) {
		    	System.out.print(" " + Integer.toHexString(d[j] & 0xFF));
		    }
		
		
//		MessageDigest md = MessageDigest.getInstance("SHA-256");
//
//		//byte[] by = new byte[18];
//		String s = "The quick brown fox jumps over the lazy dog";
//		byte[] by = s.getBytes();
////		 try {
//		     md.update(by);
//		    byte[] d = md.digest();
//		    System.out.println("Digest len:" + md.getDigestLength());
//		    for (int i=0; i< md.getDigestLength(); i++) {
//		    	System.out.print(" " + Integer.toHexString(d[i] & 0xFF));
//		    }
////		 } catch (CloneNotSupportedException cnse) {
////		     throw new DigestException("couldn't make digest of partial content");
////		 }
	}
 }
