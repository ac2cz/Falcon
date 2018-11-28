package pacSat.frames;

import ax25.Ax25Frame;
import ax25.Uframe;

public class ULCmdFrame extends PacSatFrame {
	public Uframe uFrame;
	int type;
	
	@Deprecated
	public ULCmdFrame(String fromCall, String toCall, int commandType) {
		frameType = getFrameTypeFromCommand(commandType);
		type = commandType;
		//uFrame = new Uframe(fromCall, toCall, commandType);
	}
	
	public static int getFrameTypeFromCommand(int cmd) {
		switch (cmd) {
		case Ax25Frame.TYPE_U_SABM:
			return TYPE_U_SABM;
		case Ax25Frame.TYPE_UA:
			return TYPE_UA;
		case Ax25Frame.TYPE_U_DISCONNECT:
			return TYPE_U_DISC;
		default:
			return -1;
		}
	}
	
	@Override
	public int[] getBytes() {
		return uFrame.getBytes();
	}

	@Override
	public String toString() {
		String s = uFrame.headerString();
		s = s + "UL CMD: " + Ax25Frame.getTypeString(type);
		return s;
	}
}
