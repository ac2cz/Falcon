package pacSat.frames;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import com.g0kla.telem.data.BitArrayLayout;
import com.g0kla.telem.data.BitDataRecord;
import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;
import fileStore.PacSatFileHeader;
import fileStore.telem.RecordTlm;
import pacSat.CRC;
import pacSat.Crc16;

public class TlmMirSatFrame extends PacSatFrame {
	Ax25Frame uiFrame;
	Date startDate;
	long timeStamp;
	int[] bytes;
	int[] data;
	public DataRecord record;
	SpacecraftSettings spacecraftSettings;
	BitArrayLayout layout;
	public static final int MAX_BYTES_FRAME1 = 225;
	public static final int MAX_BYTES_FRAME2 = 137;
	
	int headerLength = 6; // This disagrees with the SPEC!!
	int preAmble = 9;
	
	// MirSat header fields
	int packetType;
	int APID;
	int sequenceCount;
	int length;
	int serviceType;
	int serviceSubType;
	
	/**
	 * This creates the item and adds data from the first frame
	 * This has a 6 byte header, 9 bytes of pre-amble and 225 bytes of data
	 * The Second frame will have 8 bytes of header and 137 bytes of data.
	 * 
	 * We reserve 362 data bytes
	 * 
	 * @param spacecraftSettings
	 * @param ui
	 * @throws MalformedPfhException
	 * @throws LayoutLoadException
	 * @throws IOException
	 */
	public TlmMirSatFrame(SpacecraftSettings spacecraftSettings, Ax25Frame ui) throws MalformedPfhException, LayoutLoadException, IOException {
		this.spacecraftSettings = spacecraftSettings;
		
		Date dt = new Date();
		timeStamp = dt.getTime() / 1000; // timestamp this with current time in Unix format
		
		String name = SpacecraftSettings.TLMI_LAYOUT;
		layout = (BitArrayLayout) spacecraftSettings.db.getLayoutByName(name);
		
		frameType = PacSatFrame.PSF_TLM_MIR_SAT_1;
		uiFrame = ui;
		bytes = ui.getDataBytes();
		
		packetType = bytes[6] & 0xff;
		APID = bytes[7] & 0xff;
		
		
		serviceType = bytes[13] & 0xff;
		serviceSubType = bytes[14] & 0xff;	
		
		int headerLength = 6; // This disagrees with the SPEC!!
		int preAmble = 9;
		
		headerLength = headerLength + preAmble;
		
		data = new int[layout.getMaxNumberOfBytes()];
		int p = headerLength;
		while (p < bytes.length ) {
			data[p - (headerLength)] = bytes[p]; 
			p++;
		}
		record = null;  // we are waiting for the second half
	}
	
	public void add2ndFrame(Ax25Frame ui) {
		uiFrame = ui;
		bytes = ui.getDataBytes();
		
		int headerLength = 6; 
		int p = headerLength;
		while (p < headerLength+MAX_BYTES_FRAME2 ) { 
			data[MAX_BYTES_FRAME1 + p - headerLength] = bytes[p]; 
			p++;
		}
	}
	
	public DataRecord getTlm() throws LayoutLoadException, IOException {
		record = new BitDataRecord(layout, 0, 0, timeStamp, 0, data, BitDataRecord.BIG_ENDIAN);
		
		/*
		// Check the CRC
		long check = record.getRawValue("CheckSumBytes_x2");
		short crc = (short) (check & 0xFFFF);
		System.err.println("CRC:" + Integer.toHexString(crc));
		
		// Grab all the bytes after the header except Body Checksum itself
	//	byte[] checkBytes = new byte[this.data.length-2];

		System.err.println("Calc ... ");
		for (int start = 0; start < data.length; start++)
			for (int end = start+1; end < data.length; end++) {
				byte[] checkBytes = new byte[end-start];
				for (int i=start; i< end; i++)
					checkBytes[i-start] = (byte) (data[i] & 0xFF);

				//checkBytes = Arrays.copyOfRange(data, 0, layout.getMaxNumberOfBytes()-2);

				short calc_crc = PacSatFileHeader.checksum(checkBytes);
				short calc_crc2 = (short) CRC.calculateCRC(CRC.Parameters.CCITT, checkBytes);
				//if (!Crc16.goodCrc(data)) {
				//	System.err.println("BAD CRC");
					//throw new MalformedPfhException("Bad CRC for TLM ");
				//}
				if (crc == calc_crc || crc == calc_crc2) 
					System.err.println(start + " : " + end + " gives calculated CRC:" + Integer.toHexString(calc_crc) + " or " + Integer.toHexString(calc_crc2));
				//	if (crc != calc_crc) return null; // likely this has a first and second half from different beacons or was corrupted
			}
		System.err.println(" ... ");
		*/
		return record;
	}

	@Override
	public int[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		String s = "";
		if (Config.getBoolean(Config.DEBUG_DOWNLINK))
			s = s +uiFrame.headerString();

		s = s + "" + uiFrame.toCallsign + " Type:" + packetType + " APID:" + APID + " Service:" + serviceType + ":" + serviceSubType + " ";
		//if (Config.getBoolean(Config.DEBUG_TELEM)) {
			try {
				record = getTlm();
				if (record != null)
					s = s + "\n" + record.toString();
				else
					s = s + " - Corrupted CRC .. ignored ";
			} catch (LayoutLoadException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
//		} else {
//			for (int b : data)
//				s = s + Integer.toHexString(b) + " ";
//		}
		
		return s;
	}
	
}
