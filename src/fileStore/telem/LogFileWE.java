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
import fileStore.MalformedPfhException;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.segDb.SatTelemStore;

public class LogFileWE {
	String fileName;
	public ArrayList<RecordWE> records; // The telemetry records, once extracted
	int[] data;

	public LogFileWE(String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		this.fileName = fileName;
		data = loadData();
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
		long dt = DataRecord.getLongValue(i, data);
		Date startDate = new Date(dt*1000);
		System.out.print(startDate);
		i = i + 8;
		int interval = DataRecord.getIntValue(i, data);
		System.out.print(" Interval: "+ interval);
		i = i + 2;
		int channels = data[i];	
		System.out.println(" Channels: "+ channels);
		i++;
		// Read the channels
		i = i+channels;		
		int len = channels*2; // length of a record
		records = new ArrayList<RecordWE>();
		while (i < data.length) {
			int[] dataSet = Arrays.copyOfRange(data, i, len+i);
			RecordWE we = new RecordWE(0, 0, dt+r*interval, 0, dataSet);
			records.add(we);
			i = i + len;
			r++;
		}

	}
	
	public static void main(String[] args) throws LayoutLoadException, IOException, MalformedPfhException, NumberFormatException, DataLoadException {
		Config.init();
		Log.init("PacSatGround");
		ByteArrayLayout[] layouts = new ByteArrayLayout[3];
		layouts[0] = new ByteArrayLayout("WOD", "C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\WEformat.csv");
		layouts[1] = new ByteArrayLayout("TLM", "C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\TLMIformat.csv");
		layouts[2] = new ByteArrayLayout("TLM2", "C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\TLM2format.csv");
		SatTelemStore db = new SatTelemStore(99, "TLMDB", layouts);

		DataRecord d = new DataRecord(layouts[2], "0,0,1522039581,0,1956,2725,1963,2481,2228,515,258,31,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2034,2046,1048,1215,1217,1193,1241,1212,1207,1202,1190,1170,1211,1201,0,0,0,0,0,0,0");
		db.add(d);
		DataRecord d2 = new DataRecord(layouts[2], "0,0,1522039581,0,1956,2725,1963,2481,2228,515,258,31,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2034,2046,1048,1215,1217,1193,1241,1212,1207,1202,1190,1170,1211,1201,0,0,0,0,0,0,0");
		db.add(d2);
		
		
		LogFileWE bl = new LogFileWE("C:\\Users\\chris\\Google Drive\\AMSAT\\FalconSat-3\\telem\\we010310");
		for (DataRecord d3 : bl.records) {
			db.add(d3);
		}
	}


//	public static void main(String[] args) throws MalformedPfhException, IOException, LayoutLoadException {
//		Config.init();
//		Log.init("PacSatGround");
//
//		LogFileWE bl = new LogFileWE("C:\\Users\\chris\\Google Drive\\AMSAT\\FalconSat-3\\telem\\we010310");
//		int raw = 1353;
//		
//	}
}
