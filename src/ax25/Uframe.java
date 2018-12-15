package ax25;

public class Uframe extends Ax25Frame {

	public Uframe(String fromCallsign, String toCallsign, int P, int controlByte) {
		super(fromCallsign, toCallsign, controlByte | (P << 4));
		PF = P;
	}

}
