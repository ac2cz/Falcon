package pacSat.frames;

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
public class ResponseFrame extends PacSatFrame {
	
	Ax25Frame uiFrame;
	int[] bytes;
	int errorCode = 0;
	
	public static final int FILE_MISSING = -2;	
	public static final int PACKET_FORMAT_ERROR = -5;
	
	/**
	 * Given a UI Frame of data, decode this into a Status Frame
	 * @param ui
	 */
	public ResponseFrame(Ax25Frame ui) {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		frameType = PSF_RESPONSE_OK_OTHER;
		String myCall = Config.get(Config.CALLSIGN);
		if (ui.toCallsign.startsWith(myCall)) {
			// This is our response
			String response = Ax25Frame.makeString(bytes);
			if (response.startsWith("OK"))
				frameType = PSF_RESPONSE_OK;
			else {
				frameType = PSF_RESPONSE_ERROR;
				// Format is "NO -x"
				String[] reply = response.split(" ");
				try {
					errorCode = Integer.parseInt(reply[1]);
				} catch (NumberFormatException e) {
					
				}
			}
		} else {
			// This is a response for someone else
		}
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	@Override
	public int[] getBytes() {
		return bytes;
	}
	
	
	public String toString() {
		String s = "";
		if (bytes != null) {
			for (int b : bytes) {
				char ch = (char) b;
				if (Ax25Frame.isPrintableChar(ch))
					s = s + ch;
				else
					s = s + ".";
			}
		}
		return s;
	}

	

}
