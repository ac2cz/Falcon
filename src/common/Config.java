package common;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;

import com.g0kla.telem.data.ConversionTable;
import com.g0kla.telem.segDb.SatTelemStore;
import com.g0kla.telem.segDb.SatelliteManager;
import com.g0kla.telem.segDb.Spacecraft;
import com.g0kla.telem.segDb.TleFileException;
import com.g0kla.telem.server.STPQueue;
import com.g0kla.telem.server.Sequence;

import passControl.DownlinkStateMachine;
import passControl.UplinkStateMachine;
import uk.me.g4dpz.satellite.GroundStationPosition;
import ax25.DataLinkStateMachine;
import gui.MainWindow;
import jssc.SerialPort;
import pacSatServer.KissStpQueue;

public class Config {
	public static Properties properties; // Java properties file for user defined values
	public static String VERSION_NUM = "0.38.8";
	public static String VERSION = VERSION_NUM + " - 7 Mar 2021";
	public static String propertiesFileName = "PacSatGround.properties";
	public static String homeDir = "";
	public static String currentDir = "";
	public static boolean logDirFromPassedParam = false;
	public static int totalFrames = 0; // counter of the frames decoded this session

	public static final String WINDOWS = "win";
	public static final String MACOS = "mac";
	public static final String LINUX = "lin";
    public static String osName = "Unknown OS";
    public static String OS = WINDOWS;
    
	public static final Color AMSAT_BLUE = new Color(0,0,116);
	public static final Color AMSAT_RED = new Color(224,0,0);
	public static final Color PURPLE = new Color(123,6,130);
	public static final Color AMSAT_GREEN = new Color(0,102,0);
	
	//static public GroundStationPosition GROUND_STATION = null;
	public static final String NONE = "NONE";

	public static final String DEFAULT_STATION = NONE;
	public static final String DEFAULT_ALTITUDE = "0";
	public static final String DEFAULT_LATITUDE = "0.0";
	public static final String DEFAULT_LONGITUDE = "0.0";
	public static final String DEFAULT_LOCATOR = "XX00xx";
	
	// Allowed global paramaters follow
	// Modules can define their own too and save with the routines
	public static final String FIRST_RUN = "first_run";
	public static final String GLOBAL = "global";
//	public static final String HOME_DIR = "home_dir";
	public static final String LOGFILE_DIR = "logfile_dir";
	public static final String LOGGING = "logging";
	public static final String ECHO_TO_STDOUT = "echo_to_stdout";
	public static final String KISS_LOGGING = "kiss_logging";
	public static final String USE_NATIVE_FILE_CHOOSER = "use_native_file_chooser";
	public static final String CALLSIGN = "callsign";
	public static final String DEFAULT_CALLSIGN = "NONE";
	public static final String NO_COM_PORT = "NONE";
	public static final String TNC_COM_PORT = "COM_PORT";
	public static final String TNC_BAUD_RATE = "TNC_BAUD_RATE";
	public static final String TNC_DATA_BITS = "TNC_DATA_BITS";
	public static final String TNC_STOP_BITS = "TNC_STOP_BITS";
	public static final String TNC_PARITY = "TNC_PARITY";
	public static final String TNC_TCP_PORT = "tnc_tcp_port";
	public static final String TNC_TCP_HOSTNAME = "tnc_tcp_hostname";
	public static final String TNC_TX_DELAY = "TNC_TX_DELAY";
	public static final String DEBUG_LAYER2 = "DEBUG_LAYER2";
	public static final String DEBUG_LAYER3 = "DEBUG_LAYER3";
	public static final String DEBUG_DOWNLINK = "DEBUG_DOWNLINK";
	public static final String DEBUG_TELEM = "DEBUG_TELEM";
	public static final String DEBUG_TX = "DEBUG_TX";
	public static final String IGNORE_UA_PF = "ignore_ua_pf";
	public static final String UPLINK_ENABLED = "uplink_enabled";
	public static final String DOWNLINK_ENABLED = "downlink_enabled";
	public static final String KISS_TCP_INTERFACE = "kiss_tcp_interface";
	public static final String TX_INHIBIT = "tx_inhibit";
	public static final String SEND_TO_SERVER = "send_to_server";
	public static final String TELEM_SERVER = "telem_server";
	public static final String TELEM_SERVER_PORT = "telem_server_port";
	public static final String WEB_SITE_URL = "web_site_url";
	public static final String MAIDENHEAD_LOC = "maidenhead_loc";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String ALTITUDE = "altitude";
	public static final String STATION_DETAILS = "station_details";
	public static final String TOGGLE_KISS = "toggle_kiss";
	public static final String SHOW_SYSTEM_ON_DIR_TAB = "show_system_files_on_dir_tab";
	public static final String SHOW_USER_FILES = "show_uer_files";
	public static final String DEBUG_EVENTS = "DEBUG_EVENTS";
	public static final String KISS_BYTES_AT_START = "kiss_bytes_at_start";
	public static final String KISS_BYTES_AT_END = "kiss_bytes_at_end";
	public static final String EDIT_KISS_BYTES = "edit_kiss_bytes";
	public static final String KEEP_CARET_AT_END_OF_LOG = "keep_caret_at_end_of_log";
	public static final String SEND_USER_DEFINED_TNC_BYTES = "send_user_defined_tnc_bytes";
	public static final String ARCHIVE_DIR = "archive_dir";
	public static final String FONT_SIZE = "font_size";
	
