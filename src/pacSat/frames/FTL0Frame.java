package pacSat.frames;

import java.util.Arrays;
import java.util.Date;

import common.Config;

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
 * Response frame gives confirmation or error.  Spacecraft sends in response to request
 * 
 */
public class FTL0Frame extends PacSatFrame {
	
	UiFrame uiFrame;
	int[] bytes;
	int length;
	int ftl0Type;
	int[] data;
	
	String[] ftlTypes = {
		"DATA",
		"DATA_END",
		"LOGIN",
		"UPLOAD_CMD",
		"UL_GO_RESP",
		"UP_ERROR_RESP",
		"UL_ACK_RESP",
		"UL_NAK_RESP",
		"DOWNLOAD_CMD",
		"DL_ERROR_RESP",
		"DL_ABORTED_RESP",
		"DL_COMPLETED_RESP",
		"DL_ACK_CMD",
		"DL_NAK_CMD",
		"DIR_SHORT_CMD",
		"DIR_LONG_CMD",
		"SELECT_CMD",
		"SELECRT_RESP"
		
	};
	
	public static final int DATA = 0;
	public static final int DATA_END = 1;
	public static final int LOGIN = 2;
	public static final int UPLOAD_CMD = 3;
	public static final int UL_GO_RESP = 4;
	
	long loginDate;
	Date dateLogin;
	int loginFlags;
	long fileId;
	long offset;
	
	/**
	 * Given a UI Frame of data, decode this into a Status Frame
	 * @param ui
	 * @throws FrameException 
	 */
	public FTL0Frame(UiFrame ui) throws FrameException {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		length = (bytes[1] >> 5) * 256  + bytes[0];
		ftl0Type = bytes[1] & 0b00011111;
		if (length > 0 && length < bytes.length-1)
			data = Arrays.copyOfRange(bytes, 2, 2 + length);

		switch (ftl0Type) {
		case LOGIN:
			if (data == null) throw new FrameException("FTL0 Login has no data");
			int[] by = {data[0],data[1],data[2],data[3]};
			loginDate = KissFrame.getLongFromBytes(by);
			dateLogin = new Date(loginDate*1000);
			loginFlags = data[4];
			break;
		case UL_GO_RESP:
			if (data == null) throw new FrameException("FTL0 UL GO RESP has no data");
			int[] by2 = {data[0],data[1],data[2],data[3]};
			fileId = KissFrame.getLongFromBytes(by2);
			int[] by3 = {data[4],data[5],data[6],data[7]};
			offset = KissFrame.getLongFromBytes(by3);
			break;
		default:
			break;
		}
	}

	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	
	public String toString() {
		String s = "";
		switch (ftl0Type) {
		case LOGIN:
			s = s + "SUCCESSFUL LOGIN to " + uiFrame.fromCallsign + " by " + uiFrame.toCallsign + " at " + dateLogin.toString();  // from /to seem reversed because this is a message from the spacecraft
			break;
		case UL_GO_RESP:
			s = s + "Ready to receive file: " + fileId + " from " + uiFrame.toCallsign;
			break;
		}
		return s;
	}

	

}
