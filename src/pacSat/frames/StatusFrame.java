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
 * Status frames indicate if the spacecraft is open for business:
 * 
 * OPEN ABCD
 * 
 * OK <callsign>
 * 
 * NO -x <callsign>
 * 
 * PB: <call1> <call2>\D <call3>\D
 * 
 * PB: Empty
 * 
 * Shut: ABCD
 * 
 * B: <byteCount>
 * 
 * 
 * 
 */
public class StatusFrame extends PacSatFrame {
	public static final String BSTAT = "BSTAT";
	public static final String BBSTAT = "BBSTAT";
	public static final String PBLIST = "PBLIST";
	public static final String PBFULL = "PBFULL";
	public static final String PBSHUT = "PBSHUT";
	
	Ax25Frame uiFrame;
	int[] bytes;
	
	/**
	 * Given a UI Frame of data, decode this into a Status Frame
	 * @param ui
	 */
	public StatusFrame(Ax25Frame ui) {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		if (ui.toCallsign.startsWith(PBLIST)) {
			frameType = PSF_STATUS_PBLIST;
		} else if (ui.toCallsign.startsWith(PBFULL)) {
			frameType = PSF_STATUS_PBFULL;
		} else if (ui.toCallsign.startsWith(PBSHUT)) {
			frameType = PSF_STATUS_PBSHUT;
		} else if (ui.toCallsign.startsWith(BBSTAT)) {
			frameType = PSF_STATUS_BBSTAT;
		}else if (ui.toCallsign.startsWith(BSTAT)) {
			frameType = PSF_STATUS_BYTE;
		}
	}

	public boolean containsCall() {
		String list = Ax25Frame.makeString(bytes);
		if (list.contains(Config.get(Config.CALLSIGN)))
			return true;
		return false;
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
