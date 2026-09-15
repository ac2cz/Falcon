package pacSat.frames;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import fileStore.FileHole;
import fileStore.PacSatField;
import fileStore.SortedArrayList;

/**
 * Amsat Pacsat Ground
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * A request frame has a PID of 0xbb for a file or 0xbd for a directory.  It has a destination
 * address of the spacecraft.  If the destination address is QST-1 then it is a broadcast frame
 * and not a request frame.
 *
 */
public class RequestFileFrame extends PacSatFrame {
	Ax25Frame uiFrame;
	int flags;
	public static final int START_SENDING_FILE =    0b00010000;
	public static final int STOP_SENDING_FILE =     0b00010001; 
	public static final int FRAME_IS_HOLE_LIST =    0b00010010;
	public static final int AUTH_BIT = 0b10000000;     // bit 7 reserved-must-be-0, so free
	
	public long fileId;   // all frames with this number belong to the same file
	int blockSize = 244;  // request broadcast use this as max size
	Date authDate; 
	byte[] hashVector;
	int[] data;
	
	// Packet length on sat is limited to 255
	// 3 bytes for KISS format
	// UI frame has 16 byte header with a flag each end and 2 byte CRC - 20 bytes
	// So we are left with 255 -3 - 20 = 232 for data.  This includes the File Req header which is 7 bytes, 
	// so 225 left for holes = max of 225/5 = 45
	public static final int MAX_FILE_HOLES = 45; //47; //244/5 - 1;
	public static final int AUTH_TRAILER_LEN = 36;      // dateTime(4) + HMAC vector(32)
	private static final int HEADER_LEN = 7;            // flags(1) + fileId(4) + blockSize(2)
	public static final int MAX_FILE_HOLES_AUTH = 37;   // (232 - 7 - 36) / 5


	public RequestFileFrame(String fromCall, String toCall, boolean startSending, long file,
	                        SortedArrayList<FileHole> holes) {
	    frameType = PSF_REQ_FILE;
	    fileId = file;
	    data = buildBody(startSending, holes, false);
	    uiFrame = new Ax25Frame(fromCall, toCall, Ax25Frame.TYPE_UI, Ax25Frame.COMMAND,
	                            Ax25Frame.PID_BROADCAST, data);
	}

	public RequestFileFrame(String fromCall, String toCall, boolean startSending, long file,
	                        SortedArrayList<FileHole> holes, byte[] key)
	        throws InvalidKeyException, NoSuchAlgorithmException {
		if (key == null)
			key = new byte[32]; // use empty key if no key provided to support cubesat sim

	    frameType = PSF_REQ_FILE;
	    fileId = file;
	    int[] body = buildBody(startSending, holes, true);

	    long now = PacSatFrame.nextAuthTime();            // shared high-water — see note
	    authDate = new Date(now * 1000);
	    int[] dt = KissFrame.littleEndian4(now);

	    int macLen = body.length + dt.length;             // body + dateTime = signed span
	    data = new int[macLen + 32];
	    int j = 0;
	    for (int i : body) data[j++] = i;
	    for (int i : dt)   data[j++] = i;
	    calcHashVector(key, macLen);
	    for (int i = 0; i < hashVector.length; i++)
	        data[macLen + i] = hashVector[i] & 0xff;

	    uiFrame = new Ax25Frame(fromCall, toCall, Ax25Frame.TYPE_UI, Ax25Frame.COMMAND,
	                            Ax25Frame.PID_BROADCAST, data);
	}

	private int[] buildBody(boolean startSending, SortedArrayList<FileHole> holes, boolean auth) {
	    int[] holedata = null;
	    flags = 0;
	    if (holes != null) {
	        int h = 0;
	        flags = FRAME_IS_HOLE_LIST;
	        int num = Math.min(holes.size(), auth ? MAX_FILE_HOLES_AUTH : MAX_FILE_HOLES);
	        holedata = new int[FileHole.SIZE * num];
	        for (int i = 0; i < num; i++)
	            for (int b : holes.get(i).getBytes()) holedata[h++] = b;
	    }
	    flags = flags | (startSending ? START_SENDING_FILE : STOP_SENDING_FILE);
	    if (auth) flags = flags | AUTH_BIT;          // last, and before header[0] — it's a signed byte

	    int[] header = new int[HEADER_LEN];
	    header[0] = flags;
	    int[] byid = KissFrame.littleEndian4(fileId);
	    header[1] = byid[0]; header[2] = byid[1]; header[3] = byid[2]; header[4] = byid[3];
	    int[] byblock = KissFrame.littleEndian2(blockSize);
	    header[5] = byblock[0]; header[6] = byblock[1];

	    int[] body = new int[header.length + (holedata != null ? holedata.length : 0)];
	    int j = 0;
	    for (int i : header) body[j++] = i;
	    if (holedata != null) for (int i : holedata) body[j++] = i;
	    return body;
	}
	
