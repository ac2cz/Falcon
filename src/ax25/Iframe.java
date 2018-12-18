package ax25;

public class Iframe extends Ax25Frame {
	
	public Iframe(String fromCallsign, String toCallsign, int pid, int[] data) {
		super(fromCallsign, toCallsign, 0, Ax25Frame.COMMAND, pid, data);
	}
	
	public Iframe(String fromCallsign, String toCallsign, int nr, int ns, int P) {
		super(fromCallsign, toCallsign, Ax25Frame.COMMAND, (nr << 5) | (P << 4) | (ns << 1) | 0b00);
		NR = nr;
		NS = ns;
		PF = P;
	}
	void setControlByte(int nr, int ns, int p) {
		controlByte = (nr << 5) | (p << 4) | (ns << 1) | 0b00;
		bytes[14] = controlByte;
		NR = nr;
		NS = ns;
		PF = p;
	}
	
}
