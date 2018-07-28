package pacSat.frames;

import java.io.IOException;
import java.util.ArrayList;

import common.Config;
import common.LayoutLoadException;
import common.Spacecraft;
import fileStore.FileHole;
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
	
	public long fileId;   // all frames with this number belong to the same file
	int blockSize = 244;  // request broadcast use this as max size

	int[] data;
	
	public static final int MAX_FILE_HOLES = 10; //47; //244/5 - 1;
	
	public RequestFileFrame(String fromCall, String toCall, boolean startSending, long file, SortedArrayList<FileHole> holes) {
		frameType = PSF_REQ_FILE;
		int[] holedata = null;
		fileId = file;
		flags = 0;
		if (holes != null) {
			int h = 0;
			int holesAdded = 0;
			flags = FRAME_IS_HOLE_LIST;
			holedata = new int[FileHole.SIZE*holes.size()];
			for (FileHole hole : holes) {
				int[] hole_by = hole.getBytes();
				for (int b : hole_by) {
					holedata[h++] = b;
				}
			}
		}
		if (startSending)
			flags = flags | START_SENDING_FILE;
		else
			flags = flags | STOP_SENDING_FILE;
		
		int[] header = new int[7]; // fixed header size
		header[0] = flags;
		// bytes 1,2,3,4 are the file id
		int[] byid = KissFrame.littleEndian4(fileId);
		header[1] = byid[0];
		header[2] = byid[1];
		header[3] = byid[2];
		header[4] = byid[3];
		
		int[] byblock = KissFrame.littleEndian2(blockSize);
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
		
		uiFrame = new Ax25Frame(fromCall, toCall, Ax25Frame.PID_BROADCAST, data);
	}
	
	public int[] getBytes() {
		return uiFrame.getBytes();
	}
	
	public String toString() {
		String s = uiFrame.headerString();
		s = s + "FLG: " + Integer.toHexString(flags & 0xff);
		s = s + " FILE: " + Long.toHexString(fileId & 0xffffffff);
		s = s + " BLK_SIZE: " + Long.toHexString(blockSize & 0xffffff);
		
		int h = 7;
		int j = 1;
		while (data.length > h) {
			int[] by2 = {data[h+0],data[h+1],data[h+2]};
			long offset = KissFrame.getLongFromBytes(by2);
			int[] by3 = {data[h+3],data[h+4]};
			int length = KissFrame.getIntFromBytes(by3);
			s = s + " Hole " + j + ": " + offset + " " + length;
			h = h + 5;
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
		Config.init();
		
		RequestFileFrame req = new RequestFileFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, 0x1234, null);
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
		Config.close();
	}
}
