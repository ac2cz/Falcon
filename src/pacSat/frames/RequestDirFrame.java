package pacSat.frames;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import common.Config;
import common.LayoutLoadException;
import common.Spacecraft;
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
public class RequestDirFrame extends PacSatFrame {
	UiFrame uiFrame;
	int flags;
	public static final int START_SENDING_DIR =    0b00010000;
	public static final int STOP_SENDING_DIR =     0b00010001; 
	public static final int FRAME_IS_HOLE_LIST =    0b00010010;
	
	long fileId;   // all frames with this number belong to the same file
	int blockSize = 244;  // request broadcast use this as max size

	int[] data;
	
	public RequestDirFrame(String fromCall, String toCall, boolean startSending, ArrayList<FileHole> holes) {
		frameType = PSF_REQ_DIR;
		int[] holedata = null;
		flags = 0;
		//if (holes != null) {
			flags = FRAME_IS_HOLE_LIST;
			//holedata = new int[8*holes.size()];
			holedata = new int[8];
			// here we would populate the hole from to dates
			Date now = new Date();
			int[] fromDate = KissFrame.littleEndian4(now.getTime()/1000 - 2*60*60); // go back 2 hours
			int[] toDate = KissFrame.littleEndian4(now.getTime()/1000);
			int j=0;
			for (int i : fromDate)
				holedata[j++] = i;
			for (int i : toDate)
				holedata[j++] = i;
		//}
		if (startSending)
			flags = flags | START_SENDING_DIR;
		else
			flags = flags | STOP_SENDING_DIR;
		
		fileId = 0L; // no file id for DIR Request
		
		int[] header = new int[7]; // fixed header size
		header[0] = flags;
		// bytes 1,2,3,4 are zero - the file id
		int[] byblock = KissFrame.littleEndian2(blockSize);
		header[5] = byblock[0];
		header[6] = byblock[1];
		
		int j1 = 0;
		if (holedata != null)
			data = new int[header.length + holedata.length];
		else
			data = new int[header.length];
		for (int i : header)
			data[j1++] = i;
		if (holedata != null)
			for (int i : holedata)
				data[j1++] = i;
		
		uiFrame = new UiFrame(fromCall, toCall, UiFrame.PID_DIR_BROADCAST, data);
	}
	
	public int[] getBytes() {
		return uiFrame.getBytes();
	}
	
	public String toString() {
		String s = uiFrame.headerString();
		s = s + "FLG: " + Integer.toHexString(flags & 0xff);
		s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
		s = s + " BLK_SIZE: " + Long.toHexString(blockSize & 0xffffff);
		s = s + " HOLE:";
		int[] by = {data[7],data[8],data[9],data[10]};
		int[] by2 = {data[11],data[12],data[13],data[14]};
		long frm = KissFrame.getLongFromBytes(by);
		long to = KissFrame.getLongFromBytes(by2);
		Date frmDate = new Date(frm*1000);
		Date toDate = new Date(to*1000);
		s = s + frmDate + " " + toDate;
		return s;
	}
	
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
		
		Config.init();
		RequestDirFrame req = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, null);

//		RequestDirFrame req = new RequestDirFrame("G0KLA", "UOSAT-11", true, null);
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
		Config.close();
	}
}
