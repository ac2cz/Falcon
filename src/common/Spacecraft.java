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

public class Spacecraft extends ConfigFile implements Comparable<Spacecraft> {
	public ConfigFile cfg; // Java properties file for user defined values
	public String name;
	public String broadcastCallsign;
	public String bbsCallsign;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static final int ERROR_IDX = -1;
	
	// PARAMS
	public static final String BROADCAST_CALLSIGN = "broadcastCallsign";
	public static final String BBS_CALLSIGN = "bbsCallsign";
	
	
	public Spacecraft(String fileName ) throws LayoutLoadException, IOException {
		super(fileName);	
	}
	
	@Override
	void initParams() {
		// we don't need to init here as a mssing config file is fatal
	}
	
	public String toString() {
		return name;
	}
	
	@Override
	public int compareTo(Spacecraft s2) {
		return name.compareToIgnoreCase(s2.name);
	}



	
	
}
