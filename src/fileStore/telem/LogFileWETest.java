package fileStore.telem;


import java.io.File;
import java.io.IOException;

import org.junit.Test;

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

class LogFileWETest {

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

		LogFileWE bl = new LogFileWE(spacecraftSettings, "C:\\Users\\chris\\Google Drive\\AMSAT\\FalconSat-3\\telem\\we010310");
		ConversionTable ct = new ConversionTable("C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\Fs3coef.csv");
		int raw = 1353;
		for (DataRecord d3 : bl.records) {
			d3.layout.setConversionTable(ct);
			System.out.println(d3);
		}
	}
	
	@Test
	void test2() throws MalformedPfhException, IOException, LayoutLoadException, DataLoadException {
		Config.init("PacSatGround.properties");
		Log.init("PacSatGround");
		SpacecraftSettings spacecraftSettings = Config.getSatSettingsByName("FalconSat-3");
		ByteArrayLayout[] layouts = new ByteArrayLayout[3];
		layouts[0] = new ByteArrayLayout("WOD", "C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\WEformat.csv");
		layouts[1] = new ByteArrayLayout("TLM", "C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\TLMIformat.csv");
		layouts[2] = new ByteArrayLayout("TLM2", "C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\TLM2format.csv");
		SatTelemStore db = new SatTelemStore(99, "TLMDB", layouts);

		DataRecord d = new DataRecord(layouts[2], "0,0,1522039581,0,1956,2725,1963,2481,2228,515,258,31,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2034,2046,1048,1215,1217,1193,1241,1212,1207,1202,1190,1170,1211,1201,0,0,0,0,0,0,0");
		db.add(d);
		DataRecord d2 = new DataRecord(layouts[2], "0,0,1522039581,0,1956,2725,1963,2481,2228,515,258,31,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2034,2046,1048,1215,1217,1193,1241,1212,1207,1202,1190,1170,1211,1201,0,0,0,0,0,0,0");
		db.add(d2);
		
		LogFileWE bl = new LogFileWE(spacecraftSettings,"C:\\Users\\chris\\Google Drive\\AMSAT\\FalconSat-3\\telem\\we010310");
		for (DataRecord d3 : bl.records) {
			db.add(d3);
		}
	}

}
