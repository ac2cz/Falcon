package fileStore.telem;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;
import fileStore.PacSatField;

import com.g0kla.telem.data.BitArrayLayout;
import com.g0kla.telem.data.BitDataRecord;
import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class LogFileTlm {
	String fileName;

	public ArrayList<DataRecord> records; // The telemetry records, once extracted
	int[] data;
	SpacecraftSettings spacecraftSettings;

	public LogFileTlm(SpacecraftSettings spacecraftSettings, String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		this.spacecraftSettings = spacecraftSettings;
		this.fileName = fileName;
		data = loadData();
		parseFile();
	}
	
	public LogFileTlm(SpacecraftSettings spacecraftSettings, int[] bytes ) throws MalformedPfhException, IOException, LayoutLoadException {
		this.spacecraftSettings = spacecraftSettings;
		data = bytes;
		parseFile();
	}
	
	int[] loadData() {
		File f = new File(fileName);
		int[] b = new int[(int) f.length()];
		RandomAccessFile fileOnDisk = null;
		int p = 0;
		try {
			fileOnDisk = new RandomAccessFile(fileName, "r"); // opens file
			boolean readingBytes = true;
			while (readingBytes) {
				try {
					b[p++] = fileOnDisk.readUnsignedByte();
				} catch (EOFException e) {
					readingBytes = false;
				} catch (ArrayIndexOutOfBoundsException e) {
					// This means the datalength was wrong.  We have too much data
					Log.errorDialog("ERROR", "File: "+ fileName + "\nseems to have too many data bytes or the data is corrupt");
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try { if (fileOnDisk != null) fileOnDisk.close(); } catch (IOException e) { }
		}

		return b;
	}

	private void parseFile() throws LayoutLoadException, IOException {
		int i=0; // position in the data
		int r=0; // record we are adding
		String layout = SpacecraftSettings.WOD_LAYOUT; 
		BitArrayLayout lay = (BitArrayLayout) spacecraftSettings.spacecraft.getLayoutByName(layout);
		int len = lay.getMaxNumberOfBytes();
		records = new ArrayList<DataRecord>();
		int type = 2; // layout 2
		while (i < data.length) {
			int[] dataSet = Arrays.copyOfRange(data, i, len+i);
			long timestamp = DataRecord.getLongValue(i, data);
			DataRecord we = new BitDataRecord(lay, 0, 0, timestamp, type, dataSet, BitDataRecord.LITTLE_ENDIAN);

			//DataRecord we = new DataRecord(lay, 0, 0, timestamp, 0, dataSet);
			records.add(we);
			i = i + len;
			r++;
		}
	}
	
	public String toString() {
		String s = "";
		int i = 0;
		for (DataRecord d : records) {
			s = s + PacSatField.getDateStringSecs(new Date(d.uptime*1000)) + ", ";
			s = s + d.toString() + "\n";
		}
		return s;
	}
}
