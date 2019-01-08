	package fileStore.telem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class RecordBL extends DataRecord {
	
	public RecordBL(int id, int resets, long uptime, int type, int[] data) throws LayoutLoadException, IOException {
		super(new ByteArrayLayout("BL", "spacecraft\\BLformat.csv"), id, resets, uptime, type, data);
	}
	
	public String toString() {
		String s = new String();
		Date date = new Date(fieldValue[0]*1000);
		s = s + "BL VALUES: Collected " + date + "\n";
		s = s + super.toString();
		return s;
	}
}
