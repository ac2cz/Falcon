package passControl;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.DirHole;
import fileStore.FileHole;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;
import fileStore.SortedArrayList;
import gui.MainWindow;
import jssc.SerialPortException;
import pacSat.TncDecoder;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFileFrame;
import pacSat.frames.CmdFrame;
import pacSat.frames.PacSatEvent;
import pacSat.frames.PacSatFrame;
import pacSat.frames.PacSatPrimative;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.RequestFileFrame;
import pacSat.frames.ResponseFrame;
import pacSat.frames.StatusFrame;
import pacSat.frames.TlmFrame;
import pacSat.frames.TlmMirSatFrame;
import pacSat.frames.TlmPacsatFrame;

/**
 * 
 * @author chris
 *
 *
 * Hold the status of the downlink and support automation when user is not online.
 * If the user is online then queue requests and handle them as soon as possible.
 * if the user request is a file UPLOAD then it is handled by the Uplink State Machine.  All other
 * requests, transmissions and downloads are handled here
 * 
 * This is event driven updating when we receive a new frame or when it generates its own internal event.  It runs in a 
 * thread that times out if we have not heard the spacecraft in a certain time and that
 * waits for the result of commands.  If nothing is heard, then we time out.
 * 
 */
public class DownlinkStateMachine extends PacsatStateMachine implements Runnable {

	// These are the states of the State Machine
	public static final int DL_LISTEN = 0; // Not heard the spacecraft yet
	public static final int DL_PB_OPEN = 1; // We heard it and the PB is Empty or has a list that does not include us
	public static final int DL_ON_PB = 2; // We are on the PB
	public static final int DL_WAIT = 3; // We are waiting for the result of a command we sent
	public static final int DL_PB_FULL = 4; // PB is full, we need to wait
	public static final int DL_PB_SHUT = 5; // PB is shut, we need to wait
	
	public static final int LOOP_TIME = 1; // length of time in ms to process respones
		
	boolean needDir = true;
	Date lastChecked = null;
	public static final int DIR_CHECK_INTERVAL = 60; // mins between directory checks;
	public static final int TIMER_T4 = 60*1000; // 1 min - milli seconds for T4 - Reset State/PB if we have not heard the spacecraft
	//Timer t4_timer; 
	int t4_timer;
	
	int bytesAtLastStatus = 0;

			
	public static final String[] states = {
			"Listening",
			"PB Avail",
			"ON PB",
			"Waiting",
			"PB Full",
			"PB Shut"
	};
	
	String pbList = "";
//	String pgList = "";
	
	/**
	 * Construct a new Downlink State machine for the named pacsat and initialize it to listen for passes
	 * @param sat
	 */
	public DownlinkStateMachine(SpacecraftSettings sat) {
		super(sat);
		state = DL_LISTEN;
	}
	
	public void setSpacecraft(SpacecraftSettings spacecraftSettings) {
		// TODO - drain and process the event list before switching
		spacecraft = spacecraftSettings;
	}
	
	/**
	 * Add a new frame of data from the spacecraft to the event queue
	 */
	public void processEvent(PacSatPrimative frame) {
		DEBUG("Adding DOWN LINK Event: " + frame.toString());
		frameEventQueue.add(frame);
	}

