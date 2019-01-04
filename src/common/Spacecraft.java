package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;

import fileStore.Directory;
import fileStore.Outbox;

public class Spacecraft extends ConfigFile implements Comparable<Spacecraft> {
	public ConfigFile cfg; // Java properties file for user defined values
	public String name;
	public Directory directory;
	public Outbox outbox;
	
	public static String SPACECRAFT_DIR = "spacecraft";
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
	
	
	public Spacecraft(String fileName) throws LayoutLoadException, IOException {
		super(fileName);
		name = this.get(NAME);
		if (name == null)
			throw new LayoutLoadException("Spacecraft file is corrput and can not be loaded.  Try replacing\n"
					+ " it with the original file from the download installation. File: " + fileName);
		directory = new Directory(name);
		outbox = new Outbox(name);
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
	public int compareTo(Spacecraft s2) {
		return name.compareToIgnoreCase(s2.name);
	}
}