	public static boolean logging = true;
	
	public static SatelliteManager satManager;
	public static Spacecraft spacecraft; // The layouts and fixed paramaters
	public static SpacecraftSettings spacecraftSettings; // User settings: this can be a list of spacecraft later
	public static MainWindow mainWindow;
	public static Thread downlinkThread;
	public static Thread uplinkThread;
	public static UplinkStateMachine uplink; 
	public static DownlinkStateMachine downlink;
	public static Thread layer2Thread;
	public static DataLinkStateMachine layer2data;
	public static SatTelemStore db;
	public static STPQueue stpQueue;
	public static Thread stpThread;
	public static Sequence sequence;
	
	public static void init(String file) {
		propertiesFileName = file;
		// Work out the OS but dont save in the properties.  It might be a different OS next time!
		osName = System.getProperty("os.name").toLowerCase();
		setOs();
		properties = new Properties();
		// Set the defaults here.  They are overwritten and ignored if the value is saved in the file
		set(FIRST_RUN, true);
//		set(HOME_DIR, "");
		set(LOGFILE_DIR, "");
		set(LOGGING, false);
		set(ECHO_TO_STDOUT, false);
		set(USE_NATIVE_FILE_CHOOSER, true);
		set(CALLSIGN, DEFAULT_CALLSIGN);
		set(TNC_COM_PORT, NO_COM_PORT);
		set(TNC_BAUD_RATE, SerialPort.BAUDRATE_9600);
		set(TNC_DATA_BITS, SerialPort.DATABITS_8);
		set(TNC_STOP_BITS, SerialPort.STOPBITS_1);
		set(TNC_PARITY, SerialPort.PARITY_NONE);
		set(TNC_TX_DELAY, 130);
		set(KISS_LOGGING, false);
		set(DEBUG_LAYER2, false);
		set(DEBUG_LAYER3, false);
		set(DEBUG_DOWNLINK, false);
		set(DEBUG_TX, false);
		set(IGNORE_UA_PF, true);
		set(UPLINK_ENABLED, true);
		set(DOWNLINK_ENABLED, true);
		set(TNC_TCP_HOSTNAME,"127.0.0.1");
		set(TNC_TCP_PORT,8100);
		set(KISS_TCP_INTERFACE, false);
		set(DEBUG_TELEM, false);
		set(TX_INHIBIT, false);
		set(SEND_TO_SERVER, false);
		set(TELEM_SERVER,"tlm.amsatfox.org");
		set(TELEM_SERVER_PORT,41041);
		set(MAIDENHEAD_LOC,DEFAULT_LOCATOR);
		set(ALTITUDE,DEFAULT_ALTITUDE);
		set(LONGITUDE,DEFAULT_LONGITUDE);
		set(LATITUDE,DEFAULT_LATITUDE);
		set(STATION_DETAILS,DEFAULT_STATION);
		set(WEB_SITE_URL,"http://tlm.amsatfox.org/tlm");
		set(TOGGLE_KISS,true);
		set(SHOW_SYSTEM_ON_DIR_TAB,false);
		set(SHOW_USER_FILES,true);
		set(DEBUG_EVENTS, false);
		set(KISS_BYTES_AT_START,"4B 49 53 53 20 4F 4E 0D 52 45 53 54 41 52 54 0D");
		set(KISS_BYTES_AT_END,"C0 FF C0");
		set(EDIT_KISS_BYTES, false);
		set(KEEP_CARET_AT_END_OF_LOG, true);
		set(SEND_USER_DEFINED_TNC_BYTES, false);
		set(ARCHIVE_DIR, "");
		set(FONT_SIZE, 0);
		
	}
	
	public static void load() {
		loadFile();
	}
	
	public static boolean missing() { 
		File aFile = new File(Config.homeDir + File.separator + propertiesFileName );
		if(!aFile.exists()){
			return true;
		}
		return false;
	}
	