	protected void nextState(PacSatPrimative prim) {
		if (!Config.getBoolean(Config.DOWNLINK_ENABLED)) {
			state = DL_LISTEN;
			return;
		}
		// Special cases for Frames that are state independant
		// This prevents them being repeated in every state
		if (prim instanceof PacSatFrame) {
			PacSatFrame frame = (PacSatFrame)prim;
			switch (frame.frameType) {
			case PacSatFrame.PSF_STATUS_BYTES:
				String bytes =  Ax25Frame.makeString(frame.getBytes());
				int by = ((StatusFrame)frame).getStatusBytesCount();
				//System.err.println("BYTES: " + by);
				if (bytesAtLastStatus != 0) {
					int bytesSentBySpacecraft = by - bytesAtLastStatus;
					//System.err.println("BYTES Sent: " + bytesSentBySpacecraft + " RECEIVED: " + bytesReceivedSinceStatus);
					//System.err.println("EFF: " + bytesReceivedSinceStatus/(double)bytesSentBySpacecraft);
					Config.mainWindow.setEfficiency(spacecraft.name, bytesSentBySpacecraft, ((StatusFrame)frame).bytesReceivedOnGround);
				}
				bytesAtLastStatus = by;
				startT4();
				break;

			case PacSatFrame.PSF_STATUS_PBFULL:
				state = DL_PB_FULL;
				startT4();
				pbList =  Ax25Frame.makeString(frame.getBytes());
				if (MainWindow.frame != null)
					MainWindow.setPBStatus(spacecraft.name, pbList);
				break;
			case PacSatFrame.PSF_STATUS_PBSHUT:
				state = DL_PB_SHUT;
				startT4();
				pbList =  Ax25Frame.makeString(frame.getBytes());
				if (MainWindow.frame != null)
					MainWindow.setPBStatus(spacecraft.name, pbList);
				break;
				
			case PacSatFrame.PSF_BROADCAST_DIR:
				BroadcastDirFrame bd = (BroadcastDirFrame)frame;
				processBroadcastDir(bd);
				break;
				
			case PacSatFrame.PSF_BROADCAST_FILE:
				BroadcastFileFrame bf = (BroadcastFileFrame)frame;
				processBroadcastFile(bf);
				break;
				
			case PacSatFrame.PSF_TLM_MIR_SAT_1:
				TlmMirSatFrame tlmmir = (TlmMirSatFrame)frame;
				if (tlmmir.record != null) 
					processTelem(tlmmir.record);
				if (tlmmir.record1 != null) 
					processTelem(tlmmir.record1);
				if (tlmmir.record2 != null) 
					processTelem(tlmmir.record2);
				break;
			case PacSatFrame.PSF_TLM_PACSAT:
				TlmPacsatFrame tlmp = (TlmPacsatFrame)frame;
				processTelem(tlmp.record);
				break;
			case PacSatFrame.PSF_TLM:
				TlmFrame tlm = (TlmFrame)frame;
				processTelem(tlm.record);
				break;
				

			default:
				break;
			}


			switch (state) {
			case DL_LISTEN:
				stateInit(frame);
				break;

			case DL_PB_OPEN:
				statePbOpen(frame);
				break;

			case DL_ON_PB:
				stateOnPb(frame);
				break;

			case DL_WAIT:
				stateWait(frame);
				break;

			case DL_PB_FULL:
				stateInit(frame);
				break;

			case DL_PB_SHUT:
				stateInit(frame);
				break;

			default:
				break;
			}
		}
	}

