package pacSat.frames;

import ax25.Ax25Frame;
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
 * IORS
 */
public class IorsStatusFrame extends PacSatFrame {
	Ax25Frame uiFrame;
	int[] bytes;
	int iors_mode;
	int pm_mode;
	int channel_a;
	int channel_b;
	int x_band_rpt;
	int T4;
	
	String iors_mode_strs[] = {"SAFE", "CREW", "TELEM", "X_BAND", "APRS", "SSTV", "FS"};
	
	public IorsStatusFrame(Ax25Frame ui) {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		if (bytes.length < 10) return;
		iors_mode = bytes[0];
		pm_mode = bytes[1];
		channel_a = (bytes[2] + (bytes[3] << 8));
		channel_b = (bytes[4] + (bytes[5] << 8));
		x_band_rpt = bytes[6];
		T4 = (bytes[7] + (bytes[8] << 8));

	}

	@Override
	public int[] getBytes() {
		return bytes;
	}

	@Override
	public String toString() {
		String s = "";
		//if (Config.getBoolean(Config.DEBUG_DOWNLINK))
//			s = s + uiFrame.headerString() + " ";
		s = s + "STATUS: Mode:" + iors_mode_strs[iors_mode];
		s = s + " PM:" + pm_mode;
		s = s + " CH-A:" + channel_a;
		s = s + " CH-B:" + channel_b;
		s = s + " RPT:" + x_band_rpt;
		s = s + " T4:" + T4;
		return s;
	}


}
