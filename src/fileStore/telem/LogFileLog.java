package fileStore.telem;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;
import fileStore.PacSatField;
import fileStore.PacSatFile;

import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.data.Tools;

/**
 * This is a modern update to the AL format
 * AL was file type 201
 * This is file type 223
 * @author chris
 *
 */
public class LogFileLog extends PacSatFile {

	public static final int LEN_1 = 9;
	public static final int LEN_1F = 33;
	public static final int LEN_2 = 16;
	public static final int LEN_2F = 40;
	
	/* Events logged in the ALOG, in numerical order of their event code,
	** beginning with event 0 (nothing).
	*/
	String event_text[] = {
		"         ",  //0
		"STARTUP  ",
		"ERROR    ",
		"EXIT     ",
		"COMMAND  ",
		"FREE-DISK (blks, blk-size, files-avail, files)", //5
		"FS-START ",
		"FS-STOP  ",
		"LOG-LEVEL",
		};
	
//	public static final int ALOG__STARTUP = 1;		/* ftl0 startup */
//	public static final int ALOG_FTL0_SHUTDOWN = 2;		/* ftl0 shutdown */
//	public static final int ALOG_START_SESSION = 3;		/* user logon */
//	public static final int ALOG_CLOSE_SESSION = 4;		/* user logout */
//	public static final int ALOG_DISCONNECT = 5;		/* user timedout */
//	public static final int ALOG_USER_REFUSED = 6;		/* user refused (max sessions) */
//	public static final int ALOG_BCAST_START = 7;		/* added to list */
//	public static final int ALOG_BCAST_STOP = 8;		/* removed from list */
//	public static final int ALOG_DISKSPACE = 9;		/* free disk space */
//	public static final int ALOG_FILE_DELETE = 10;		/* file deleted */
//	public static final int ALOG_FILE_DOWNLOAD = 11;		/* file download */
//	public static final int ALOG_FILE_UPLOAD = 12;		/* file upload */
//	public static final int ALOG_BBS_SHUT = 13;		/* BBS is shut */
//	public static final int ALOG_BBS_OPEN = 14;		/* BBS is open */
//	public static final int ALOG_DIR = 15;			/* directory request */
//	public static final int ALOG_SELECT = 16;		/* Select */
//	public static final int ALOG_FILE_REMOVED = 17; /* Autodelete */
//	public static final int ALOG_FILE_NOT_REMOVED = 18; /* Autodelete failed */
//	public static final int ALOG_END_DOWNLOAD = 19;	/* End of download */
//	public static final int ALOG_END_UPLOAD = 20;	/* End of download */
//	public static final int ALOG_END_DIR = 21;				/* end of downloading dir file */
//	public static final int ALOG_SELECT_DONE = 22;	/* End of select */
//	/* These codes were not in the original ALOG*
//	 */
	public static final int ALOG_ERROR = 2;
//	public static final int ALOG_EVENT = 24;
//	public static final int ALOG_EVENT_WITH_VARS = 25;
//	public static final int ALOG_EVENT_WITH_CALL = 26;
//	public static final int ALOG_EVENT_WITH_CALL_AND_VARS = 27;

	
	public static final int MAX_SESSION = 20;
	int sidx=0;
	int event;
	AlogSession[] session;
	public String content;

	/* These errors were not in the original ALOG */
	String log_errors[] = {
			"Undefined error",
			"Removing PID File" //1
			,"Could not store WOD" //2
			,"Sending Packet" //3
			,"Setting Time"
			,"Max Radio Retries"
			,"Max TNC Retries"
			,"SSTV Failure"
			,"FS Failure"
			,"Direwolf Failure"
			,"PTT Failure"
			,"TNC Failure"
			,"Crew Interface Fail"
			,"Setting radio mode"
			,"Checking disk space"
		};
	
//	public static final int LOG_FTYPE = 223;			/* pfh file type */
	
	int[] data;
	RecordAL1 alog_1f;
	RecordAL2 alog_2f;
	
	/**
	 * Given the directory path and an ID,load this log file
	 * @param dir
	 * @param id
	 * @throws LayoutLoadException 
	 * @throws IOException 
	 */
	public LogFileLog(SpacecraftSettings spacecraftSettings, String dir, long id) throws LayoutLoadException, IOException {
		super(spacecraftSettings, dir, id);
		data = getData();
		session = new AlogSession[MAX_SESSION];
		content = parseFile();
	}
	
	public LogFileLog(SpacecraftSettings spacecraftSettings, String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		super(spacecraftSettings, fileName);
		data = getData();
		session = new AlogSession[MAX_SESSION];
		content = parseFile();
	}
	
	public LogFileLog(SpacecraftSettings spacecraftSettings, int[] bytes ) throws MalformedPfhException, IOException, LayoutLoadException {
		super(spacecraftSettings, bytes);
		data = bytes;
		session = new AlogSession[MAX_SESSION];
		content = parseFile();
	}

