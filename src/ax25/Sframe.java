package ax25;

public class Sframe extends Ax25Frame {

	public Sframe(String fromCallsign, String toCallsign, int controlByte, int command, int[] data) {
		super(fromCallsign, toCallsign, controlByte, command);
	}
	
	public Sframe(String fromCallsign, String toCallsign, int nr, int P, int S, int command) {
		super(fromCallsign, toCallsign, (nr << 5) | (P << 4) | (S << 2) | 0b01, command);
		NR = nr;
		SS = S;
		PF = P;
	}

}
