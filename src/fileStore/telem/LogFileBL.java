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
	String content = "";
	
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
		content = parseFile();
	}
	
	public LogFileBL(String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		super(fileName);
		data = getData();
		content = parseFile();
	}

	private String parseFile() throws LayoutLoadException, IOException {
		int i=0; // position in the data
		String s = "";
		while (i < data.length) {
			long timeStamp = 0;
			int len = DataRecord.getIntValue(i, data);
//			System.out.print("Length: "+ len + " ");
			i+=2;
			int[] dataSet = Arrays.copyOfRange(data, i, len+i);
			RecordBL bl = new RecordBL(0, 0, timeStamp, 0, dataSet);
			i = i + len;
			s = s + bl;
		}
		return s;
	}
	
	public String toString() {
		return content;
	}
	
	public static void main(String[] args) throws MalformedPfhException, IOException, LayoutLoadException {
		Config.init("PacSatGround.properties");
		Log.init("PacSatGround");
		
		LogFileBL bl = new LogFileBL("C:\\dos\\BL2788.act");
		System.out.println(bl);
	}
}
