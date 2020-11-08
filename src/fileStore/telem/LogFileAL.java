package fileStore.telem;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import common.Config;
import common.Log;
import fileStore.MalformedPfhException;
import fileStore.PacSatField;
import fileStore.PacSatFile;

import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.data.Tools;

public class LogFileAL extends PacSatFile {

	public static final int ALOG_FTL0_STARTUP = 1;		/* ftl0 startup */
	public static final int ALOG_FTL0_SHUTDOWN = 2;		/* ftl0 shutdown */
	public static final int ALOG_START_SESSION = 3;		/* user logon */
	public static final int ALOG_CLOSE_SESSION = 4;		/* user logout */
	public static final int ALOG_DISCONNECT = 5;		/* user timedout */
	public static final int ALOG_USER_REFUSED = 6;		/* user refused (max sessions) */
	public static final int ALOG_BCAST_START = 7;		/* added to list */
	public static final int ALOG_BCAST_STOP = 8;		/* removed from list */
	public static final int ALOG_DISKSPACE = 9;		/* free disk space */
	public static final int ALOG_FILE_DELETE = 10;		/* file deleted */
	public static final int ALOG_FILE_DOWNLOAD = 11;		/* file download */
	public static final int ALOG_FILE_UPLOAD = 12;		/* file upload */
	public static final int ALOG_BBS_SHUT = 13;		/* BBS is shut */
	public static final int ALOG_BBS_OPEN = 14;		/* BBS is open */
	public static final int ALOG_DIR = 15;			/* directory request */
	public static final int ALOG_SELECT = 16;		/* Select */
	public static final int ALOG_FILE_REMOVED = 17; /* Autodelete */
	public static final int ALOG_FILE_NOT_REMOVED = 18; /* Autodelete failed */
	public static final int ALOG_END_DOWNLOAD = 19;	/* End of download */
	public static final int ALOG_END_UPLOAD = 20;	/* End of download */
	public static final int ALOG_END_DIR = 21;				/* end of downloading dir file */
	public static final int ALOG_SELECT_DONE = 22;	/* End of select */
	
	public static final int MAX_SESSION = 20;
	int sidx=0;
	AlogSession[] session;
	public String content;

	/* General FTL0 error messages
	*/
	String ftl0_errors[]={
			"", // 0
	"ERROR:PG command bug",
	"ERROR:cannot continue",
	"ERROR:PACSAT err",
	"ERROR:No such file",
	"ERROR:Selection empty", // 5
	"ERROR:Mandatory PFH err",
	"ERROR:PHF err",
	"ERROR:Bad selection",
	"ERROR:File locked",
	"ERROR:No such destination", //10
	"ERROR:File partial.",
	"ERROR:File complete.",
	"ERROR:PACSAT file system full",
	"ERROR:PFH err",
	"ERROR:PFH checksum failure", //15
	"ERROR:body checksum failure"
	};
	
	/* CAUSES FOR FTL0 SESSION TERMINATION
	** Used in calls to ftl_close_session()
	*/
	public static final int FTC_FRMR_REM = 1;													/* Remote frame reject 				*/
	public static final int FTC_FRMR_LOCAL = 2;												/* Locally generated frmr			*/
	public static final int FTC_DISC_LOCAL = 3;												/* Locally requested disc			*/
	public static final int FTC_DISC_REM = 4;												/* Remote requested disc			*/
	public static final int FTC_TIMEOUT = 5;														/* AX25 timeout.							*/
	public static final int FTC_RECONNECT = 6;													/* Client reconnected.				*/
	public static final int FTC_QUIT = 7;															/* BBS closed down.						*/
	
	/* Reasons for closing an FTL0 session
	*/
	String close_reasons[] =
	{
		"Undefined error", // 0
			"user FRMR",
			"server FRMR",
			"server disconnect",
			"user disconnect",
			"FTL0 timeout",
			"reconnection",
			"quit"
	};

	/* Disconnect reasons */
	public static final long DC_TIMEOUT = 1l;
	public static final long DC_IN_ULOK = 2l;
	public static final long DC_IN_DLOK = 3l;
	public static final long DC_IN_DLEND = 4l;
	public static final long DC_IN_ULRX = 5l;
	public static final long DC_UNKNOWN_PKT = 6l;
	public static final long DC_PKT_TOO_BIG = 7l;
	

	String disc_reasons[] = {
		"Undefined error",
			"Inactivity timeout", //1
			"Unexpected input (uplink command)", //2
			"Unexpected input (downlink command)", //3
			"Unexpected input (downlink end)", //4
			"Unexpected input (uplink data)", //5
			"FTL0 packet type unknown.", //6
			"FTL0 command packet too long." //7
	};