	public static void setHome() {
		File aFile = new File(Config.homeDir);
		if(!aFile.isDirectory()){
			
			aFile.mkdir();
			//Log.errorDialog("GOOD", "Making directory: " + Config.homeDirectory);
			
		}
		if(!aFile.isDirectory()){
			Log.errorDialog("ERROR", "ERROR can't create the directory: " + aFile.getAbsolutePath() +  
					"\nFoxTelem needs to save the program settings.  The directory is either not accessible or not writable\n");
		}		
	}
	
	public static void simpleStart() throws com.g0kla.telem.data.LayoutLoadException, IOException {
		try {
			File satFile = new File(Config.get(Config.LOGFILE_DIR) + File.separator + "FalconSat-3.properties");
			if (!satFile.exists()) {
				File masterFile = new File(Config.currentDir + File.separator + "FalconSat-3.properties");
				if (!masterFile.exists()) {
					Log.errorDialog("FATAL", "Installation is corrupt.  Could not find: " + masterFile.getPath());
					System.exit(1);
				}
					
				if (!copyFile(masterFile, satFile)) {
					Log.errorDialog("FATAL", "Could not install: " + masterFile.getPath() + "\ninto: " +  Config.get(Config.LOGFILE_DIR));
					System.exit(1);
				}
			}
			spacecraftSettings = new SpacecraftSettings(Config.get(Config.LOGFILE_DIR) + File.separator + "FalconSat-3.properties");
		} catch (LayoutLoadException e) {
			Log.errorDialog("FATAL ERROR", e.getMessage() );
			System.exit(1);
		} catch (IOException e) {
			Log.errorDialog("FATAL ERROR", "Spacecraft file can not be processed: " + e.getMessage() );
			System.exit(1);
		}

		satManager = new SatelliteManager(false, Config.get(Config.LOGFILE_DIR) + File.separator + spacecraftSettings.name);
		
		try {
			spacecraft = new Spacecraft(Config.get(Config.LOGFILE_DIR) + File.separator + spacecraftSettings.name, 
					new File(Config.currentDir + File.separator + "spacecraft"+File.separator+"FS-3.dat"));
		} catch (IOException e1) {
			Log.errorDialog("FATAL ERROR", "Spacecraft file could not be loaded: spacecraft"+File.separator+"FS-3.dat\n" + e1.getMessage() );
			System.exit(1);
		}
		try {
			spacecraft.loadTleHistory();
		} catch (TleFileException e) {
			// Ignore this error.  NO TLE but not fatal
		}
		satManager.add(spacecraft);
		try {
			satManager.fetchTLEFile(Config.get(Config.LOGFILE_DIR) + File.separator + spacecraftSettings.name);
		} catch (Exception e) {
			//Could not download the Keps file.  Not critical.  Log the error
			Log.println("ERROR: " + e.getMessage());
		}
		initSegDb();		
	}
	
	public static void start() throws com.g0kla.telem.data.LayoutLoadException, IOException {
		simpleStart();
		storeGroundStation();

		downlink = new DownlinkStateMachine(spacecraftSettings);		
		downlinkThread = new Thread(downlink);
		downlinkThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		downlinkThread.setName("Downlink");
		downlinkThread.start();

		uplink = new UplinkStateMachine(spacecraftSettings);		
		uplinkThread = new Thread(uplink);
		uplinkThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		uplinkThread.setName("Uplink");
		uplinkThread.start();
		
		initLayer2();
		
		stpQueue = new KissStpQueue(get(LOGFILE_DIR) + File.separator + "stp.dat", Config.get(Config.TELEM_SERVER), Config.getInt(Config.TELEM_SERVER_PORT));		
		stpThread = new Thread(stpQueue);
		stpThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		stpThread.setName("STP Queue");
		stpThread.start();
		sequence = new Sequence(get(LOGFILE_DIR));
	}
	
	public static void initLayer2() {
		layer2data = new DataLinkStateMachine();		
		layer2Thread = new Thread(layer2data);
		layer2Thread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		layer2Thread.setName("Layer2data");
		layer2Thread.start();
	}
	
//	public static ByteArrayLayout[] layouts;
	
	public static void initSegDb() throws com.g0kla.telem.data.LayoutLoadException, IOException {
		ConversionTable ct = new ConversionTable(Config.currentDir + File.separator + "spacecraft"+File.separator+"Fs3coef.csv");
		spacecraft.layout[0].setConversionTable(ct);
		spacecraft.layout[1].setConversionTable(ct);
		spacecraft.layout[2].setConversionTable(ct);
		db = new SatTelemStore(spacecraft.satId, Config.get(Config.LOGFILE_DIR) + File.separator + spacecraftSettings.name + File.separator + "TLMDB", spacecraft.layout);
	}
	
