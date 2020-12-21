package common;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fileStore.Directory;
import fileStore.Outbox;

public class SpacecraftSettings extends ConfigFile implements Comparable<SpacecraftSettings> {
	public String name;
	public Directory directory;
	public Outbox outbox;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static String SOURCE = "usaf.fs-3.pacsat.tlm";
	public static final int ERROR_IDX = -1;
	public static final int MAX_DIR_AGE = 99; // Maximum days back in time for directory requests.  10-30 is a more practical value.
	public static final int MAX_DIR_TABLE_ENTRIES = 9999;
	
	// PARAMS
	public static final String BROADCAST_CALLSIGN = "broadcastCallsign";
	public static final String BBS_CALLSIGN = "bbsCallsign";
	public static final String DIGI_CALLSIGN = "digiCallsign";
	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	public static final String SEQUENCE_NUM = "sequence_num";
	public static final String DIR_AGE = "DIR_AGE";

	// Pass Control
	public static final String REQ_DIRECTORY = "request_directory";
	public static final String FILL_DIRECTORY_HOLES = "fill_directory_holes";
	public static final String REQ_FILES = "request_files";
	public static final String UPLOAD_FILES = "upload_files";

	// Layouts
	public static final String TLMI_LAYOUT = "TLMI_LAYOUT";
	public static final String TLM2_LAYOUT = "TLM2_LAYOUT";
	public static final String WOD_LAYOUT = "WOD_LAYOUT";
	
	public static final String NUMBER_DIR_TABLE_ENTRIES = "number_dir_table_entries";
	
	public SpacecraftSettings(String fileName) throws LayoutLoadException, IOException {
		super(fileName);
		init();
		name = this.get(NAME);
		if (name == null)
			throw new LayoutLoadException("Spacecraft file is corrput and can not be loaded.  Try replacing\n"
					+ " it with the original file from the download installation. File: " + fileName);
		initDirectory();
		outbox = new Outbox(name);
	}
	
	public void initDirectory() {
		directory = new Directory(name, this);
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(2); // one live and one spare thread
		ses.scheduleAtFixedRate(new Runnable() {
		    @Override
		    public void run() {
				Thread.currentThread().setName("Dir Saver");
		        Config.spacecraftSettings.directory.saveIfNeeded();
		        //Date now = new Date();
		        //System.err.println("SAVING DIR: " + now);
		    }
		}, 5*60, 5*60, TimeUnit.SECONDS);  // In general we save at exit.  Dir is in memory.  But save every 5 mins just in case
		
	}
		
	@Override
	void initParams() {
		// we only init params that are not in the distributed paramaters
		set(SEQUENCE_NUM, 0);
		set(REQ_DIRECTORY, true);
		set(FILL_DIRECTORY_HOLES, true);
		set(REQ_FILES, true);
		set(UPLOAD_FILES, true);
		set(NUMBER_DIR_TABLE_ENTRIES, 200);
	}

	public int getNextSequenceNum() {
		int seq = getInt(SEQUENCE_NUM);
		seq++;
		if (seq > 99)
			seq = 0;
		set(SEQUENCE_NUM, seq);
		save();
		return seq;
	}
	
	public String toString() {
		return name;
	}
	
	@Override
	public int compareTo(SpacecraftSettings s2) {
		return name.compareToIgnoreCase(s2.name);
	}
}
