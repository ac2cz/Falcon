package fileStore.telem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.ConversionTable;
import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.segDb.SatTelemStore;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;

class LogFileWEFullTest {

	@Test
	void test() throws MalformedPfhException, IOException, LayoutLoadException {
		Config.init("PacSatGround.properties");
		File current = new File(System.getProperty("user.dir"));
		Config.currentDir = current.getAbsolutePath();
		String logFileDir = "C:\\Users\\chris\\Desktop\\Test\\FS-3-TEST";
		Config.logDirFromPassedParam = true;
		File log = new File(logFileDir);
		Config.set(Config.LOGFILE_DIR, logFileDir);
		Config.homeDir = log.getAbsolutePath();
		
		Log.init("PacSatGround");
		Config.simpleStart();
		SpacecraftSettings spacecraftSettings = Config.getSatSettingsByName("FalconSat-3");

		LogFileWE bl = new LogFileWE(spacecraftSettings, "C:\\Users\\chris\\Google Drive\\AMSAT\\FalconSat-3\\Full WOD\\21a-we071300");
		ConversionTable ct = new ConversionTable("C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\Fs3coef.csv");

		System.out.println(bl);

//		for (DataRecord d3 : bl.records) {
//			d3.layout.setConversionTable(ct);
//			System.out.println(d3);
//		}
	}
	

}
