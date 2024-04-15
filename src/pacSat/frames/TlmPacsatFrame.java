package pacSat.frames;

import java.io.IOException;
import java.util.Date;

import com.g0kla.telem.data.BitArrayLayout;
import com.g0kla.telem.data.BitDataRecord;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;

public class TlmPacsatFrame extends PacSatFrame {
	Ax25Frame uiFrame;
	Date startDate;
	int reset;
	long uptime;
	int type;
	int[] bytes;
	int[] data;
	public DataRecord record;
	SpacecraftSettings spacecraftSettings;
	BitArrayLayout layout;
	
	public TlmPacsatFrame(SpacecraftSettings spacecraftSettings, Ax25Frame ui) throws MalformedPfhException, LayoutLoadException, IOException {
		this.spacecraftSettings = spacecraftSettings;
		frameType = PacSatFrame.PSF_TLM_PACSAT;
		uiFrame = ui;
		bytes = ui.getDataBytes();
		int[] by = {bytes[5],bytes[4]};
//		reset = KissFrame.getIntFromBytes(by);
		reset = 0;
//		int[] by2 = {bytes[3],bytes[2],bytes[1],bytes[0]};
		int[] by2 = {bytes[0],bytes[1],bytes[2],bytes[3]};
		uptime = KissFrame.getLongFromBytes(by2);

		String name = SpacecraftSettings.TLMI_LAYOUT;
		if (ui.toCallsign.startsWith("TLMP1")) {
			name = SpacecraftSettings.TLMI_LAYOUT;
			type = 1;
		} else {
			throw new LayoutLoadException("Invalid telemetry frame destination type: "+ui.toCallsign +".  Ignored.");
		}
		layout = (BitArrayLayout) spacecraftSettings.db.getLayoutByName(name);
		
		if (bytes.length > layout.getMaxNumberOfBytes())
			throw new LayoutLoadException("Too many bytes in Pacsat telemetry frame.  Ignored.");
		
		record = new BitDataRecord(layout, 0, reset, uptime, type, bytes, BitDataRecord.LITTLE_ENDIAN);

	}
	
	public DataRecord getTlm() throws LayoutLoadException, IOException {		
		record = new BitDataRecord(layout, 0, reset, uptime, 0, bytes, BitDataRecord.LITTLE_ENDIAN);

		return record;
	}
	
	@Override
	public int[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		String s = "";
		if (Config.getBoolean(Config.DEBUG_DOWNLINK))
			s = s +uiFrame.headerString();

		s = s + "PACSAT: " + uiFrame.toCallsign + " Type:" + frameType + " ";
		if (Config.getBoolean(Config.DEBUG_TELEM)) {
			try {
				record = getTlm();
				if (record != null)
					s = s + "\n" + record.toString();
				else
					s = s + " - Corrupted CRC .. ignored ";
			} catch (LayoutLoadException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		return s;
	}

}
