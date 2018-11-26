package ax25;

public class Ax25Request extends Ax25Primitive {
	public static final int TIMER_T1_EXPIRY = 0;
	public static final int TIMER_T3_EXPIRY = 1;
	public static final int DL_CONNECT = 2;
	public static final int DL_DISCONNECT = 3;
	public static final int DL_DATA = 4;
	public static final int DL_POP_IFRAME = 5;
	
	public static final String types[] = {
		"TIMER T1 EXPIRE",
		"TIMER T3 EXPIRE",
		"DL_CONNECT",
		"DL_DISCONNECT",
		"DL_DATA",
		"DL_POP_IFRAME"
	};
	
	int type;
	String fromCall;
	String toCall;
	public Iframe iFrame;

	// Simple event
	public Ax25Request(int type) {
		this.type = type;
	}
	
	// Connection Request
	public Ax25Request(String fromCall, String toCall) {
		this.type = DL_CONNECT;
		this.fromCall = fromCall;
		this.toCall = toCall;
	}
	
	// DL Data
	public Ax25Request(Iframe iFrame) {
		this.type = DL_DATA;
		this.iFrame = iFrame;
	}
	
	public String toString() {
		String s = "";
		s = s + "Type: " + types[type];
		return s;
	}
}
