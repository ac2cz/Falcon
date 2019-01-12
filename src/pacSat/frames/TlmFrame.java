package pacSat.frames;

import java.io.IOException;
import java.util.Date;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import common.Spacecraft;
import fileStore.MalformedPfhException;
import fileStore.telem.RecordTlm;

public class TlmFrame extends PacSatFrame {
	Ax25Frame uiFrame;
	Date startDate;
	long timeStamp;
	int[] bytes;
	int[] data;
	public DataRecord record;
	
	public TlmFrame(Ax25Frame ui) throws MalformedPfhException, LayoutLoadException, IOException {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		int[] by = {bytes[0],bytes[1],bytes[2],bytes[3]};
		timeStamp = KissFrame.getLongFromBytes(by);
		startDate = new Date(timeStamp*1000);
		
		// format is channel, then 2 bytes per channel.
		if (uiFrame.toCallsign.startsWith("TLMI-1")) {
			data = new int[72*2];
		} else {
			data = new int[63*2];
		}
		int j = 0;
		int p = 0;
		while (p < data.length) {
			data[p] = bytes[j+5]; // 4 is the channel number, 5 is the first data byte
			data[p+1] = bytes[j+6];
			j=j+3;
			p=p+2;
		}
		getTlm();
	}
	
	public DataRecord getTlm() throws LayoutLoadException, IOException {
		// TODO get this from the spacecraft and not hard code
		String name = Spacecraft.TLM2_LAYOUT;
		if (uiFrame.toCallsign.startsWith("TLMI-1")) {
			name = Spacecraft.TLMI_LAYOUT;
		}
		ByteArrayLayout layout = Config.db.getLayoutByName(name);
		record = new RecordTlm(layout, 0, 0, timeStamp, 0, data);
		return record;
	}
	
	@Override
	public int[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		String s = uiFrame.headerString();

		s = s + "TLM: " + startDate;
		if (Config.getBoolean(Config.DEBUG_TELEM))
		try {
			s = s + "\n" + getTlm().toString();
		} catch (LayoutLoadException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}

}
