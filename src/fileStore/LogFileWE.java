package fileStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import ax25.KissFrame;
import common.Config;
import common.Log;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class LogFileWE extends PacSatFile {

	int[] data;
	
	/**
	 * Given the directory path and an ID,load this log file
	 * @param dir
	 * @param id
	 * @throws LayoutLoadException 
	 * @throws IOException 
	 */
	public LogFileWE(String dir, long id) throws LayoutLoadException, IOException {
		super(dir, id);
		data = getData();
		parseFile();
	}
	
	public LogFileWE(String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		super(fileName);
		data = getData();
		parseFile();
	}

	private void parseFile() throws LayoutLoadException, IOException {
		int i=0; // position in the data

		while (i < data.length) {
			int len = DataRecord.getIntValue(i, data);
			System.out.print("Len: "+ len);
			i+=2;
			int[] dataSet = Arrays.copyOfRange(data, i, len+i);
			RecordBL bl = new RecordBL("WEformat.csv", dataSet);
			i = i + len;
			System.out.println(bl);
		}
		
	}
	
	public static void main(String[] args) throws MalformedPfhException, IOException, LayoutLoadException {
		Config.load();
		Log.init("PacSatGround");
		Config.init();
		LogFileWE bl = new LogFileWE("C:\\Users\\chris\\Desktop\\Test\\FS-3-telem-test\\FalconSat-3\\b91.act");
		Config.close();
	}
}
