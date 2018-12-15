package pacSat.frames;

import java.io.File;

import ax25.Ax25Frame;
import ax25.Iframe;
import ax25.KissFrame;
import fileStore.PacSatFile;

public class ULCmdFrame extends PacSatFrame {
	public Iframe iFrame;
	int type; // A PACSAT EVENT Type
	
	public ULCmdFrame(String fromCall, String toCall, PacSatEvent event) {
		type = event.type;
		int control = 0;
		int[] data = null;
		switch (type) {
		case PacSatEvent.UL_REQUEST_UPLOAD:
			data = new int[10];
			int length_of_file = (int) event.psf.getActualLength();
			// populate the FTL0 header (2), fileNo (4), filelength (4)
			int length = 8; // number of info bytes
			data[0] = length & 0xff; // least sig 8 bits of data length
			int msb_length = (length >> 8) & 0x11; // 3 msb of length
			data[1] = (msb_length << 5) | (FTL0Frame.UPLOAD_CMD & 0xf);
			if (event.continueFileNumber != 0) {
				int[] byid = KissFrame.littleEndian4(event.continueFileNumber);
				data[2] = byid[0];
				data[2] = byid[1];
				data[3] = byid[2];
				data[4] = byid[3];
			}
			int[] byid = KissFrame.littleEndian4(length_of_file);
			data[5] = byid[0];
			data[6] = byid[1];
			data[7] = byid[2];
			data[8] = byid[3];
			break;
		case PacSatEvent.UL_DATA:
			data = new int[event.bytes.length+2];
			// populate the FTL0 header (2), data (x)
			data[0] = event.bytes.length & 0xff; // least sig 8 bits of data length
			msb_length = (event.bytes.length >> 8) & 0x11; // 3 msb of length
			data[1] = (msb_length << 5) | (FTL0Frame.DATA & 0xf);
			int j=2;
			for (int i : event.bytes)
				data[j++] = i;
			break;
		case PacSatEvent.UL_DATA_END:
			data = new int[2];
			// populate the FTL0 header (2)
			data[0] = 0 & 0xff; // least sig 8 bits of data length
			msb_length = (0 >> 8) & 0x11; // 3 msb of length
			data[1] = (msb_length << 5) | (FTL0Frame.DATA_END & 0xf);
		default:
			break;
		}
		iFrame = new Iframe(fromCall, toCall, Ax25Frame.PID_NO_PROTOCOL, data);
	}
	
	@Override
	public int[] getBytes() {
		return iFrame.getBytes();
	}

	@Override
	public String toString() {
		String s = iFrame.headerString();
		s = s + "UL CMD: " + PacSatEvent.types[type];
		return s;
	}
	
	public static void main(String[] args) {
		PacSatFile psf = new PacSatFile("FRED.OUT", 0x0000);
		PacSatEvent ev = new PacSatEvent(psf);
		ULCmdFrame ul = new ULCmdFrame("G0KLA ", "PFS-3 ",ev);
		System.out.print(ul);
	}
}
