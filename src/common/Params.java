package common;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import passControl.DownlinkStateMachine;
import passControl.UplinkStateMachine;
import fileStore.Directory;
import gui.MainWindow;

public class Params extends ConfigFile {
	public static String VERSION_NUM = "0.01";
	public static String VERSION = VERSION_NUM + " - 10 Jun 2018";

	public static final String WINDOWS = "win";
	public static final String MACOS = "mac";
	public static final String LINUX = "lin";
    public static String osName = "Unknown OS";
    public static String OS = WINDOWS;
    
	public static final Color AMSAT_BLUE = new Color(0,0,116);
	public static final Color AMSAT_RED = new Color(224,0,0);
	public static final Color PURPLE = new Color(123,6,130);
	public static final Color AMSAT_GREEN = new Color(0,102,0);
	
	// Allowed global paramaters follow
	// Modules can define their own too and save with the routines
	public static final String GLOBAL = "global";
	public static final String LOGFILE_DIR = "logfile_dir";
	public static final String LOGGING = "logging";
	public static final String USE_NATIVE_FILE_CHOOSER = "use_native_file_chooser";
	public static final String CALLSIGN = "callsign";
	public static final String DEFAULT_CALLSIGN = "NONE";
	
	public Params() {
		super("PacSatGround.properties");		
	}
	
	public void initParams() {
		// Set the defaults here
		set(LOGFILE_DIR, ".");
		set(LOGGING, true);
		set(USE_NATIVE_FILE_CHOOSER, false);
		set(CALLSIGN, DEFAULT_CALLSIGN);
		load();
		
	}
	
}
