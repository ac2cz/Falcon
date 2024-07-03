package pacSat.frames;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ax25.Ax25Frame;
import ax25.Iframe;
import ax25.KissFrame;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

public class ULCmdFrame extends PacSatFrame {
	public Iframe iFrame;
	int type; // A PACSAT EVENT Type
	int[] data;
	SpacecraftSettings spacecraftSettings;
	byte[] hashVector;

	public ULCmdFrame(SpacecraftSettings spacecraftSettings, String fromCall, String toCall, 
			PacSatEvent event, byte[] key) {
		this.spacecraftSettings = spacecraftSettings;
		type = event.type;
		switch (type) {
		case PacSatEvent.UL_REQUEST_AUTH_UPLOAD:
			// 2 byte header and 12 information bytes 32 check bytes
			data = new int[46];
			int length_of_file = (int) event.psf.getActualLength();
			// populate the FTL0 header (2), fileNo (4), filelength (4)
			int length = 44; // number of info bytes

			makeHeader(FTL0Frame.AUTH_UPLOAD_CMD, length);

			// Add date
			Date now = new Date();
			long time = now.getTime()/1000;
			int[] bydt = KissFrame.littleEndian4(time);
			data[2] = bydt[0];
			data[3] = bydt[1];
			data[4] = bydt[2];
			data[5] = bydt[3];
			
			// now the details of the file
			PacSatFile psf = event.psf;
			PacSatFileHeader pfh = psf.getPfh(spacecraftSettings);

			if (pfh.getFileId() != 0) {
				int[] byid = KissFrame.littleEndian4(pfh.getFileId());
				data[6] = byid[0];
				data[7] = byid[1];
				data[8] = byid[2];
				data[9] = byid[3];
			}
			int[] byid = KissFrame.littleEndian4(length_of_file);
			data[10] = byid[0];
			data[11] = byid[1];
			data[12] = byid[2];
			data[13] = byid[3];
			
			try {
				calcHashVector(key, 12);
				for (int i=0; i< 32; i++) {
					data[14 + i] = hashVector[i] & 0xFF;
				}
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case PacSatEvent.UL_AUTH_DATA_END:
			// 2 byte header and 8 information bytes 32 check bytes
			data = new int[42];
			length = 40; // number of info bytes

			makeHeader(FTL0Frame.AUTH_DATA_END, length);
			// Add date
			now = new Date();
			time = now.getTime()/1000;
			bydt = KissFrame.littleEndian4(time);
			data[2] = bydt[0];
			data[3] = bydt[1];
			data[4] = bydt[2];
			data[5] = bydt[3];

			// now the check bytes
			byid = KissFrame.littleEndian2(event.header_check);
			data[6] = byid[0];
			data[7] = byid[1];
			byid = KissFrame.littleEndian2(event.body_check);
			data[8] = byid[0];
			data[9] = byid[1];

			try {
				calcHashVector(key, 8);
				for (int i=0; i< 32; i++) {
					data[10 + i] = hashVector[i] & 0xFF;
				}
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
		iFrame = new Iframe(fromCall, toCall, Ax25Frame.PID_NO_PROTOCOL, data);
	}

	
	public ULCmdFrame(SpacecraftSettings spacecraftSettings, String fromCall, String toCall, PacSatEvent event) {
		this.spacecraftSettings = spacecraftSettings;
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
			PacSatFileHeader pfh = psf.getPfh(spacecraftSettings);
			
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
			makeHeader(FTL0Frame.DATA_END, 0);
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
	
	private void calcHashVector(byte[] key, int len) throws NoSuchAlgorithmException, InvalidKeyException {

		byte[] by = new byte[len];
		for (int i=0; i<len; i++ ) {
			by[i] = (byte)data[i+2]; // offset by 2 past FTL0 header.  Just the data bytes
		}

		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(secretKeySpec);

		hashVector = mac.doFinal(by);

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
		s = s + " UL CMD: " + FTL0Frame.ftl0Types[actualType];
		
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