	public static void storeGroundStation() {
		int h = 0;
		GroundStationPosition pos;
		try {
			if (Config.get(Config.ALTITUDE).equalsIgnoreCase(Config.NONE)) {
				Config.set(Config.ALTITUDE,"0");
				h = 0;
			} else
				h = Integer.parseInt(Config.get(Config.ALTITUDE));
		} catch (NumberFormatException e) {
			// not much to do.  Just leave h as 0;
		}
		try {
			float lat = Float.parseFloat(Config.get(Config.LATITUDE));
			float lon = Float.parseFloat(Config.get(Config.LONGITUDE));
			pos = new GroundStationPosition(lat, lon, h);
		} catch (NumberFormatException e) {
			pos = new GroundStationPosition(0, 0, 0); // Dummy ground station.  This works for position calculations but not for Az/El
		}
		spacecraft.setGroundStationPosition(pos);

	}

	
	public static void close() {
		downlink.stopRunning();
		uplink.stopRunning();
		layer2data.stopRunning();
	}
	
	private static void setOs() {
		if (osName.indexOf("win") >= 0) {
			OS = WINDOWS;
		} else if (osName.indexOf("mac") >= 0) {
			OS = MACOS;
		} else {
			OS = LINUX;
		}
	}
	
	public static boolean isWindowsOs() {
		if (OS == WINDOWS) {
			return true;
		}
		return false;
	}

	public static boolean isLinuxOs() {
		if (OS == LINUX) {
			return true;
		}
		return false;
	}

	public static boolean isMacOs() {
		if (OS == MACOS) {
			return true;
		}
		return false;
	}
	
	public static void set(String key, String value) {
		properties.setProperty(key, value);
		//store();
	}
	
	public static String get(String key) {
		return properties.getProperty(key);
	}
	
	public static void set(String sat, String fieldName, String key, String value) {
		properties.setProperty(sat + fieldName + key, value);
		//store();
	}
	
	public static String get(String sat, String fieldName, String key) {
		return properties.getProperty(sat + fieldName + key);
	}
	
	public static void set(String key, int value) {
		properties.setProperty(key, Integer.toString(value));
		//store();
	}

	public static void set(String sat, String fieldName, String key, int value) {
		properties.setProperty(sat +  fieldName + key, Integer.toString(value));
		//store();
	}

	public static void set(String sat, String fieldName, String key, long value) {
		properties.setProperty(sat +  fieldName + key, Long.toString(value));
		//store();
	}
	public static void set(String key, boolean value) {
		properties.setProperty(key, Boolean.toString(value));
		//store();
	}
	public static void set(String sat, String fieldName, String key, boolean value) {
		properties.setProperty(sat +  fieldName + key, Boolean.toString(value));
		//store();
	}
	
	public static int getInt(String key) {
		try {
			return Integer.parseInt(properties.getProperty(key));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	public static int getInt(String sat, String fieldName, String key) {
		try {
			return Integer.parseInt(properties.getProperty(sat +  fieldName + key));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static long getLong(String sat, String fieldName, String key) {
		try {
			return Long.parseLong(properties.getProperty(sat +  fieldName + key));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static boolean getBoolean(String key) {
		try {
			return Boolean.parseBoolean(properties.getProperty(key));
		} catch (NumberFormatException e) {
			return false;
		}
	}
	public static boolean getBoolean(String sat, String fieldName, String key) {
		try {
			return Boolean.parseBoolean(properties.getProperty(sat +  fieldName + key));
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static void save() {
		try {
			FileOutputStream fos = new FileOutputStream(homeDir + File.separator + propertiesFileName);
			properties.store(fos, "PacSat Ground Station Properties");
			fos.close();
		} catch (FileNotFoundException e1) {
			Log.errorDialog("ERROR", "Could not write properties file. Check permissions on directory or on the file\n" +
					homeDir + propertiesFileName);
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error writing properties file");
			e1.printStackTrace(Log.getWriter());
		}

	}
	
	private static void loadFile() {
		// try to load the properties from a file
		try {
			FileInputStream fis = new FileInputStream(homeDir +  File.separator + propertiesFileName);
			properties.load(fis);
			fis.close();
		} catch (IOException e) {
			save();
		}
	}
	
	private static String getProperty(String key) {
		String value = properties.getProperty(key);
		if (value == null) throw new NullPointerException();
		return value;
	}
	
	/**
	 * Utility function to copy a file
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	@SuppressWarnings("resource") // because we have a finally statement and the checker does not seem to realize that
	public static boolean copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        long transferred = destination.transferFrom(source, 0, source.size());
	        if (transferred == source.size()) return true;
	        return false;
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
}
