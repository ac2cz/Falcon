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

import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class LogFileWE {
	String fileName;
	long startDate;
	int interval;
	int channels;
	public ArrayList<DataRecord> records; // The telemetry records, once extracted
	int[] data;

	public LogFileWE(String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		this.fileName = fileName;
		data = loadData();
		parseFile();
	}
	
	public LogFileWE(int[] bytes ) throws MalformedPfhException, IOException, LayoutLoadException {
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
		
		// Read the header
		startDate = DataRecord.getLongValue(i, data);
		//System.out.print(startDate);
		i = i + 8;
		interval = DataRecord.getIntValue(i, data);
		//System.out.print(" Interval: "+ interval);
		i = i + 2;
		channels = data[i];	
		//System.out.println(" Channels: "+ channels);
		i++;
		// Read the channels
		i = i+channels;		
		int len = channels*2; // length of a record
		records = new ArrayList<DataRecord>();
		while (i < data.length) {
			int[] dataSet = Arrays.copyOfRange(data, i, len+i);
			DataRecord we = new DataRecord(Config.spacecraft.getLayoutByName(SpacecraftSettings.WOD_LAYOUT), 0, 0, startDate+r*interval, 0, dataSet);
			records.add(we);
			i = i + len;
			r++;
		}

	}
	
	public String toString() {
		String s = "Start Date: " + PacSatField.getDateStringSecs(new Date(startDate*1000));
		s = s + "\nInterval: " + interval + "s";
		s = s + "\nChannels: " + channels + "\n\n";
		s = s + "Date, " + records.get(0).toHeaderString() + "\n";
		int i = 0;
		for (DataRecord d : records) {
			s = s + PacSatField.getDateStringSecs(new Date(d.uptime*1000)) + ", ";
			s = s + d.toString() + "\n";
		}
		return s;
	}
}
