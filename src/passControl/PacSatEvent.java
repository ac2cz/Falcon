package passControl;

import pacSat.frames.PacSatFrame;

public class PacSatEvent {

	public static final int EVENT_USER_REQ_DL = 0;
	public static final int EVENT_USER_REQ_DIR = 1;
	public static final int EVENT_USER_REQ_SELECT = 2;
	public static final int EVENT_USER_REQ_ABORT = 3;
	public static final int EVENT_DL_ABORTED_RESP = 4;
	public static final int EVENT_DL_COMPLETED_RESP = 5;
	public static final int EVENT_SELECT_RESP = 6;
	public static final int EVENT_LOGIN_RESP = 7;
	public static final int EVENT_DATA = 8;
	public static final int EVENT_DATA_END = 9;
	public static final int EVENT_DL_ERROR_RESP = 10;

	public static final String[] events = {
		"User Req Download",
		"User Req Directory",
		"User Req Select",
		"User Req Abort",
		"Download Aborted",
		"Download Complete",
		"Select",
		"Login",
		"Data",
		"Data End",
		"Download Error"
	};
	
	int eventId;
	PacSatFrame pacSatFrame;
	
	public PacSatEvent(int eventId) {
		this.eventId = eventId;
	}
	
	public PacSatEvent(int eventId, PacSatFrame pacSatFrame) {
		this.eventId = eventId;
		this.pacSatFrame = pacSatFrame;
	}
	
	public int getEventId() {
		return eventId;
	}
	
	public String toString() {
		String s = "";
		s = s + events[eventId];
		
		return s;
	}
	
}
