package ax25;

public class Sframe extends Ax25Frame {

	public Sframe(String fromCallsign, String toCallsign, int controlByte, int[] data) {
		super(fromCallsign, toCallsign, controlByte);
	}
	
	public Sframe(String fromCallsign, String toCallsign, int nr, int P, int S) {
		super(fromCallsign, toCallsign, (nr << 5) | (P << 4) | (S << 2) | 0b01);
		NR = nr;
		SS = S;
		PF = P;
	}

}
