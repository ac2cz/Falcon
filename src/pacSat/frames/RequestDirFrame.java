package pacSat.frames;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import common.Config;
import common.LayoutLoadException;
import common.Spacecraft;
import fileStore.DirHole;
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
public class RequestDirFrame extends PacSatFrame {
	Ax25Frame uiFrame;
	int flags;
	public static final int DIR_FILL_REQUEST =    0b00010000;
	
	
	public static final int MAX_DIR_HOLES = 29; //29; //244/8 - 1;
	
	long fileId;   // all frames with this number belong to the same file
	int blockSize = 244;  // request broadcast use this as max size

	Date frmDate;
	Date toDate;
	int[] data;
	
	public RequestDirFrame(String fromCall, String toCall, boolean startSending, SortedArrayList<DirHole> holes)  {
		frameType = PSF_REQ_DIR;
		int[] holedata = null;
		flags = 0;
		
		// process the holes to make the holedata
		if (holes != null) {
			int h = 0;
			int holesAdded = 0;
			flags = DIR_FILL_REQUEST;
			if (holes.size() > MAX_DIR_HOLES)
				holedata = new int[DirHole.SIZE*MAX_DIR_HOLES];
			else
				holedata = new int[DirHole.SIZE*holes.size()];
			for (DirHole hole : holes) {
				holesAdded++;
				if (holesAdded > MAX_DIR_HOLES)
					break;
				int[] hole_by = hole.getBytes();
				for (int b : hole_by) {
					holedata[h++] = b;
				}
			}
		}
		makeFrame(fromCall, toCall, startSending, holedata);
	}
	
//	public RequestDirFrame(String fromCall, String toCall, boolean startSending, Date startDate) {
//		frameType = PSF_REQ_DIR;
//		flags = 0;
//		frmDate = startDate;
//		
//		int[] holedata = null;	
//		int[] toBy = {0xff,0xff,0xff,0x7f}; // end of time, well 2038.. This is the max date for a 32 bit in Unix Timestamp
//		long to = KissFrame.getLongFromBytes(toBy);
//		toDate = new Date(to*1000);
//		DirHole hole = new DirHole(frmDate,toDate);
//		holedata = hole.getBytes();
//		
//		makeFrame(fromCall, toCall, startSending, holedata);
//	}
	
	private void makeFrame(String fromCall, String toCall, boolean startSending, int[] holedata) {
//		if (startSending)
//			flags = flags | START_SENDING_DIR;
//		else
//			flags = flags | STOP_SENDING_DIR;

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
		uiFrame = new Ax25Frame(fromCall, toCall, Ax25Frame.PID_DIR_BROADCAST, data);
	}
	
	public RequestDirFrame(Ax25Frame ui) {
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
		s = s + " BLK_SIZE: " + Long.toHexString(blockSize & 0xffffff);
		int h = 3;
		int j = 1;
		while (data.length > h) {
			int[] by2 = {data[h+0],data[h+1],data[h+2],data[h+3]};
			long frm = KissFrame.getLongFromBytes(by2);
			int[] by3 = {data[h+4],data[h+5],data[h+6],data[h+7]};
			long to = KissFrame.getLongFromBytes(by3);
			Date fDate = new Date(frm*1000);
			Date tDate = new Date(to*1000);
			s = s + "\n Hole " + j + ": " + PacSatField.getDateString(fDate) + " " + PacSatField.getDateString(tDate);
			h = h + 8;
			j++;
		}
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
		Config.load();
//		RequestDirFrame req = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, frmDate);

		SortedArrayList<DirHole> holes = new SortedArrayList<DirHole>();
		Date frmDate, toDate;
		int[] toBy = {0xff,0xff,0xff,0x7f}; // end of time, well 2038.. This is the max date for a 32 bit in Unix Timestamp
		long to = KissFrame.getLongFromBytes(toBy);
		toDate = new Date(to*1000);
		frmDate = new Date(0); //  the begining of time
		DirHole hole = new DirHole(frmDate,toDate);
		holes.add(hole); // initialize with one hole that is maximum length 
		
		RequestDirFrame req = new RequestDirFrame("G0KLA", "UOSAT-11", true, holes);
		System.out.println(req);
		KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, req.getBytes());
		
		KissFrame decode = new KissFrame();
		for (int b : kss.bytes) {
			decode.add(b);
			System.out.print(Integer.toHexString(b)+ " ");
		}
		System.out.println("");
		Ax25Frame ui = new Ax25Frame(decode);
		System.out.println(ui);
		RequestDirFrame req2 = new RequestDirFrame(ui);
		System.out.println(req2);
		
		///////////////
//		KissFrame decode2 = new KissFrame();
//		int by[] = {0xC0,0x00,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF6,0x82,0x86,0x64,0x86,
//		0xB4,0x40,0x61,0x03,0xBD,0x10,0xF4,0x00,0xCF,0x26,0x3C,0x5B,0xFF,0xFF,0xFF,0x7F,0xC0};
//		for (int b : by) {
//			decode2.add(b);
//		}
//		Ax25Frame ui2 = new Ax25Frame(decode2);
//		System.out.println(ui2);
//		RequestDirFrame req2 = new RequestDirFrame(ui2);
//		System.out.println(req2);
		
	}
}