	public static final int ALOG_FTYPE = 0x0c;			/* pfh file type */
	
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
	public LogFileAL(String dir, long id) throws LayoutLoadException, IOException {
		super(dir, id);
		data = getData();
		session = new AlogSession[MAX_SESSION];
		content = parseFile();
	}
	
	public LogFileAL(String fileName) throws MalformedPfhException, IOException, LayoutLoadException {
		super(fileName);
		data = getData();
		session = new AlogSession[MAX_SESSION];
		content = parseFile();
	}
	
	public LogFileAL(int[] bytes ) throws MalformedPfhException, IOException, LayoutLoadException {
		super(bytes);
		data = bytes;
		session = new AlogSession[MAX_SESSION];
		content = parseFile();
	}

	private String parseFile() throws LayoutLoadException, IOException {
		System.err.println("Processing AL file");
		String s = "";
		// Print the header once for each log file
		s = s + "FTL0 Activity Log. " + " Length: " + data.length + " bytes\n\n";
		s = s + "Time\t\t\tActivity\tCall\tRx   Session\n";

		int i=0; // position in the data
		int len = 999;
		while (i < data.length && len > 0) {
			long timeStamp = 0;
			
			// Get the ALOG structure
			int event = DataRecord.getByteValue(i, data);			
			len = DataRecord.getByteValue(i+1, data);
			//s = s + "Event: " + event + " Length: "+ len + " == ";
			if (len ==0 || len > 40) {
				s = s + "File is damaged, can not be parsed";
				break;
			}
						
//			if ( (argc>2) && !callmatch(argv[2]) )
//			continue;

			// This seems lazy but follows the C code
			int[] dataSet1 = Arrays.copyOfRange(data, i, 33+i);
			int[] dataSet2 = Arrays.copyOfRange(data, i, 40+i);
			alog_1f = new RecordAL1(0, 0, timeStamp, 0, dataSet1);
			alog_2f = new RecordAL2(0, 0, timeStamp, 0, dataSet2);

			i = i + len;
			
//			if (alog_1f->var3 != 0L)
//				err_str = search_for_str(ftl0_errors, (int) alog_1f->var3);
//			else
//				err_str = "";

			switch(event) {
			case ALOG_FTL0_STARTUP:
				/* FTL0 BBS code executed */
				s = s + alog_1f;
				//printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
			break;

			case ALOG_FTL0_SHUTDOWN:
				/* FTL0 BBS task exits. */
				s = s + alog_1f;
				//printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
			break;
			
			case ALOG_BBS_SHUT:
				/* Ground command shuts bbs */
				pcalla();
			break;

			case ALOG_BBS_OPEN:
				/* Ground command opens bbs */
				pcalla();
			break;

			case ALOG_BCAST_START:
				/* User starts a broadcast
					 var1 = file number
					 var2 = number of seconds for broadcast
					 var3 = error
					 var4 = length of data packets
				*/
				s = s + pcalla();
				String err_str = "";
				int var3 = alog_2f.getRawValue(RecordAL2.VAR3);
				if (var3 != 0)
				err_str = search_for_string(ftl0_errors, var3);
//				if (alog_2f->var3 != 0L)
//					err_str = search_for_str(ftl0_errors, (int) alog_2f->var3);
//				else
//					err_str = "";
				long id = alog_2f.getRawValue("var1");
				long dur = alog_2f.getRawValue("var2");
				long pkt_len = alog_2f.getRawValue("var4");
				s = s + "        File:" + Long.toHexString(id) + " " + dur + "s broadcast, " 
				+ "with Pkt Len:" + pkt_len + " " + err_str;
//				printf("        f#%lx dur:%lu l:%ld %s",alog_2f->var1,
//					alog_2f->var2,alog_2f->var4,err_str);
			break;

			case ALOG_BCAST_STOP:
				/* User stops a broadcast
					 var1 = file number
					 var2 = unused
					 var3 = error
				*/
				s = s + pcalla();
				err_str = "";
				var3 = alog_2f.getRawValue(RecordAL2.VAR3);
				if (var3 != 0)
				err_str = search_for_string(ftl0_errors,var3);
				id = alog_2f.getRawValue("var1");
				s = s + "    File:" + Long.toHexString(id) + " " + err_str;
//				printf("        f#%lx  %s",alog_2f->var1,err_str);
			break;

			case ALOG_DISKSPACE:
				/* Diskspace is logged hourly
				 	var1 = number of disk clusters free (1008 bytes of data each)
				 	var2 = number of directory entries free
				*/
				s = s + alog_1f;
				int var1 = alog_1f.getRawValue("var1");
				int var2 = alog_1f.getRawValue("var2");
				s = s + "                     " + var1*1008l + " bytes \\ " + var2 + " dirs";
//				printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
//				printf("                     %ld bytes \\",alog_1f->var1*1008l);
//				printf(" %ld dirs",alog_1f->var2);
			break;

			case ALOG_FILE_DELETE:
				/* Command station deletes file
					 var1 = file number
					 var2 = unused
					 var3 = error code
				*/
				pcalla();
				err_str = "";
				var3 = alog_1f.getRawValue("var3");
				if (var3 != 0)
				err_str = search_for_string(ftl0_errors, var3);
				id = alog_1f.getRawValue("var1");
				s = s + "    File:" + Long.toHexString(id) + " " + err_str;
//				printf("        f#%lx",alog_1f->var1);
			break;

			case ALOG_USER_REFUSED:
				s = s + alog_1f + " BBS was full";
//				printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
				/* BBS was full.
				*/
			break;

			case ALOG_START_SESSION :
				/* User logs on. This is the only time we get callsign
					 information for connected mode users; must use serial_no
					 from now on.
				*/
				s = s + pcallsession();
				int serial_no = alog_1f.getRawValue("serial_no");
				s = s + " SESSION: " + serial_no;
//				printf(" %06u",alog_1f->serial_no);
			break;

			case ALOG_CLOSE_SESSION :
				/* Server closes session.
					var1 = reason for closing session, see close_reasons array.
					var2 = bytes downloaded, if download in progress
					var3 = bytes uploaded, if upload in progress
					var2 and var3 are 0 if no transfer in progress.
					download includes directory commands.
				*/
				s = s + pcall();
				serial_no = alog_1f.getRawValue("serial_no");
				s = s + " SESSION: " + serial_no;
//				printf(" %06u",alog_1f->serial_no);
				
				s = s + search_for_string(close_reasons, alog_1f.getRawValue("var1"));
//				printf(" %s  ", search_for_str(close_reasons, (int) alog_1f->var1));
				int bytes = alog_1f.getRawValue("var2");
				if (bytes > 0)
					s = s + " --Incomplete D/L @ "+bytes+" bytes";
					//printf("\n%41.41s Incomplete D/L @ %lu bytes", "",alog_1f->var2);
				bytes = alog_1f.getRawValue("var3");
				if (bytes > 0)
					s = s + " --Incomplete U/L @ "+ bytes +" bytes";
					//printf("\n%41.41s Incomplete U/L @ %lu bytes","", alog_1f->var3);
			break;

			case ALOG_DISCONNECT :
				/* Server is disconnecting user.
					 var1 = reason for disconnect, see disc_reasons array.
				*/
				s = s + pcall();
				s = s + " " + search_for_string(disc_reasons, alog_1f.getRawValue("var1"));
				//printf(" %s  ", search_for_str(disc_reasons, (int) alog_1f->var1));
			break;
			

			case ALOG_FILE_DOWNLOAD:
				/* Server receives download command.
					 var1 = file number
					 var2 = continue offset
					 var3 = error code
					 var4 = flag indicating that download was from select list.
				*/
				s = s + pcall();
				var3 = alog_1f.getRawValue("var3");
				err_str = "";
				if (var3 != 0)
					err_str = search_for_string(ftl0_errors, var3);
				id = alog_1f.getRawValue("var1");
				s = s + "        File:" + Long.toHexString(id) + " " + err_str + " ";
//				printf("        f#%lx off:%lu %s",alog_1f->var1,
//					alog_1f->var2,err_str);
				if (alog_1f.getRawValue("var4") > 0)
					s = s + "(selected)";
//				if (alog_1f->var4)
//					printf("(selected)");
			break;

			case ALOG_END_DOWNLOAD:
				/* User has indicated end of download to server. May be good or bad.
					 var1 = number of bytes downloaded. Not necessarily file length,
					        if this was a continued download.
					 var2 = FTL_DL_ACK_CMD if user acked.
					        FTL_DL_NAK_CMD if user n'acked.
					 User may n'ack if checksum fails.
				*/
				s = s + pcall();
				s = s + "        " + alog_1f.getRawValue("var1") + " bytes";
//				printf("        %ld bytes ",alog_1f->var1);
				var2 = alog_1f.getRawValue("var2");
				if ((var2) == 0x0c)
					s = s + "Ack'd";
//					printf("Ack'd");
				else
					s = s + "Nak'd";
//					printf("Nak'd");
			break;

			case ALOG_FILE_UPLOAD:
				/* Server receives client's upload command.
					 var1 = file number
					 var2 = continue offset
					 var3 = error code
					 var4 = file length as told by client to server.
				*/
				s = s + pcall();
				var3 = alog_1f.getRawValue("var3");
				err_str = "";
				if (var3 != 0)
					err_str = search_for_string(ftl0_errors, var3);
				id = alog_1f.getRawValue("var1");
				s = s + "        File:" + Long.toHexString(id) + " Offset:" + alog_1f.getRawValue("var2")
				+ " Length:" + alog_1f.getRawValue("var4") + " " + err_str;
//				printf("        f#%lx off:%lu l#%lu %s",alog_1f->var1,
//					alog_1f->var2,alog_1f->var4,err_str);
			break;

			case ALOG_END_UPLOAD:
				/* Server has received, and finished checksumming, the file.
					 var1 = time checksum started. Subtractr from alog_1f->time
					        to figure out how many seconds to checksum the file.
					 var2 = error code.
					 var3 = number of bytes uploaded; not necessarily same as file size,
					        since this might be a continued upload.
				*/
				s = s + pcall();
				s = s + "        " + alog_1f.getRawValue("var3") + " bytes " + (alog_1f.getRawValue("tstamp")-alog_1f.getRawValue("var3")) 
						+ " seconds cksum";
				//printf("        %ld bytes %ld seconds cksum",alog_1f->var3,
				//	alog_1f->tstamp - alog_1f->var1);
				var2 = alog_1f.getRawValue("var2");
				if ((var2) > 0)
					s = s + "Nak'd";
//				if (alog_1f->var2)
//					printf(" Nak'd");
			break;

			case ALOG_DIR:
				/* Server receives directory request from client.
					 var1 = file number (0xffffffff or 0 indicate dir from selection.)
					 var2 = error code.
				*/
				s = s + pcall();
				var3 = alog_1f.getRawValue("var3");
				err_str = "";
				if (var3 != 0)
					err_str = search_for_string(ftl0_errors, var3);
				id = alog_1f.getRawValue("var1");

				if ((id != 0xffffffff) && id != 0L) 
					s = s + "        File:" + Long.toHexString(id) + " " + err_str;
//				if ((alog_1f->var1 != 0xffffffff) && (alog_1f->var1 != 0L))
//					printf("        f#%lx  %s",alog_1f->var1,err_str);
				else
					s = s + "        from selection.";
//					printf("        from selection.");
			break;

			case ALOG_END_DIR:
				/* Server sends last packet of directory to client.
					 var1 = total number of bytes in directory.
			  */
					s = s + pcall();
					s = s + "        " + alog_1f.getRawValue("var1") + " bytes";
//					printf("        %ld bytes",alog_1f->var1);
			break;

				/* Automatic file removal
					 var1 = file number
					 var2 = free directory entries in system
					 var3 = free file clusters in system
					 var4 = expiry time (earliest at which file could be deleted)
				*/
			case ALOG_FILE_REMOVED:
				s = s + alog_1f;
//				printf("%s  %-9.9s  ",short_time(alog_1f->tstamp), event_text[alog_1f->event]);
				
				// add the var1 etc
				id = alog_1f.getRawValue("var1");
				long dt = alog_1f.getRawValue("var4");
				Date date = new Date(dt*1000);
				s = s + "        File:" + Long.toHexString(id) + " (exp: (" + Long.toHexString(dt) + ") " + PacSatField.getDateStringSecs(date);;
//				printf("        f#%lx (exp: (%lx) %23.23s)",alog_1f->var1,alog_1f->var4,
//					asctime(gmtime(&(alog_1f->var4))) );
			break;

			case ALOG_SELECT:
			/* Select command received by server. No vars used */
				s = s + pcall();
			break;

			case ALOG_SELECT_DONE:
//			/* Finished building select list
//				 var1 = length of user's selection equation
//				 var2 = oldest file considered for the selection
//				 var3 = newest file considered for the selection
//				 var4 = number of files selected
//			*/
				s = s + pcall();
				s = s + " Len of select Equ:" + alog_1f.getRawValue("var1") 
				+ " oldest file:" + alog_1f.getRawValue("var2") + " newest file:" + alog_1f.getRawValue("var3") + " num selected:" + alog_1f.getRawValue("var4");
//				if (alog_1f->var2 == 0x131d1741) 
//					strcpy(tmptxt1,"start");
//				else {
//					/* compute time before "now" */
//					tmpl = alog_1f->tstamp - alog_1f->var2;
//					if (tmpl < 0)
//						strcpy(tmptxt1,"future?");
//					else {
//						day = tmpl/msday;
//						tmpl= tmpl % msday;
//						hour = tmpl/mshour;
//						tmpl = tmpl % mshour;
//						min = tmpl/msmin;
//						tmpl = tmpl % msmin;
//						sec = tmpl;
//						sprintf(tmptxt1,"%03u/%02u:%02u:%02u",
//							day,hour,min,sec);
//					}
//				}
//				printf("        %s len:%ld selected:%ld",
//					tmptxt1, alog_1f->var1, alog_1f->var4);
			break;

			default:
				s = s + alog_1f; // unknown event.  Print Timestamp and event number
			break;
			}
			s = s + "\n";
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
		s = s + alog_1f.getTimeStamp() + "\t" + alog_1f.getEventString() + "\t";
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
