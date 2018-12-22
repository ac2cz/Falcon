package pacSat.frames;

import java.io.IOException;

import ax25.Ax25Frame;
import ax25.Iframe;
import ax25.KissFrame;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

public class ULCmdFrame extends PacSatFrame {
	public Iframe iFrame;
	int type; // A PACSAT EVENT Type
	int[] data;
	
	public ULCmdFrame(String fromCall, String toCall, PacSatEvent event) {
		type = event.type;
		int control = 0;
		switch (type) {
		case PacSatEvent.UL_REQUEST_UPLOAD:
			// 2 byte header and 8 information bytes
			data = new int[10];
			int length_of_file = (int) event.psf.getActualLength();
			// populate the FTL0 header (2), fileNo (4), filelength (4)
			int length = 8; // number of info bytes
			
			makeHeader(FTL0Frame.UPLOAD_CMD, length);
			
			// now the details of the file
			PacSatFile psf = event.psf;
			PacSatFileHeader pfh = psf.getPfh();
			
			if (pfh.getFileId() != 0) {
				int[] byid = KissFrame.littleEndian4(pfh.getFileId());
				data[2] = byid[0];
				data[3] = byid[1];
				data[4] = byid[2];
				data[5] = byid[3];
			}
			int[] byid = KissFrame.littleEndian4(length_of_file);
			data[6] = byid[0];
			data[7] = byid[1];
			data[8] = byid[2];
			data[9] = byid[3];
			break;
		case PacSatEvent.UL_DATA:
			data = new int[event.bytes.length+2];
			makeHeader(FTL0Frame.DATA, event.bytes.length);
			int j=2;
			for (int i : event.bytes)
				data[j++] = i;
			break;
		case PacSatEvent.UL_DATA_END:
			data = new int[2];
			makeHeader(FTL0Frame.DATA_END, 2);
		default:
			break;
		}
		iFrame = new Iframe(fromCall, toCall, Ax25Frame.PID_NO_PROTOCOL, data);
	}
	
	ULCmdFrame(Iframe ifrm) {
		iFrame = ifrm;
	}
	
	private void makeHeader(int ftlType, int length) {
		// The 2 header bytes: length_lsb and h1
		data[0] = length & 0xff; // least sig 8 bits of data length
		int msb_length = (length >> 8) & 0b111; // 3 msb of length
		data[1] = (msb_length << 5) | (ftlType & 0b11111);

	}
	
	@Override
	public int[] getBytes() {
		return iFrame.getBytes();
	}

	@Override
	public String toString() {
		String s = iFrame.headerString();
		int[] data = iFrame.getDataBytes();
		int length_lsb = data[0];
		int h1 = data[1];
		int length = length_lsb | (h1 & 0b11100000);
		int actualType = h1 & 0b00011111;
		s = s + " UL CMD: " + FTL0Frame.types[actualType];
		
		switch (actualType) {
		case FTL0Frame.UPLOAD_CMD:
			int[] by = {data[2],data[3],data[4],data[5]};
			long file = KissFrame.getLongFromBytes(by);
			s = s + " File: " + Long.toHexString(file);
			int[] by2 = {data[6],data[7],data[8],data[9]};
			long size = KissFrame.getLongFromBytes(by2);
			s = s + " Size: " + Long.toHexString(size);
			break;
		}
		return s;
	}
	
	public static void main(String[] args) throws MalformedPfhException, IOException {
//		PacSatFile psf = new PacSatFile("FRED.OUT", 0x0000);
//		PacSatFile psf = new PacSatFile("C:\\Users\\chris\\Desktop\\Test\\DEV2\\FalconSat-3\\AC2CZ40.txt.out");
//		PacSatEvent ev = new PacSatEvent(psf);
//		ULCmdFrame ul = new ULCmdFrame("G0KLA ", "PFS-3 ",ev);
//		System.out.println(ul);
//		ul = new ULCmdFrame("G0KLA ", "PFS-3 ",new PacSatEvent(PacSatEvent.UL_DATA_END));
//		System.out.println(ul);
		
		int[] by = {0x8, 0x3, 0xbd, 0x14, 0x0, 0x0, 0xef, 0x6, 0x0, 0x0};
		int[] by2 = {0x2, 0x1};
		Iframe ifrm = new Iframe("From", "To", 0xf0, by);
		ULCmdFrame ftl = new ULCmdFrame(ifrm);
		System.out.println(ftl);

	}
}