	private void calcHashVector(byte[] key, int len) throws NoSuchAlgorithmException, InvalidKeyException {
	    byte[] by = new byte[len];
	    for (int i = 0; i < len; i++) by[i] = (byte) data[i];    // offset 0 here, unlike FTL0's data[i+2]
	    SecretKeySpec sks = new SecretKeySpec(key, "HmacSHA256");
	    Mac mac = Mac.getInstance("HmacSHA256");
	    mac.init(sks);
	    hashVector = mac.doFinal(by);
	}	
	
	private int holesRegionEnd() {
	    if ((flags & AUTH_BIT) != 0 && data.length >= HEADER_LEN + AUTH_TRAILER_LEN)
	        return data.length - AUTH_TRAILER_LEN;
	    return data.length;
	}
	
	public int[] getBytes() {
		return uiFrame.getBytes();
	}
	
	public String toString() {
	    String s = "";
	    if (Config.getBoolean(Config.DEBUG_DOWNLINK)) uiFrame.headerString();
	    s = s + "FILE REQ: ";
	    if ((flags & AUTH_BIT) != 0) s = s + "AUTH ";
	    s = s + "FLG: " + Integer.toHexString(flags & 0xff);
	    s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
	    s = s + " BLK_SIZE: " + Long.toHexString(blockSize & 0xffffff);

	    int end = holesRegionEnd();
	    int h = HEADER_LEN, j = 1;
	    while (h + FileHole.SIZE <= end) {
	        int[] by2 = {data[h+0],data[h+1],data[h+2]};
	        long offset = KissFrame.getLongFromBytes(by2);
	        int[] by3 = {data[h+3],data[h+4]};
	        int length = KissFrame.getIntFromBytes(by3);
	        s = s + " Hole " + j + ": " + offset + " " + length;
	        h = h + FileHole.SIZE;
	        j++;
	    }
	    if ((flags & AUTH_BIT) != 0 && data.length >= HEADER_LEN + AUTH_TRAILER_LEN) {
	        int base = data.length - AUTH_TRAILER_LEN;
	        int[] dt = {data[base],data[base+1],data[base+2],data[base+3]};
	        long t = KissFrame.getLongFromBytes(dt);
	        s = s + " AUTH: " + PacSatField.getDateString(new Date(t*1000)) + " [+32 byte vector]";
	    }
	    return s;
	}
	
	/*
	public static final void main(String[] argc) throws FrameException, LayoutLoadException, IOException {
//		int[] bytes = { 2, 39, 3, 0, 0, 16, 172, 6, 0, 84, 243, 32, 116, 32, 208 }; //-84, 6, 0
//		int[] by2 = {bytes[6],bytes[7],bytes[8]};
//		int offLow = KissFrame.getIntFromBytes(by2);
//		long offset = ((bytes[8] & 0xff) << 16) + offLow;
//		
//		System.out.println(((char)bytes[0]) + " " + offLow + " " + offset);
		
//		byte b = (byte) 0xff;
//		int i = b & L_BIT;
//		System.out.println(Integer.toHexString(i));
		Config.init("PacSatGround.properties");
		
		RequestFileFrame req = new RequestFileFrame("G0KLA", "FS-3", true, 0x1234, null);
		System.out.println(req);
		KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, req.getBytes());
		
		KissFrame decode = new KissFrame();
		for (int b : kss.getDataBytes()) {
			decode.add(b);
			System.out.print(Integer.toHexString(b)+ " ");
		}
		System.out.println("");
		Ax25Frame ui = new Ax25Frame(decode);
		System.out.println(ui);
		
		KissFrame decode2 = new KissFrame();
		int[] by = {0xC0,0x00,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF6,0x82,0x86,0x64,0x86,0xB4,0x40,
		0x61,0x03,0xBB,0x10,0x34,0x12,0x00,0x00,0xF4,0x00,0xC0};
		for (int b : by) {
			decode2.add(b);
		}
		Ax25Frame ui2 = new Ax25Frame(decode2);
		System.out.println(ui2);
		//RequestFileFrame req2 = new RequestFileFrame(ui2);
		//System.out.println(req2);
		//Config.close();
	}
	*/
}
