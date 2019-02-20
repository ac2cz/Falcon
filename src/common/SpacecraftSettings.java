package common;

import java.io.IOException;
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
	
	// PARAMS
	public static final String BROADCAST_CALLSIGN = "broadcastCallsign";
	public static final String BBS_CALLSIGN = "bbsCallsign";
	public static final String DIGI_CALLSIGN = "digiCallsign";
	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	public static final String SEQUENCE_NUM = "sequence_num";
	public static final String DIR_AGE = "DIR_AGE";
	
	// Layouts
	public static final String TLMI_LAYOUT = "TLMI_LAYOUT";
	public static final String TLM2_LAYOUT = "TLM2_LAYOUT";
	public static final String WOD_LAYOUT = "WOD_LAYOUT";
	
	
	public SpacecraftSettings(String fileName) throws LayoutLoadException, IOException {
		super(fileName);
		name = this.get(NAME);
		if (name == null)
			throw new LayoutLoadException("Spacecraft file is corrput and can not be loaded.  Try replacing\n"
					+ " it with the original file from the download installation. File: " + fileName);
		initDirectory();
		outbox = new Outbox(name);
	}
	
	public void initDirectory() {
		directory = new Directory(name);
	}
	
	@Override
	void initParams() {
		// we only init params that are not in the distributed paramaters
		set(SEQUENCE_NUM, 0);
		set(DIR_AGE, 10);
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
