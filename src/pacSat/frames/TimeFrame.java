package pacSat.frames;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ax25.Ax25Frame;
import ax25.KissFrame;

public class TimeFrame extends PacSatFrame  {
	public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	Ax25Frame uiFrame;
	int[] bytes;
	String timeStr = "";
	
	public TimeFrame(Ax25Frame ui) {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		frameType = PSF_TIME;
		if (bytes.length != 4)
			return;
		int[] by = new int[4];
		for (int i=0; i<4; i++)
			by[i] = (int)bytes[i];
		long timemills = KissFrame.getLongFromBytes(by);
		Date t = new Date(timemills*1000);
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		timeStr = dateFormat.format(t) + " UTC";
	}

	@Override
	public int[] getBytes() {
		return bytes;
	}

	@Override
	public String toString() {
		
		return timeStr;
	}
}
