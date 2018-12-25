package fileStore;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class RecordWE extends DataRecord {
	
	public RecordWE(String formatName, int[] data) throws LayoutLoadException, IOException {
		super(formatName, data);
	}
	
	public String toString() {
		String s = new String();
		s = s + "WE VALUES: Collected " + "\n";
		s = s + super.toString();
		return s;
	}
}
