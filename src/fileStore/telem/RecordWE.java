package fileStore.telem;
import java.io.IOException;
import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

import common.Config;
import common.SpacecraftSettings;

public class RecordWE extends DataRecord {
	SpacecraftSettings spacecraftSettings;
	
	public RecordWE(SpacecraftSettings spacecraftSettings, int id, int resets, long uptime, int type, int[] data) throws LayoutLoadException, IOException {
		super(spacecraftSettings.db.getLayoutByName(SpacecraftSettings.WOD_LAYOUT), id, resets, uptime, type, data);
	}
	
	public String toString() {
		String s = new String();
		s = s + "WE VALUES:" + "\n";
		s = s + super.toString();
		return s;
	}
}