	private String parseFile() throws LayoutLoadException, IOException {
		//System.err.println("Processing AL file");
		String s = "";
		// Print the header once for each log file
		s = s + "Spacecraft Computer Log. " + " Length: " + data.length + " bytes\n\n";
		s = s + "Time\t\tEvent             Text     \tRx          Data\n";

		int i=0; // position in the data
		int len = 999;
		while (i < data.length && len > 0) {
			long timeStamp = 0;
			
			// Get the ALOG structure
			event = DataRecord.getByteValue(i, data);			
			len = DataRecord.getByteValue(i+1, data);
			//s = s + "Event: " + event + " Length: "+ len + " == ";
			if (len ==0 || len > 40) {
				s = s + "File is damaged, can not be parsed";
				break;
			}
			// s = s + len + " ";	
			
			int[] dataSet1 = Arrays.copyOfRange(data, i, 33+i);
			alog_1f = new RecordAL1(0, 0, timeStamp, 0, dataSet1);
			int[] dataSet2 = Arrays.copyOfRange(data, i, 40+i);
			alog_2f = new RecordAL2(0, 0, timeStamp, 0, dataSet2);
			
			switch (len) {
			case LEN_1:
				s = s + alog_1f.getTimeStamp() + "\t" + search_for_string(event_text, event);
				if (event == ALOG_ERROR)
					s = s + " " + search_for_string(log_errors, alog_1f.getRawValue("serial_no"));
				else
					s = s + " " + alog_1f.getRawValue("serial_no");

				break;
			case LEN_1F:
				s = s + alog_1f.getTimeStamp() + "\t" + search_for_string(event_text, event);
				long var1 = alog_1f.getRawValue("var1");
				long var2 = alog_1f.getRawValue("var2");
				long var3 = alog_1f.getRawValue("var3");
				long var4 = alog_1f.getRawValue("var4");
				long var5 = alog_1f.getRawValue("var5");
				long var6 = alog_1f.getRawValue("var6");
				s = s + " " + var1
						+ ", " + var2 
						+ ", " + var3 
						+ ", " + var4 
						+ ", " + var5 
						+ ", " + var6;
				break;
			
			case LEN_2:				
				s = s + pcalla();
			break;
			case LEN_2F:
				s = s + pcalla();
				var1 = alog_2f.getRawValue("var1");
				var2 = alog_2f.getRawValue("var2");
				var3 = alog_2f.getRawValue("var3");
				var4 = alog_2f.getRawValue("var4");
				var5 = alog_2f.getRawValue("var5");
				var6 = alog_2f.getRawValue("var6");
				s = s + "        " + var1
						+ ", " + var2 
						+ ", " + var3 
						+ ", " + var4 
						+ ", " + var5 
						+ ", " + var6;
			break;
			default:
				s = s + "UNK: " + alog_1f; // unknown event.  Print Timestamp and event number
			break;
			}
			s = s + "\n";
		
			i = i + len;
		}

		return s;
		
	}
	
	/*------------------------------------------------------------------------
	 Prints callsign for the beginning of a session, and puts the
	 call and ssid into the structure array holding each session.
	------------------------------------------------------------------------*/
	private String pcallsession() {
		String s = "";
		String c = alog_2f.getCallString();
		String ss = alog_2f.getSsid();
		int n = alog_2f.getRawValue("serial_no");
		session[sidx] = new AlogSession(c, ss, n);
		
//		memcpy(session[sidx].call,alog_2f->call,6);
//		session[sidx].ssid = alog_2f->ssid;
//		session[sidx].ssid = alog_2f->ssid;
//		session[sidx].session = alog_2f->serial_no;

		/* Move circularly through session array. */
		sidx = (sidx+1)%MAX_SESSION;

		s = s + pcalla();
//		printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
//		printf("%6.6s-%-2u  %u ",alog_2f->call,alog_2f->ssid,alog_2f->rxchan);
		return s;
	}
	
	/*------------------------------------------------------------------------
	  Prints callsign for action not related to a connected session.
		Takes call, ssid and receiver out of current alog_2f structure.
	------------------------------------------------------------------------*/
	private String pcalla() {
		String s = "";
		s = s + alog_1f.getTimeStamp() + "\t" + search_for_string(event_text, event) + " ";
//		printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
		s = s + alog_2f.getCallString() + "-" + alog_2f.getSsid() + "\t" + alog_2f.getRxChan();
//		printf("%6.6s-%-2u  %u ",alog_2f->call,alog_2f->ssid,alog_2f->rxchan); 
		return s;
	}
	
	/*------------------------------------------------------------------------
	  Prints callsign for action within a connected session.
		Looks up correct session in session[] using serial_no from current
		alog_1f structure.
		Takes call and ssid out of session[].
	------------------------------------------------------------------------*/
	private String pcall() {
		String s = "";
		int i;
		/* Find the session looking up serial numbers */
		int serial = alog_1f.getRawValue("serial_no");
		for (i=0;i<MAX_SESSION;i++)
			if (session[i].session == serial)
				break;

		/* Found it */
		s = s + alog_1f;
//		printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
		if (i < MAX_SESSION) 
			s = s + session[i].call + "-" + session[i].ssid;
//			printf("%6.6s-%-2u    ",session[i].call,session[i].ssid);
		else
			s = s + "******-**    ";
//			printf("******-**    "); 
		return s;
	}
	
	private String search_for_string(String[] strings, int var3) {
		String err_str = "";
		if (var3 > 0 && var3 < strings.length)
			err_str = " " + strings[var3];
		else 
			err_str = " " + var3;
		if (err_str == null)
			err_str = "";
		return err_str;
	}
	
	
	/**
	 * From offset passed, extract the next ALOG structure and return it
	 * @param data
	 * @param offset
	 */
	private RecordAL2 getAlogStruct(int[] data, int offset) {
		/* First two bytes contain structure length */
		int len = DataRecord.getIntValue(offset, data);
		
		// something went wrong
		return null;
		
	}
	
	public String toString() {
		return content;
	}
	
//	public static void main(String[] args) throws MalformedPfhException, IOException, LayoutLoadException {
//		Config.init("PacSatGround.properties");
//		Log.init("PacSatGround");
//		
//		LogFileAL bl = new LogFileAL("C:\\dos\\2bda.act");
//		System.out.println(bl);
//	}
}
