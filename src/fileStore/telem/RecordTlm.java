package fileStore.telem;

import java.io.IOException;
import java.util.Date;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.ConversionTable;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class RecordTlm extends DataRecord {

	public RecordTlm(String name, String formatName, int id, int resets, long uptime, int type, int[] data) throws LayoutLoadException, IOException {
		super(new ByteArrayLayout(name, formatName), id, resets, uptime, type, data);
	}
	
	public String toString() {
		String s = new String();
		s = s + "Telemetry: "+ "\n";
		//s = s + super.toString();

		ConversionTable ct = layout.getConversionTable(); 
		for (int i=0; i < layout.fieldName.length; i++) {
			if (ct != null)
				s = s + layout.shortName[i] + ": " + ct.getStringValue(layout.conversion[i], fieldValue[i]) + ",   ";
			else
				s = s + layout.shortName[i] + ": " + fieldValue[i] + ",   ";
			if ((i+1)%6 == 0) s = s + "\n";
		}
		return s;
	}
}
