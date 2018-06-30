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
	
	UiFrame uiFrame;
	int[] bytes;
	
	/**
	 * Given a UI Frame of data, decode this into a Status Frame
	 * @param ui
	 */
	public ResponseFrame(UiFrame ui) {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		frameType = PSF_RESPONSE_OK_OTHER;
		String myCall = Config.get(Config.CALLSIGN);
		if (ui.toCallsign.startsWith(myCall)) {
			// This is our response
			String response = UiFrame.makeString(bytes);
			if (response.startsWith("OK"))
				frameType = PSF_RESPONSE_OK;
			else
				frameType = PSF_RESPONSE_ERROR;
		} else {
			// This is a response for someone else
		}
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
				if (UiFrame.isPrintableChar(ch))
					s = s + ch;
				else
					s = s + ".";
			}
		}
		return s;
	}

	

}
