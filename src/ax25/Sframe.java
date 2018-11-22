package ax25;

public class Sframe extends Ax25Frame {

	public Sframe(String fromCallsign, String toCallsign, int controlByte, int[] data) {
		super(fromCallsign, toCallsign, controlByte);
	}

}
