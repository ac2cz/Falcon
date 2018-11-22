package ax25;

public class UIframe extends Ax25Frame {
	
	public UIframe(String fromCallsign, String toCallsign, int controlByte, int pid, int[] data) {
		super(fromCallsign, toCallsign, controlByte, pid, data);
	}
}