	/**
	 * We are not in a pass or we lost the signal during a pass.  Waiting for the spacecraft
	 * @param event
	 */
	private void stateInit(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_PBLIST:
			startT4();
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList = Ax25Frame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(spacecraft.name, pbList);
			break;
			
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response when we don't think we are in a pass, we ignore this
			startT4();
			waitTimer = 0;
			retries = 0;
			lastCommand = null;
			break;
		case PacSatFrame.PSF_RESPONSE_ERROR: // we have an ERR response when we don't think we are in a pass, we ignore this
			startT4();
			waitTimer = 0;
			retries = 0;
			lastCommand = null;
			break;
			
		case PacSatFrame.PSF_REQ_DIR:
			startT4();
			RequestDirFrame dirFrame = (RequestDirFrame)frame;
			KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, dirFrame.getBytes());
			PRINT("TX: " + dirFrame.toShortString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = dirFrame;
				tncDecoder.sendFrame(kss.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				PRINT("Nothing was transmitted as no TNC is connected\n ");
			}
			//Log.infoDialog("Ignored", "Wait until the Spacecraft PB status has been heard before requesting a directory");
			break;
		case PacSatFrame.PSF_REQ_FILE:
			startT4();
			RequestFileFrame fileFrame = (RequestFileFrame)frame;
			KissFrame kssFile = new KissFrame(0, KissFrame.DATA_FRAME, fileFrame.getBytes());
			PRINT("DL SENDING: " + fileFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = fileFrame;
				tncDecoder.sendFrame(kssFile.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				PRINT("Nothing was transmitted as no TNC is connected\n ");
			}
			//Log.infoDialog("Ignored", "Wait until the Spacecraft PB status has been heard before requesting a file");
			break;
		
		case PacSatFrame.PSF_COMMAND:
			startT4();
			CmdFrame cmdFrame = (CmdFrame)frame;
			KissFrame kssCmd = new KissFrame(0, KissFrame.DATA_FRAME, cmdFrame.getBytes());
			PRINT("TX: " + cmdFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = cmdFrame;
				tncDecoder.sendFrame(kssCmd.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				PRINT("Nothing was transmitted as no TNC is connected\n ");
			}
			break;
		}
		
	}
	
	private void statePbOpen(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_REQ_DIR:
			startT4();
			RequestDirFrame dirFrame = (RequestDirFrame)frame;
			KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, dirFrame.getBytes());
			PRINT("TX: " + dirFrame.toShortString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = dirFrame;
				tncDecoder.sendFrame(kss.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				PRINT("Nothing was transmitted as no TNC is connected\n ");
			}
			break;

		case PacSatFrame.PSF_REQ_FILE:
			startT4();
			RequestFileFrame fileFrame = (RequestFileFrame)frame;
			KissFrame kssFile = new KissFrame(0, KissFrame.DATA_FRAME, fileFrame.getBytes());
			PRINT("DL SENDING: " + fileFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = fileFrame;
				tncDecoder.sendFrame(kssFile.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				PRINT("Nothing was transmitted as no TNC is connected\n ");
			}
			break;
			
		case PacSatFrame.PSF_COMMAND:
			startT4();
			CmdFrame cmdFrame = (CmdFrame)frame;
			KissFrame kssCmd = new KissFrame(0, KissFrame.DATA_FRAME, cmdFrame.getBytes());
			PRINT("TX: " + cmdFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = cmdFrame;
				tncDecoder.sendFrame(kssCmd.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				PRINT("Nothing was transmitted as no TNC is connected\n ");
			}
			break;	
	
		case PacSatFrame.PSF_STATUS_PBLIST:
			startT4();
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList =  Ax25Frame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(spacecraft.name, pbList);
			break;
			
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we must now be on the PB
			startT4();
			state = DL_ON_PB;
			lastCommand = null;
			retries = 0;
			break;
			
		default:
			break;
		}
	}
	
	private void stateOnPb(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_REQ_DIR: // Requested a DIR but we are already on the PB, so ignore
//			Log.infoDialog("Ignored", "Wait until your current PB ssession has completed before requesting another directory");
			PRINT("Ignored DIR REQ: Wait until your current PB ssession has completed before requesting another directory");
			break;
		case PacSatFrame.PSF_REQ_FILE: // Requested a FILE but we are already on the PB, so ignore
			PRINT("Ignored FILE REQ: Wait until your current PB ssession has completed before requesting a file");
//			Log.infoDialog("Ignored", "Wait until your current PB ssession has completed before requesting another directory");
			break;
		case PacSatFrame.PSF_STATUS_PBLIST:
			startT4();
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList =  Ax25Frame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(spacecraft.name, pbList);
			break;
		}
		
		switch (frame.frameType) {
		case PacSatFrame.PSF_COMMAND:
			startT4();
			CmdFrame cmdFrame = (CmdFrame)frame;
			KissFrame kssCmd = new KissFrame(0, KissFrame.DATA_FRAME, cmdFrame.getBytes());
			PRINT("TX: " + cmdFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = cmdFrame;
				tncDecoder.sendFrame(kssCmd.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				PRINT("Nothing was transmitted as no TNC is connected\n ");
			}
			break;	

		}	
	}

	private void stateWait(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we stop sending command
			startT4();
			state = DL_ON_PB;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			break;
			
		case PacSatFrame.PSF_RESPONSE_ERROR: // we have an ERR response, this is echoed to the screen, tell user.  Abandon automated action!
			startT4();
			ResponseFrame sf = (ResponseFrame)frame;
			if (sf.getErrorCode() == ResponseFrame.FILE_MISSING ||
					sf.getErrorCode() == ResponseFrame.FILE_MARKED_NOT_TO_DOWNLOAD) {
				if (lastCommand.frameType == PacSatFrame.PSF_REQ_FILE) {
					RequestFileFrame rf = (RequestFileFrame)lastCommand;
					// we are requesting a file that does not exist on the server
					// Mark it to no longer be downloaded
					// This should not call the GUI directly!!  Update the directory.
					//Config.mainWindow.dirPanel.setPriority(rf.fileId, -2);
					spacecraft.directory.setPriority(rf.fileId, sf.getErrorCode());
					String[][] data = spacecraft.directory.getTableData();
					if (data.length > 0)
						if (Config.mainWindow != null)
							MainWindow.setDirectoryData(spacecraft.name, data);
				}
			} else if (	sf.getErrorCode() == ResponseFrame.FREQUENCY_SWITCHING_ON_UPLOAD) {
				// requesting a file that is temporarily not available
				// We will abandon the action but we do not mark the file as unavailable
			}
			state = DL_LISTEN;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			
			break;
			
		case PacSatFrame.PSF_STATUS_PBLIST:
			startT4();
			if (((StatusFrame)frame).containsCall()) { // looks like we missed the OK response, stop sending
				state = DL_ON_PB;
				pbList =  Ax25Frame.makeString(frame.getBytes());
			} else {
				// we dont change state, stay in WAIT
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			MainWindow.setPBStatus(spacecraft.name, pbList);
			break;
			
			/////// NEED LOGIC HERE TO SEE IF COMMAND IS BEING EXECUTED BUT WE MISSED THE RESPONSES.  e.g. DO WE GET
			/// PARTS OF A FILE WE REQUESTED.  IS THERE A DIR BROADCAST FOR PARTS WE NEED?  NOT PERFECT BUT NICE FOR
			// CHANNEL CAPACITY
			
		default:
			break;
		}
	}
	
	/**
	 * Decide if we need a directory.  
	 * How fresh is our data?  
	 *     If we have no dir headers for 60 minutes then assume this is a new pass and ask for headers
	 *     
	 * Do we have holes? 
	 *     
	 * 
	 * @return
	 */
	public boolean needDir() {
		if (lastChecked == null) {
			lastChecked = new Date();
			PRINT("First pass since starting. Requesting dir ..");
			return true;
		}
		
		// Otherwise we get the timestamp of the last time we checked
		// If it is some time ago then we ask for another directory
		Date timeNow = new Date();
		long minsNow = timeNow.getTime() / 60000;
		long minsLatest = lastChecked.getTime() / 60000;
		long diff = minsNow - minsLatest;
		if (diff > DIR_CHECK_INTERVAL) {
			PRINT("Have not checked for an hour or more. Requesting dir ..");
			lastChecked = new Date();
			return true;
		}
		return false;
	}
	
	private void processBroadcastFile(BroadcastFileFrame bf ) {
		if (spacecraft == null)
			Log.errorDialog("ERROR", " No Spacecraft for file chunk for: " + bf +"\n");
		String s = "";
		boolean updated = false;
		try {
			updated = spacecraft.directory.add(bf);
		} catch (NumberFormatException e) {
			s = "ERROR: Number Format issue with telemetry " + e.getMessage();
		} catch (com.g0kla.telem.data.LayoutLoadException e) {
			s = "ERROR: Opening Layout " + e.getMessage();
		} catch (DataLoadException e) {
			s = "ERROR: Loading Data " + e.getMessage();
		} catch (IOException e) {
			if (bf != null) {
				s = "ERROR: Writing received file chunk for: " + bf +"\n" + e.getMessage();
				PRINT(s);
			}
		} catch (MalformedPfhException e) {
			if (bf != null ) {
				s = "ERROR: Bad PFH - " + e.getMessage() + ": " + bf.toString();
				DEBUG(s);
			}
		}
		if (updated) {
			String[][] data = spacecraft.directory.getTableData();
			if (data.length > 0)
				if (Config.mainWindow != null)
					MainWindow.setDirectoryData(spacecraft.name, data);
		}
	}
	
	private void processBroadcastDir(BroadcastDirFrame bd) {
		if (spacecraft == null)
			Log.errorDialog("ERROR", " No Spacecraft for file chunk for: " + bd +"\n");
		String s = bd.toString();
		boolean updated = false;
		try {
			updated = spacecraft.directory.add(bd);
		} catch (IOException e) {
			if (bd != null) {
				s = "ERROR: Writing received file chunk for: " + bd +"\n" + e.getMessage();
				PRINT(s);
			}
		}
		if (updated) {
			String[][] data = spacecraft.directory.getTableData();
			if (data.length > 0)
				if (Config.mainWindow != null && spacecraft != null)
					Config.mainWindow.setDirectoryData(spacecraft.name, data);
		}
	}
	
	private void processTelem(DataRecord tlm) {
		try {
			spacecraft.db.add(tlm);
			PRINT("TELEMETRY FRAME: " + tlm.resets +":"+ tlm.uptime + " Type: " + tlm.type );
			if (Config.getBoolean(Config.DEBUG_TELEM)) {
				String s = tlm.toString();
				PRINT(s);
			}
			
		} catch (NumberFormatException e) {
			PRINT("ERROR: Number parse for: " + tlm +" " + e.getMessage());
		} catch (DataLoadException e) {
			PRINT("ERROR: Loading data for: " + tlm +" " + e.getMessage());
		} catch (IOException e) {
			PRINT("ERROR: Writing received file chunk for: " + tlm +"\n" + e.getMessage());
		}		
	}

	public void stopRunning() {
		running = false;
	}
	
	private void DEBUG(String s) {
		s = "DEBUG DL: " + states[state] + ": " + s;

		if (Config.getBoolean(Config.DEBUG_DOWNLINK)) {
			if (ta != null)
				ta.append(s + "\n");
			Log.println(s);
		}
	}
	
	private void PRINT(String s) {
		if (ta != null)
			ta.append(s + "\n");
		Log.println(s);
	}
	
	private void startT4() {
		t4_timer = 1;
		
//		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1); 
//		ses.schedule(new Runnable() {
//		    @Override
//		    public void run() {
//				Thread.currentThread().setName("Timer T4");
//		        Config.spacecraftSettings.directory.saveIfNeeded();
//		        
//		        t4_timer = TIMER_T4; // mark as complete
//		    }
//		}, TIMER_T4, TimeUnit.SECONDS);  
		
	}

	private void stopT4() {
		t4_timer = 0;
	}

	@Override
	public void run() {
		DEBUG("STARTING DL Thread");
		Thread.currentThread().setName("DownlinkStateMachine: " + spacecraft.name);

		while (running) {
			if (t4_timer > 0) {
				// we are timing something
				t4_timer++;
				if (t4_timer > TIMER_T4) {
					t4_timer = 0;
					DEBUG("Downlink T4 expired - back to listening");
//					nextState(new PacSatEvent(PacSatEvent.UL_TIMER_T3_EXPIRY));
					state = DL_LISTEN;
					retries = 0;
					lastCommand = null;
				}				
			}
			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
			} else if (state == DL_WAIT) {
				waitTimer++;
				if (waitTimer * LOOP_TIME >= WAIT_TIME) {
					waitTimer = 0;
					retries++;
					if (retries > MAX_RETRIES) {
						state = DL_LISTEN; // end the wait state.  Assume we lost the spacecraft.  Listen again. Wait to see PB Status.
						if (lastCommand instanceof RequestDirFrame)
							lastChecked = null; // we failed with our DIR request, so try this again next time spaceacraft avail
						retries = 0;
					} else {
						// retry the command
						state = DL_PB_OPEN;
						processEvent(lastCommand);
					}
				}
			} else if (state == DL_PB_OPEN) {
				// Here we decide if we should request the DIR or a FILE depending on the status of the Directory
				// The PB must be open and we must need one or the other according to the Directory

				if (!Config.getBoolean(Config.TX_INHIBIT)) {
					if (spacecraft.getBoolean(SpacecraftSettings.REQ_DIRECTORY) && needDir()) {
						SortedArrayList<DirHole> holes = spacecraft.directory.getHolesList();
						if (holes != null) {
							DEBUG("Requesting "+ holes.size() +" holes for directory");
							RequestDirFrame dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), spacecraft.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, holes);
							processEvent(dirFrame);
						} else {
							Log.errorDialog("ERROR", "Something has gone wrong and the directory holes file is missing or corrupt\nCan't request the directory\n");
						}
					} else if (spacecraft.getBoolean(SpacecraftSettings.REQ_FILES) && spacecraft.directory.needFile()) {
						long fileId = spacecraft.directory.getMostUrgentFile();
						if (fileId != 0) {
							PacSatFile pf = new PacSatFile(spacecraft, spacecraft.directory.dirFolder, fileId);
							SortedArrayList<FileHole> holes = pf.getHolesList();
							PRINT("Requesting file " + Long.toHexString(fileId));
							RequestFileFrame fileFrame = new RequestFileFrame(Config.get(Config.CALLSIGN), spacecraft.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, fileId, holes);
							spacecraft.downlink.processEvent(fileFrame);
						}	
					} else if (spacecraft.getBoolean(SpacecraftSettings.FILL_DIRECTORY_HOLES) && spacecraft.directory.hasHoles()) {
						SortedArrayList<DirHole> holes = spacecraft.directory.getHolesList();
						if (holes != null) {
							DEBUG("We have dir holes. Requesting dir ..");
							DEBUG("Requesting "+ holes.size() +" holes for directory");
							RequestDirFrame dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), spacecraft.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, holes);
							processEvent(dirFrame);
						} else {
							Log.errorDialog("ERROR", "Something has gone wrong and the directory holes file is missing or corrupt\nCan't request the directory\n");
						}
					}
				}
			}
			try {
				Thread.sleep(LOOP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Config.mainWindow != null)
				Config.mainWindow.setDownlinkStatus(spacecraft.name, states[state]);
		}
		DEBUG("EXIT DL Thread");
	}
	
}
