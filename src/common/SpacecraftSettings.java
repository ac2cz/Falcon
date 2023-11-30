package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.g0kla.telem.data.ConversionTable;
import com.g0kla.telem.segDb.SatTelemStore;
import com.g0kla.telem.segDb.Spacecraft;

import ax25.DataLinkStateMachine;
import fileStore.Directory;
import fileStore.Outbox;
import passControl.DownlinkStateMachine;
import passControl.UplinkStateMachine;

public class SpacecraftSettings extends ConfigFile implements Comparable<SpacecraftSettings> {
	public String name;
	public Directory directory;
	public Outbox outbox;
	public SatTelemStore db;
	public Spacecraft spacecraft; // reference to the telem layouts
	public ArrayList<CommandParams> commandParams;
	public Thread downlinkThread;
	public Thread uplinkThread;
	public UplinkStateMachine uplink; 
	public DownlinkStateMachine downlink;
	public Thread layer2Thread;
	public DataLinkStateMachine layer2data;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static String SOURCE = "usaf.fs-3.pacsat.tlm";
	
	public static final String MIR_SAT_1 = "Mir-Sat-1";
	
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
	public static final String TELEM_LAYOUT_FILE = "telemLayoutFile";
	public static final String TLMI_LAYOUT = "TLMI_LAYOUT";
	public static final String TLM1_LAYOUT = "TLM1_LAYOUT";
	public static final String TLM2_LAYOUT = "TLM2_LAYOUT";
	public static final String WOD_LAYOUT = "WOD_LAYOUT";
	public static final String FULL_WOD_LAYOUT = "FULL_WOD_LAYOUT";
	public static final String TLM16_LAYOUT = "TLM16_LAYOUT";
	
	public static final String TELEM_SERVER = "telem_server";
	
	public static final String NUMBER_DIR_TABLE_ENTRIES = "number_dir_table_entries";
	public static final String SHOW_SYSTEM_ON_DIR_TAB = "show_system_files_on_dir_tab";
	public static final String SHOW_USER_FILES = "show_user_files";
	public static final String SUPPORTS_FILE_UPLOAD = "supports_file_upload";
	public static final String SUPPORTS_PB_STATUS = "supports_pb_status";
	public static final String NORAD_ID = "norad_id";
	public static final String WEB_SITE_URL = "web_site_url";
	public static final String PSF_HEADER_CHECK_SUMS = "psf_header_check_sums";
	public static final String IS_COMMAND_STATION = "is_command_station";
	public static final String SECRET_KEY = "secret_key";
	public static final String COMMANDS_FILE = "commandsFile";
	
	public SpacecraftSettings(String fileName) throws LayoutLoadException, IOException {
		super(fileName);
		init();
		name = this.get(NAME);
		if (name == null)
			throw new LayoutLoadException("Spacecraft file is corrput and can not be loaded.  Try replacing\n"
					+ " it with the original file from the download installation. File: " + fileName);
		if (this.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) {
			loadCommands();
		}
		initDirectory();
		outbox = new Outbox(this, name);
		initStateMachines();
	}
	
	
	public void loadCommands()  {
		commandParams = new ArrayList<CommandParams>();
		String line;
		String fileName = "spacecraft" +File.separator + this.get(SpacecraftSettings.COMMANDS_FILE);
		Log.println("Loading Commands from: " + fileName);
		try {
			BufferedReader dis = new BufferedReader(new FileReader(fileName));
		
			while ((line = dis.readLine()) != null) {
				if (line != null) {
					String[] items = line.split("\\s*,\\s*"); // split and trim white space

					try {
						CommandParams commandParam = new CommandParams(items);
						commandParams.add(commandParam);
						//Log.println("Loaded: "+commandParam);
					} catch (Exception e) {
						//Log.println("Ignoring CommandParam: " + e.toString());
						// Ignore bad line
					}
				}
			}
			dis.close();
		} catch (FileNotFoundException n) {
			Log.println("ERROR: Commands file not found.  Missing: " + fileName);
			set(IS_COMMAND_STATION, false);
		} catch (IOException e) {
			Log.println("ERROR: Reading from commands file: " + fileName);
			set(IS_COMMAND_STATION, false);		}
	}
	
	public void initDirectory() {
		directory = new Directory(name, this);
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(2); // one live and one spare thread
		ses.scheduleAtFixedRate(new Runnable() {
		    @Override
		    public void run() {
				Thread.currentThread().setName("Dir Saver");
		        directory.saveIfNeeded();
		        //Date now = new Date();
		        //System.err.println("SAVING DIR: " + now);
		    }
		}, 5*60, 5*60, TimeUnit.SECONDS);  // In general we save at exit.  Dir is in memory.  But save every 5 mins just in case
		
	}
	
	public void initSegDb(Spacecraft spacecraft) throws com.g0kla.telem.data.LayoutLoadException, IOException {
		ConversionTable ct = new ConversionTable(Config.currentDir + File.separator + "spacecraft"+File.separator+spacecraft.conversionCoefficientsFileName);
		for (int i=0; i< spacecraft.layout.length; i++)
			spacecraft.layout[i].setConversionTable(ct);
		db = new SatTelemStore(spacecraft.satId, Config.get(Config.LOGFILE_DIR) + File.separator + name + File.separator + "TLMDB", spacecraft.layout);
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
		set(TELEM_LAYOUT_FILE, "FS-3.dat");
		set(IS_COMMAND_STATION, false);
		set(SECRET_KEY, "");
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
	
	public boolean hasCallsign(String call) {
		call = call.toUpperCase();
		String broadcastCall = get(SpacecraftSettings.BROADCAST_CALLSIGN);
		if (call.startsWith(broadcastCall))
			return true;
		String bbsCall = get(SpacecraftSettings.BBS_CALLSIGN);
		if (call.startsWith(bbsCall))
			return true;
		String digiCall = get(SpacecraftSettings.DIGI_CALLSIGN);
		if (call.startsWith(digiCall))
			return true;
		return false;
	}
	
	private void initStateMachines() {
		if (downlink != null) {
			downlink.stopRunning();
			try { Thread.sleep(10);	} catch (InterruptedException e) { e.printStackTrace();}
		}
		downlink = new DownlinkStateMachine(this);		
		downlinkThread = new Thread(downlink);
		downlinkThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		downlinkThread.setName("Downlink " + this.name);
		downlinkThread.start();

		if (uplink != null) {
			uplink.stopRunning();
			try { Thread.sleep(10);	} catch (InterruptedException e) { e.printStackTrace();}
		}
		uplink = new UplinkStateMachine(this);		
		uplinkThread = new Thread(uplink);
		uplinkThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		uplinkThread.setName("Uplink "+ this.name);
		uplinkThread.start();
		
		initLayer2();
	}
	
	public void initLayer2() {
		if (layer2data != null) {
			layer2data.stopRunning();
			try { Thread.sleep(10);	} catch (InterruptedException e) { e.printStackTrace();}
		}
		layer2data = new DataLinkStateMachine(this);		
		layer2Thread = new Thread(layer2data);
		layer2Thread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		layer2Thread.setName("Layer2data "+ this.name);
		layer2Thread.start();
	}
	
	public void close() {
		downlink.stopRunning();
		uplink.stopRunning();
		layer2data.stopRunning();
	}
	
	public String toString() {
		return name;
	}
	
	@Override
	public int compareTo(SpacecraftSettings s2) {
		return name.compareToIgnoreCase(s2.name);
	}
}
