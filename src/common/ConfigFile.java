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

public abstract class ConfigFile {
	public Properties properties; // Java properties file for user defined values
	public String propertiesFileName = "";

	public ConfigFile(String name) {
		properties = new Properties();
		propertiesFileName = name;
		init();
	}
	
	abstract void initParams(); 		// Set the defaults here
	
	public void init() {
		initParams();
		load();
	}
	
	public void set(String key, String value) {
		properties.setProperty(key, value);
		//store();
	}
	
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	public void set(String sat, String fieldName, String key, String value) {
		properties.setProperty(sat + fieldName + key, value);
		//store();
	}
	
	public String get(String sat, String fieldName, String key) {
		return properties.getProperty(sat + fieldName + key);
	}
	
	public void set(String key, int value) {
		properties.setProperty(key, Integer.toString(value));
		//store();
	}

	public void set(String sat, String fieldName, String key, int value) {
		properties.setProperty(sat +  fieldName + key, Integer.toString(value));
		//store();
	}

	public void set(String sat, String fieldName, String key, long value) {
		properties.setProperty(sat +  fieldName + key, Long.toString(value));
		//store();
	}
	public void set(String key, boolean value) {
		properties.setProperty(key, Boolean.toString(value));
		//store();
	}
	public void set(String sat, String fieldName, String key, boolean value) {
		properties.setProperty(sat +  fieldName + key, Boolean.toString(value));
		//store();
	}
	
	public int getInt(String key) {
		try {
			return Integer.parseInt(properties.getProperty(key));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	public int getInt(String sat, String fieldName, String key) {
		try {
			return Integer.parseInt(properties.getProperty(sat +  fieldName + key));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public long getLong(String sat, String fieldName, String key) {
		try {
			return Long.parseLong(properties.getProperty(sat +  fieldName + key));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public boolean getBoolean(String key) {
		try {
			return Boolean.parseBoolean(properties.getProperty(key));
		} catch (NumberFormatException e) {
			return false;
		}
	}
	public boolean getBoolean(String sat, String fieldName, String key) {
		try {
			return Boolean.parseBoolean(properties.getProperty(sat +  fieldName + key));
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public void save() {
		try {
			FileOutputStream fos = new FileOutputStream(Config.get(Config.HOME_DIR) + File.separator + propertiesFileName);
			properties.store(fos, "PacSat Ground Station Properties");
			fos.close();
		} catch (FileNotFoundException e1) {
			Log.errorDialog("ERROR", "Could not write properties file. Check permissions on directory or on the file\n" +
					Config.get(Config.HOME_DIR) + File.separator + propertiesFileName);
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error writing properties file");
			e1.printStackTrace(Log.getWriter());
		}

	}
	
	public void load() {
		// try to load the properties from a file
		try {
			FileInputStream fis = new FileInputStream(Config.get(Config.HOME_DIR) + File.separator + propertiesFileName);
			properties.load(fis);
			fis.close();
		} catch (IOException e) {
			save();
		}
	}
	
	private String getProperty(String key) {
		String value = properties.getProperty(key);
		if (value == null) throw new NullPointerException();
		return value;
	}
	

}
