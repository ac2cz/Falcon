package pacSat.frames;

import java.util.Arrays;
import java.util.Date;

import ax25.Ax25Frame;
import ax25.KissFrame;
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
	
	Ax25Frame iFrame;
	int[] bytes;
	int length;
	int ftl0Type;
	int[] data;
	
	String[] ftl0Types = {
		"DATA",       // 0
		"DATA_END",
		"LOGIN",
		"UPLOAD_CMD",
		"UL_GO_RESP",
		"UP_ERROR_RESP",
		"UL_ACK_RESP",
		"UL_NAK_RESP",   // 7
		"DOWNLOAD_CMD",
		"DL_ERROR_RESP",
		"DL_ABORTED_RESP",
		"DL_COMPLETED_RESP",
		"DL_ACK_CMD",
		"DL_NAK_CMD",
		"DIR_SHORT_CMD",
		"DIR_LONG_CMD",
		"SELECT_CMD",
		"SELECRT_RESP"	//17
	};
	
	String[] ftl0Errors = {
			"UNDEFINED", //0
			"ER_ILL_FORMED_CMD",
			"ER_BAD_CONTINUE",
			"ER_SERVER_FSYS",
			"ER_NO_SUCH_FILE_NUMBER",
			"ER_SELECTION_EMPTY",
			"ER_MANDATORY_FIELD_MISSING",
			"ER_NO_PFH",
			"ER_POORLY_FORMED_SEL",
			"ER_ALREADY_LOCKED",
			"ER_NO_SUCH_DESTINATION",  // 10
			"ER_SELECTION_EMPTY",
			"ER_FILE_COMPLETE",
			"ER_NO_ROOM",
			"ER_BAD_HEADER",
			"ER_HEADER_CHECK",
			"ER_BODY_CHECK"
		};
	
	public static final int DATA = 0;
	public static final int DATA_END = 1;
	public static final int LOGIN = 2;
	public static final int UPLOAD_CMD = 3;
	public static final int UL_GO_RESP = 4;
	public static final int UP_ERROR_RESP = 5;
	public static final int UL_ACK_RESP = 6;
	public static final int UL_NAK_RESP = 7;
	
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
	public FTL0Frame(Ax25Frame i) throws FrameException {
		iFrame = i;
		bytes = i.getDataBytes();
		length = (bytes[1] >> 5) * 256  + bytes[0];
		ftl0Type = bytes[1] & 0b00011111;
		if (length > 0 && length < bytes.length-1)
			data = Arrays.copyOfRange(bytes, 2, 2 + length);

		switch (ftl0Type) {
		case LOGIN:
			frameType = PSF_LOGIN_RESP;
			if (data == null) throw new FrameException("FTL0 Login has no data");
			int[] by = {data[0],data[1],data[2],data[3]};
			loginDate = KissFrame.getLongFromBytes(by);
			dateLogin = new Date(loginDate*1000);
			loginFlags = data[4];
			break;
		case UL_GO_RESP:
			frameType = PSF_UL_GO_RESP;
			if (data == null) throw new FrameException("FTL0 UL GO RESP has no data");
			int[] by2 = {data[0],data[1],data[2],data[3]};
			fileId = KissFrame.getLongFromBytes(by2);
			int[] by3 = {data[4],data[5],data[6],data[7]};
			offset = KissFrame.getLongFromBytes(by3);
			break;
		case UL_ACK_RESP:
			frameType = PSF_UL_ACK_RESP;
			break;
		case UL_NAK_RESP:
			frameType = PSF_UL_NAK_RESP;
			break;
		default:
			break;
		}
	}

	public boolean sentToCallsign(String callsign) {
		if (iFrame.toCallsign.startsWith(callsign) )
			return true;
		return false;
	}
	
	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	public long getFileId() { return fileId; }
	public long getContinuationOffset() { return offset; }
	
	public String toString() {
		String s = "";

		switch (ftl0Type) {
		case LOGIN:
			s = s + "SUCCESSFUL LOGIN to " + iFrame.fromCallsign + " by " + iFrame.toCallsign; // + " at " + dateLogin.toString();  // from /to seem reversed because this is a message from the spacecraft
			break;
		case UL_GO_RESP:
			s = s + "Ready to receive file: " + Long.toHexString(fileId) + " from " + iFrame.toCallsign;
			break;
		default:
			s = s + iFrame.headerString() + " ";
			if (iFrame.isIFrame()) {
				s = s + "PID:" + Integer.toHexString(iFrame.pid) + " ";
			}
			if (ftl0Type > 0 && ftl0Type < ftl0Types.length)
				s = s + ftl0Types[ftl0Type] + " ";
			else
				s = s + ftl0Type + " ";
		}
		return s;
	}

	

}
