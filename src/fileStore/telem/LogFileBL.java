package fileStore.telem;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import common.Config;
import common.Log;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;

import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.data.Tools;

public class LogFileBL extends PacSatFile {

	int[] data;
	
	/**
	 * Given the directory path and an ID,load this log file
	 * @param dir
	 * @param id
	 * @throws LayoutLoadException 
	 * @throws IOException 
	 */
	public LogFileBL(String dir, long id) throws LayoutLoadException, IOException {
		super(dir, id);
		data = getData();
		parseFile();
	}
	
	public LogFileBL(String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		super(fileName);
		data = getData();
		parseFile();
	}

	private void parseFile() throws LayoutLoadException, IOException {
		int i=0; // position in the data

		while (i < data.length) {
			long timeStamp = 0;
			int len = DataRecord.getIntValue(i, data);
			System.out.print("Length: "+ len + " ");
			i+=2;
			int[] dataSet = Arrays.copyOfRange(data, i, len+i);
			RecordBL bl = new RecordBL(0, 0, timeStamp, 0, dataSet);
			i = i + len;
			System.out.println(bl);
		}
		
	}
	
	
	public static void main(String[] args) throws MalformedPfhException, IOException, LayoutLoadException {
		Config.load();
		Log.init("PacSatGround");
		Config.init("PacSatGround.properties");
		LogFileBL bl = new LogFileBL("C:\\Users\\chris\\Desktop\\Test\\FS-3-telem-test\\FalconSat-3\\b33.act");
		Config.close();
	}
}
