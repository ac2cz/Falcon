package pacSat.frames;

import java.util.ArrayList;

import fileStore.FileHole;

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
	UiFrame uiFrame;
	int flags;
	public static final int START_SENDING_FILE =    0b00010000;
	public static final int STOP_SENDING_FILE =     0b00010001; 
	public static final int FRAME_IS_HOLE_LIST =    0b00010010;
	
	long fileId;   // all frames with this number belong to the same file
	int blockSize = 244;  // request broadcast use this as max size

	int[] data;
	
	public RequestFileFrame(String fromCall, String toCall, boolean startSending, long file, ArrayList<FileHole> holes) {
		frameType = PSF_REQ_FILE;
		int[] holedata = null;
		fileId = file;
		flags = 0;
		if (holes != null) {
			flags = FRAME_IS_HOLE_LIST;
			holedata = new int[5*holes.size()];
			// here we would populate the hole list
		}
		if (startSending)
			flags = flags | START_SENDING_FILE;
		else
			flags = flags | STOP_SENDING_FILE;
		
		int[] header = new int[7]; // fixed header size
		header[0] = flags;
		// bytes 1,2,3,4 are the file id
		int[] byid = KissFrame.bigEndian4(fileId);
		header[1] = byid[0];
		header[2] = byid[1];
		header[3] = byid[2];
		header[4] = byid[3];
		
		int[] byblock = KissFrame.bigEndian2(blockSize);
		header[5] = byblock[0];
		header[6] = byblock[1];
		
		int j = 0;
		if (holedata != null)
			data = new int[header.length + holedata.length];
		else
			data = new int[header.length];
		for (int i : header)
			data[j++] = i;
		if (holedata != null)
			for (int i : holedata)
				data[j++] = i;
		
		uiFrame = new UiFrame(fromCall, toCall, UiFrame.PID_BROADCAST, data);
	}
	
	public int[] getBytes() {
		return uiFrame.getBytes();
	}
	
	public String toString() {
		String s = uiFrame.headerString();
		s = s + "FLG: " + Integer.toHexString(flags & 0xff);
		s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
		s = s + " BLK_SIZE: " + Long.toHexString(blockSize & 0xffffff);
		return s;
	}
	
	public static final void main(String[] argc) throws FrameException {
//		int[] bytes = { 2, 39, 3, 0, 0, 16, 172, 6, 0, 84, 243, 32, 116, 32, 208 }; //-84, 6, 0
//		int[] by2 = {bytes[6],bytes[7],bytes[8]};
//		int offLow = KissFrame.getIntFromBytes(by2);
//		long offset = ((bytes[8] & 0xff) << 16) + offLow;
//		
//		System.out.println(((char)bytes[0]) + " " + offLow + " " + offset);
		
//		byte b = (byte) 0xff;
//		int i = b & L_BIT;
//		System.out.println(Integer.toHexString(i));
		
		RequestFileFrame req = new RequestFileFrame("G0KLA", "UOSAT-11", false, 0xDEAD, null);
		System.out.println(req);
		KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, req.getBytes());
		
		KissFrame decode = new KissFrame();
		for (int b : kss.bytes) {
			decode.add(b);
			System.out.print(Integer.toHexString(b)+ " ");
		}
		System.out.println("");
		UiFrame ui = new UiFrame(decode);
		System.out.println(ui);
	}
}
