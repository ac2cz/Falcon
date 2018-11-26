package ax25;

public class Uframe extends Ax25Frame {

	public Uframe(String fromCallsign, String toCallsign, int PF, int controlByte) {
		super(fromCallsign, toCallsign, controlByte & (PF << 4));
	}

}
