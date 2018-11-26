package ax25;

public class Sframe extends Ax25Frame {

	public Sframe(String fromCallsign, String toCallsign, int controlByte, int[] data) {
		super(fromCallsign, toCallsign, controlByte);
	}
	
	public Sframe(String fromCallsign, String toCallsign, int NR, int P, int S) {
		super(fromCallsign, toCallsign, (NR << 5) & (P << 4) & (S << 2) & 0b01);
	}

}
