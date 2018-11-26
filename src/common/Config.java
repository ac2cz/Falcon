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
import ax25.DataLinkStateMachine;
import gui.MainWindow;
import jssc.SerialPort;

public class Config {
	public static Properties properties; // Java properties file for user defined values
	public static String VERSION_NUM = "0.06";
	public static String VERSION = VERSION_NUM + " - 29 July 2018";
	public static final String propertiesFileName = "PacSatGround.properties";

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
	public static final String HOME_DIR = "home_dir";
	public static final String LOGFILE_DIR = "logfile_dir";
	public static final String LOGGING = "logging";
	public static final String KISS_LOGGING = "kiss_logging";
	public static final String USE_NATIVE_FILE_CHOOSER = "use_native_file_chooser";
	public static final String CALLSIGN = "callsign";
	public static final String DEFAULT_CALLSIGN = "NONE";
	public static final String TNC_COM_PORT = "COM_PORT";
	public static final String TNC_BAUD_RATE = "TNC_BAUD_RATE";
	public static final String TNC_DATA_BITS = "TNC_DATA_BITS";
	public static final String TNC_STOP_BITS = "TNC_STOP_BITS";
	public static final String TNC_PARITY = "TNC_PARITY";
	public static final String TNC_TX_DELAY = "TNC_TX_DELAY";
	public static final String DEBUG_LAYER2 = "DEBUG_LAYER2";
	
	public static boolean logging = true;
	
	public static Spacecraft spacecraft; // this can be a list later
	public static MainWindow mainWindow;
	public static Thread downlinkThread;
	public static Thread uplinkThread;
	public static UplinkStateMachine uplink; 
	public static DownlinkStateMachine downlink;
	public static Thread layer2Thread;
	public static DataLinkStateMachine layer2data;
	
	public static void load() {
		properties = new Properties();
		// Set the defaults here.  They are overwritten and ignored if the value is saved in the file
		set(HOME_DIR, "");
		set(LOGFILE_DIR, "");
		set(LOGGING, false);
		set(USE_NATIVE_FILE_CHOOSER, false);
		set(CALLSIGN, DEFAULT_CALLSIGN);
		set(TNC_COM_PORT, 0);
		set(TNC_BAUD_RATE, SerialPort.BAUDRATE_9600);
		set(TNC_DATA_BITS, SerialPort.DATABITS_8);
		set(TNC_STOP_BITS, SerialPort.STOPBITS_1);
		set(TNC_PARITY, SerialPort.PARITY_NONE);
		set(TNC_TX_DELAY, 130);
		set(KISS_LOGGING, false);
		set(DEBUG_LAYER2, true);
		loadFile();
	}
	
	public static void init() {
		try {
			spacecraft = new Spacecraft("FalconSat-3.dat");
		} catch (LayoutLoadException e) {
			Log.errorDialog("FATAL ERROR", e.getMessage() );
			System.exit(1);
		} catch (IOException e) {
			Log.errorDialog("FATAL ERROR", "Spacecraft file can not be processed: " + e.getMessage() );
			System.exit(1);
		}

		downlink = new DownlinkStateMachine(spacecraft);		
		downlinkThread = new Thread(downlink);
		downlinkThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		downlinkThread.setName("Downlink");
		downlinkThread.start();

		uplink = new UplinkStateMachine(spacecraft);		
		uplinkThread = new Thread(uplink);
		uplinkThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		uplinkThread.setName("Uplink");
		uplinkThread.start();
		
		layer2data = new DataLinkStateMachine(spacecraft);		
		layer2Thread = new Thread(layer2data);
		layer2Thread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		layer2Thread.setName("Layer2data");
		layer2Thread.start();

	}
	
	public static void close() {
		downlink.stopRunning();
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
			FileOutputStream fos = new FileOutputStream(Config.get(HOME_DIR) + propertiesFileName);
			properties.store(fos, "PacSat Ground Station Properties");
			fos.close();
		} catch (FileNotFoundException e1) {
			Log.errorDialog("ERROR", "Could not write properties file. Check permissions on directory or on the file\n" +
					Config.get(HOME_DIR) + propertiesFileName);
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error writing properties file");
			e1.printStackTrace(Log.getWriter());
		}

	}
	
	private static void loadFile() {
		// try to load the properties from a file
		try {
			FileInputStream fis = new FileInputStream(Config.get(HOME_DIR) +  propertiesFileName);
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
	

}
