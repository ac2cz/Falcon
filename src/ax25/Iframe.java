package ax25;

public class Iframe extends Ax25Frame {
	
	public Iframe(String fromCallsign, String toCallsign, int controlByte, int pid, int[] data) {
		super(fromCallsign, toCallsign, controlByte, pid, data);
	}
}
