package pacSat.frames;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import common.Config;
import common.LayoutLoadException;
import common.Spacecraft;
import fileStore.DirHole;
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

	Date frmDate;
	Date toDate;
	int[] data;
	
	public RequestDirFrame(String fromCall, String toCall, boolean startSending, ArrayList<FileHole> holes) throws FrameException {
		if (holes == null) throw new FrameException("Hole ist is null");
		frameType = PSF_REQ_DIR;
		int[] holedata = new int[DirHole.SIZE*holes.size()];
		flags = 0;
		
	}
	public RequestDirFrame(String fromCall, String toCall, boolean startSending, Date startDate) {
		frameType = PSF_REQ_DIR;
		flags = 0;
		int[] holedata = null;			
		frmDate = startDate;
	
		int[] toBy = {0xff,0xff,0xff,0x7f}; // end of time, well 2038.. Used because WISP uses it
		long to = KissFrame.getLongFromBytes(toBy);
		toDate = new Date(to*1000);
		DirHole hole = new DirHole(frmDate,toDate);
		holedata = hole.getBytes();

		if (startSending)
			flags = flags | START_SENDING_DIR;
		else
			flags = flags | STOP_SENDING_DIR;

		fileId = 0L; // no file id for DIR Request

		int[] header = new int[3]; // fixed header size
		header[0] = flags;
		int[] byblock = KissFrame.littleEndian2(blockSize);
		header[1] = byblock[0];
		header[2] = byblock[1];
		
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

		// Test for just 1 hole
		uiFrame = new UiFrame(fromCall, toCall, UiFrame.PID_DIR_BROADCAST, data);
	}
	
	public RequestDirFrame(UiFrame ui) {
		uiFrame = ui;
		data = ui.getDataBytes();
		flags = data[0];
		//int[] by = {data[1],data[2],data[3],data[4]};
		//fileId = KissFrame.getLongFromBytes(by);
			
		int[] by = {data[1],data[2]};
		blockSize = KissFrame.getIntFromBytes(by);
		
		int[] by2 = {data[3],data[4],data[5],data[6]};
		int[] by3 = {data[7],data[8],data[9],data[10]};
		long frm = KissFrame.getLongFromBytes(by2);
		long to = KissFrame.getLongFromBytes(by3);
		frmDate = new Date(frm*1000);
		toDate = new Date(to*1000);


	}
	
	public int[] getBytes() {
		return uiFrame.getBytes();
	}
	
	public String toString() {
		String s = uiFrame.headerString();
		s = s + "FLG: " + Integer.toHexString(flags & 0xff);
		s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
		s = s + " BLK_SIZE: " + Long.toHexString(blockSize & 0xffffff);
		s = s + " First HOLE: ";
		s = s + frmDate + " - " + toDate;
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
		
		
		/////////////////////
		Config.init();
		Date frmDate = new Date();
		RequestDirFrame req = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, frmDate);

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
		
		///////////////
		KissFrame decode2 = new KissFrame();
		int by[] = {0xC0,0x00,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF6,0x82,0x86,0x64,0x86,
		0xB4,0x40,0x61,0x03,0xBD,0x10,0xF4,0x00,0xCF,0x26,0x3C,0x5B,0xFF,0xFF,0xFF,0x7F,0xC0};
		for (int b : by) {
			decode2.add(b);
		}
		UiFrame ui2 = new UiFrame(decode2);
		System.out.println(ui2);
		RequestDirFrame req2 = new RequestDirFrame(ui2);
		System.out.println(req2);
		
	}
}
