package ax25;

public class Uframe extends Ax25Frame {

	public Uframe(String fromCallsign, String toCallsign, int P, int controlByte, int command) {
		super(fromCallsign, toCallsign, controlByte | (P << 4), command);
		PF = P;
	}

}
