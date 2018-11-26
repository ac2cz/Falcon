package ax25;

public class Iframe extends Ax25Frame {
	
	public Iframe(String fromCallsign, String toCallsign, int controlByte, int pid, int[] data) {
		super(fromCallsign, toCallsign, controlByte, pid, data);
	}
	
	void setControlByte(int nr, int ns, int p) {
		controlByte = ns << 1;
		controlByte = controlByte & (p << 8); // & 0b100000000 );
		controlByte = controlByte & (nr << 9);
		bytes[14] = controlByte;
	}
	
}
