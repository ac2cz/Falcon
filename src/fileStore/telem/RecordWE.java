package fileStore.telem;
import java.io.IOException;
import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class RecordWE extends DataRecord {
	
	public RecordWE(int id, int resets, long uptime, int type, int[] data) throws LayoutLoadException, IOException {
		super(new ByteArrayLayout("WOD", "spacecraft\\WEformat.csv"), id, resets, uptime, type, data);
	}
	
	public String toString() {
		String s = new String();
		s = s + "WE VALUES:" + "\n";
		s = s + super.toString();
		return s;
	}
}
