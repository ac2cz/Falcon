package passControl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
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
import fileStore.SortedArrayList;
import gui.MainWindow;
import pacSat.TncDecoder;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFileFrame;
import pacSat.frames.CmdFrame;
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
 *
 * REFACTOR NOTES (structural only, behaviour preserved):
 *   - Inbound spacecraft frames that are handled the same way in every state
 *     (telemetry, broadcasts, status bytes, PB full/shut) live in
 *     handleStateIndependent().
 *   - Outbound COMMANDS (REQ_DIR / REQ_FILE / CMD) are no longer duplicated inside
 *     each state.  They are handled once in handleCommand() and transmitted by
 *     sendCommand().  Commands can be issued from (almost) any state; the only
 *     state-specific behaviour that remains is the ON_PB guard (don't re-ask for a
 *     dir/file while we're already on the PB) and the WAIT guard (see FUTURE QUEUE).
 *   - The run() loop is split into tickT4() / serviceWaitState() / servicePbOpen(),
 *     and the command-station-key branching is collapsed into buildDirRequest() /
 *     buildFileRequest().
 *
 * FUTURE QUEUE: the natural seam for the "commands auto-sent at AOS" queue is
 *   servicePbOpen() (drain the queue there before auto-requesting a dir/file) plus
 *   handleCommand() (re-queue instead of drop in WAIT / ignore in ON_PB).
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
	public boolean openForCommandStationsOnly = false;
	
	public static final int LOOP_TIME = 1; // length of time in ms to process respones
		
	boolean needDir = true;
	Date lastChecked = null;
	public static final int DIR_CHECK_INTERVAL = 15; // mins between directory checks, designed to check each pass;
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
		if (frame == null) return;
		DEBUG("Adding DOWN LINK Event: " + frame.toString());
		frameEventQueue.add(frame);
	}

	protected void nextState(PacSatPrimative prim) {
		if (!Config.getBoolean(Config.DOWNLINK_ENABLED)) {
			state = DL_LISTEN;
			return;
		}
		if (!(prim instanceof PacSatFrame)) return;
		PacSatFrame frame = (PacSatFrame) prim;

		// 1) Frames that are handled identically in every state.
		//    (Some of these set the state; we still fall through to the state switch,
		//     exactly as the original did.)
		handleStateIndependent(frame);

		// 2) Commands can be sent from (almost) any state - pulled out of the states.
		if (handleCommand(frame)) return;

		// 3) State-dependent inbound frames.
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

	/**
	 * Frames whose handling does not depend on the current state: telemetry,
	 * directory/file broadcasts, status-byte accounting and PB full/shut.
	 */
	private void handleStateIndependent(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_BYTES:
			int by = ((StatusFrame) frame).getStatusBytesCount();
			if (bytesAtLastStatus != 0) {
				int bytesSentBySpacecraft = by - bytesAtLastStatus;
				Config.mainWindow.setEfficiency(spacecraft.name, bytesSentBySpacecraft, ((StatusFrame) frame).bytesReceivedOnGround);
			}
			bytesAtLastStatus = by;
			startT4();
			break;

		case PacSatFrame.PSF_STATUS_PBFULL:
			state = DL_PB_FULL;
			startT4();
			pbList = Ax25Frame.makeString(frame.getBytes());
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(spacecraft.name, pbList);
			break;
		case PacSatFrame.PSF_STATUS_PBSHUT:
			state = DL_PB_SHUT;
			startT4();
			pbList = Ax25Frame.makeString(frame.getBytes());
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(spacecraft.name, pbList);
			break;

		case PacSatFrame.PSF_BROADCAST_DIR:
			processBroadcastDir((BroadcastDirFrame) frame);
			break;

		case PacSatFrame.PSF_BROADCAST_FILE:
			processBroadcastFile((BroadcastFileFrame) frame);
			break;

		case PacSatFrame.PSF_TLM_MIR_SAT_1:
			TlmMirSatFrame tlmmir = (TlmMirSatFrame) frame;
			if (tlmmir.record != null)
				processTelem(tlmmir.record);
			if (tlmmir.record1 != null)
				processTelem(tlmmir.record1);
			if (tlmmir.record2 != null)
				processTelem(tlmmir.record2);
			break;
		case PacSatFrame.PSF_TLM_PACSAT:
			processTelem(((TlmPacsatFrame) frame).record);
			break;
		case PacSatFrame.PSF_TLM:
			processTelem(((TlmFrame) frame).record);
			break;

		default:
			break;
		}
	}

	/**
	 * Handle a frame that represents a command WE want to transmit (a directory
	 * request, a file request or a command frame).  Returns true if the frame was a
	 * command and has been dealt with here (so nextState should stop).
	 *
	 * State-specific behaviour that is preserved from the original:
	 *   - WAIT  : we already have a command in flight, so a new one is dropped.
	 *   - ON_PB : a dir/file request is ignored (we're already on the PB); a plain
	 *             command is still sent.
	 *   - all other states (LISTEN, PB_OPEN, PB_FULL, PB_SHUT): transmit.
	 */
	private boolean handleCommand(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_REQ_DIR:
			if (state == DL_WAIT) return true; // command already in flight - drop  TODO - could re-queue in the future
			if (state == DL_ON_PB) {
				PRINT("Ignored DIR REQ: Wait until your current PB ssession has completed before requesting another directory");
				return true;
			}
			sendCommand(frame);
			return true;

		case PacSatFrame.PSF_REQ_FILE:
			if (state == DL_WAIT) return true; // command already in flight - drop
			if (state == DL_ON_PB) {
				PRINT("Ignored FILE REQ: Wait until your current PB ssession has completed before requesting a file");
				return true;
			}
			sendCommand(frame);
			return true;

		case PacSatFrame.PSF_COMMAND:
			if (state == DL_WAIT) return true; // command already in flight - drop
			sendCommand(frame);
			return true;

		default:
			return false; // not a command
		}
	}

	/**
	 * Transmit a command to the spacecraft and enter DL_WAIT so we pick up the
	 * OK/ERR response.  If no TNC is connected nothing is transmitted and we do not
	 * change state.
	 */
	private void sendCommand(PacSatFrame frame) {
		startT4();
		KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, frame.getBytes());
		PRINT(txLogLine(frame) + " ... ");
		if (tncDecoder != null) {
			state = DL_WAIT;
			waitTimer = 0;
			lastCommand = frame;
			tncDecoder.sendFrame(kss.getDataBytes(), TncDecoder.NOT_EXPEDITED);
		} else {
			PRINT("Nothing was transmitted as no TNC is connected\n ");
		}
	}

	/** The console line printed as a command is transmitted (unchanged text per type). */
	private String txLogLine(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_REQ_DIR:  return "TX: " + ((RequestDirFrame) frame).toShortString();
		case PacSatFrame.PSF_REQ_FILE: return "DL SENDING: " + frame.toString();
		default:                       return "TX: " + frame.toString();
		}
	}

	private void setPbStatus(PacSatFrame frame) {
		startT4();
		pbList = Ax25Frame.makeString(frame.getBytes());
		
		if (((StatusFrame)frame).containsCall()) {
			state = DL_ON_PB;
		} else {
			state = DL_PB_OPEN;
		}
		if (((StatusFrame)frame).uiFrame.toCallsign.startsWith(StatusFrame.PBCOM)) {
			openForCommandStationsOnly = true;
			if (!spacecraft.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) {
				state = DL_PB_SHUT;
			}
		} else {
			openForCommandStationsOnly = false;
		}
		if (MainWindow.frame != null)
			MainWindow.setPBStatus(spacecraft.name, pbList);
		
	}
	
	/**
	 * We are not in a pass or we lost the signal during a pass.  Waiting for the spacecraft.
	 * (Command frames are no longer handled here - see handleCommand().)
	 */
	private void stateInit(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_PBLIST:
			setPbStatus(frame);
			break;

		case PacSatFrame.PSF_COMMAND_STOP:
			retries = 0;
			lastCommand = null;
			state = DL_LISTEN;
			break;

		case PacSatFrame.PSF_CMD_RESPONSE_OK:
		case PacSatFrame.PSF_CMD_RESPONSE_ERROR:
		case PacSatFrame.PSF_CMD_RESPONSE_OK_OTHER:
		case PacSatFrame.PSF_RESPONSE_OK:    // OK response when we don't think we are in a pass - ignore
		case PacSatFrame.PSF_RESPONSE_ERROR: // ERR response when we don't think we are in a pass - ignore
			startT4();
			waitTimer = 0;
			retries = 0;
			lastCommand = null;
			break;

		default:
			break;
		}
	}
	
	private void statePbOpen(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_PBLIST:
			setPbStatus(frame);
			break;
		case PacSatFrame.PSF_CMD_RESPONSE_OK:
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we must now be on the PB
			startT4();
			state = DL_ON_PB;
			lastCommand = null;
			retries = 0;
			break;

		case PacSatFrame.PSF_COMMAND_STOP:
			state = DL_LISTEN;
			lastCommand = null;
			retries = 0;
			break;

		default:
			break;
		}
	}
	
	private void stateOnPb(PacSatFrame frame) {
		if (frame.frameType == PacSatFrame.PSF_STATUS_PBLIST)
			setPbStatus(frame);
	}

	private void stateWait(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_COMMAND_STOP:
			state = DL_LISTEN;
			lastCommand = null;
			retries = 0;
			break;
		case PacSatFrame.PSF_CMD_RESPONSE_OK:
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we stop sending command
			startT4();
			state = DL_ON_PB;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			break;
			
		case PacSatFrame.PSF_CMD_RESPONSE_ERROR:
		case PacSatFrame.PSF_CMD_RESPONSE_OK_OTHER:
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
			} else if (	sf.getErrorCode() == ResponseFrame.TEMPORARY_ERROR) {
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
			pbList = Ax25Frame.makeString(frame.getBytes());
			if (((StatusFrame)frame).containsCall()) { // looks like we missed the OK response, stop sending
				state = DL_ON_PB;
			} else {
				// we don't change state, stay in WAIT
			}
			if (((StatusFrame)frame).uiFrame.toCallsign.startsWith(StatusFrame.PBCOM)) {
				openForCommandStationsOnly = true;
				if (!spacecraft.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) {
					state = DL_PB_SHUT;
				}
			} 
			if (MainWindow.frame != null)
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
			PRINT("Have not checked recently. Requesting dir ..");
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
					MainWindow.setDirectoryData(spacecraft.name, data);
		}
	}
	
	private void processTelem(DataRecord tlm) {
		try {
			spacecraft.db.add(tlm);
			PRINT("TELEM FRAME: " + tlm.resets +":"+ tlm.uptime + " Type: " + tlm.layout.name + " - " + tlm.type );
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
	}

	private void stopT4() {
		t4_timer = 0;
	}

	// ---------------------------------------------------------------------------
	// run() loop, broken into named steps
	// ---------------------------------------------------------------------------

	@Override
	public void run() {
		DEBUG("STARTING DL Thread");
		Thread.currentThread().setName("DownlinkStateMachine: " + spacecraft.name);

		while (running) {
			tickT4();

			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
			} else if (state == DL_WAIT) {
				serviceWaitState();
			} else if (state == DL_PB_OPEN) {
				servicePbOpen();
			}

			try {
				Thread.sleep(LOOP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Config.mainWindow != null)
				MainWindow.setDownlinkStatus(spacecraft.name, states[state]);
		}
		DEBUG("EXIT DL Thread");
	}

	/** Advance the T4 (lost-spacecraft) timer; on expiry drop back to listening. */
	private void tickT4() {
		if (t4_timer <= 0) return;
		t4_timer++;
		if (t4_timer > TIMER_T4) {
			stopT4();
			DEBUG("Downlink T4 expired - back to listening");
//			nextState(new PacSatEvent(PacSatEvent.UL_TIMER_T3_EXPIRY));
			state = DL_LISTEN;
			retries = 0;
			lastCommand = null;
		}
	}

	/** We sent a command and are waiting for a response.  Retry, or give up. */
	private void serviceWaitState() {
		waitTimer++;
		if (waitTimer * LOOP_TIME < WAIT_TIME) return;

		waitTimer = 0;
		retries++;
		if (retries > MAX_RETRIES) {
			// end the wait state.  Assume we lost the spacecraft.  Listen again. Wait to see PB Status.
			state = DL_LISTEN;
			if (lastCommand instanceof RequestDirFrame)
				lastChecked = null; // we failed with our DIR request, so try this again next time spacecraft avail
			retries = 0;
		} else {
			// retry the command
			state = DL_PB_OPEN;
			processEvent(lastCommand);
		}
	}

	/**
	 * The PB is open.  Decide whether to request the directory, a file, or fill
	 * directory holes, according to the state of the Directory.
	 */
	private void servicePbOpen() {
		// Only command stations may proceed if the PB is command-only, we need a
		// secret key, and none is loaded.
		if (openForCommandStationsOnly
				&& spacecraft.getBoolean(SpacecraftSettings.HAS_SECRET_KEY)
				&& spacecraft.key == null) {
			return;
		}
		if (Config.getBoolean(Config.TX_INHIBIT)) return;

		if (spacecraft.getBoolean(SpacecraftSettings.REQ_DIRECTORY) && needDir()) {
			requestDirectory();
		} else if (spacecraft.getBoolean(SpacecraftSettings.REQ_FILES) && spacecraft.directory.needFile()) {
			requestFile();
		} else if (spacecraft.getBoolean(SpacecraftSettings.FILL_DIRECTORY_HOLES) && spacecraft.directory.hasHoles()) {
			DEBUG("We have dir holes. Requesting dir ..");
			requestDirectory();
		}
	}

	/** Build and enqueue a directory request covering the current dir holes. */
	private void requestDirectory() {
		SortedArrayList<DirHole> holes = spacecraft.directory.getHolesList();
		if (holes == null) {
			Log.errorDialog("ERROR", "Something has gone wrong and the directory holes file is missing or corrupt\nCan't request the directory\n");
			return;
		}
		DEBUG("Requesting " + holes.size() + " holes for directory");
		processEvent(buildDirRequest(holes));
	}

	/** Build and enqueue a request for the most urgent file, covering its holes. */
	private void requestFile() {
		long fileId = spacecraft.directory.getMostUrgentFile();
		if (fileId == 0) return;
		PacSatFile pf = new PacSatFile(spacecraft, spacecraft.directory.dirFolder, fileId);
		SortedArrayList<FileHole> holes = pf.getHolesList();
		PRINT("Requesting file " + Long.toHexString(fileId));
		processEvent(buildFileRequest(fileId, holes));
	}

	/**
	 * Construct a RequestDirFrame, signed with the secret key when the PB is open
	 * for command stations only.  Returns null (a no-op for processEvent) if the
	 * key is bad; the error is reported to the user.
	 */
	private RequestDirFrame buildDirRequest(SortedArrayList<DirHole> holes) {
		try {
			if (openForCommandStationsOnly)
				return new RequestDirFrame(Config.get(Config.CALLSIGN),
						spacecraft.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, holes, spacecraft.key);
			return new RequestDirFrame(Config.get(Config.CALLSIGN),
					spacecraft.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, holes);
		} catch (InvalidKeyException e) {
			Log.errorDialog("ERROR", "Invalid secret command key\n");
		} catch (NoSuchAlgorithmException e) {
			Log.errorDialog("ERROR", "No such algorithm for secret command key\n");
		}
		return null;
	}

	/**
	 * Construct a RequestFileFrame, signed with the secret key when the PB is open
	 * for command stations only.  Returns null (a no-op for processEvent) if the
	 * key is bad; the error is reported to the user.
	 */
	private RequestFileFrame buildFileRequest(long fileId, SortedArrayList<FileHole> holes) {
		try {
			if (openForCommandStationsOnly)
				return new RequestFileFrame(Config.get(Config.CALLSIGN),
						spacecraft.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, fileId, holes, spacecraft.key);
			return new RequestFileFrame(Config.get(Config.CALLSIGN),
					spacecraft.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, fileId, holes);
		} catch (InvalidKeyException e) {
			Log.errorDialog("ERROR", "Invalid secret command key\n");
		} catch (NoSuchAlgorithmException e) {
			Log.errorDialog("ERROR", "No such algorithm for secret command key\n");
		}
		return null;
	}
	
}