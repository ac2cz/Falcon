package pacSat.frames;

public class PacSatEvent extends PacSatFrame {
	public static final int UL_CONNECT = 0;
	
	public static final String types[] = {
			"UL_CONNECT"
		};
	
	int eventType;
	
	PacSatEvent(int type) {
		eventType = type;
	}
	
	@Override
	public int[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		String s = "";
		s = s + types[eventType];
		return s;
	}

}
