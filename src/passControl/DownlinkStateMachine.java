package passControl;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import ax25.Ax25Frame;
import common.Config;
import common.Log;
import common.Spacecraft;
import fileStore.DirHole;
import fileStore.FileHole;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;
import fileStore.SortedArrayList;
import gui.MainWindow;
import jssc.SerialPortException;
import pacSat.TncDecoder;
import pacSat.frames.KissFrame;
import pacSat.frames.PacSatFrame;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.RequestFileFrame;
import pacSat.frames.ResponseFrame;
import pacSat.frames.StatusFrame;

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
	public static final int DL_LISTEN = 0; // No heard the spacecraft yet
	public static final int DL_PB_OPEN = 1; // We heard it and the PB is Empty or has a list that does not include us
	public static final int DL_ON_PB = 2; // We are on the PB
	public static final int DL_WAIT = 3; // We are waiting for the result of a command we sent
	public static final int DL_PB_FULL = 4; // PB is full, we need to wait
	public static final int DL_PB_SHUT = 5; // PB is shut, we need to wait
	
	public static final int LOOP_TIME = 1; // length of time in ms to process respones
		
	boolean needDir = true;
	Date lastChecked = null;
	public static final int DIR_CHECK_INTERVAL = 60; // mins between directory checks;

			
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
	public DownlinkStateMachine(Spacecraft sat) {
		super(sat);
		state = DL_LISTEN;
	}
		
	/**
	 * Add a new frame of data from the spacecraft to the event queue
	 */
	public void processEvent(PacSatFrame frame) {
		Log.println("Adding DOWN LINK Event: " + frame.toString());
		frameEventQueue.add(frame);
	}
	
	protected void nextState(PacSatFrame frame) {

		// Special cases for Frames that are state independant
		// This prevents them being repeated in every state
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_BYTE:
			Log.println("BYTES");
			break;

		case PacSatFrame.PSF_STATUS_PBFULL:
			state = DL_PB_FULL;
			pbList =  Ax25Frame.makeString(frame.getBytes());
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_PBSHUT:
			state = DL_PB_SHUT;
			pbList =  Ax25Frame.makeString(frame.getBytes());
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
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
	
	/**
	 * We are not in a pass or we lost the signal during a pass.  Waiting for the spacecraft
	 * @param event
	 */
	private void stateInit(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList = Ax25Frame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
			break;
			
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response when we don't think we are in a pass, we ignore this
			waitTimer = 0;
			retries = 0;
			lastCommand = null;
			break;
		case PacSatFrame.PSF_RESPONSE_ERROR: // we have an ERR response when we don't think we are in a pass, we ignore this
			waitTimer = 0;
			retries = 0;
			lastCommand = null;
			break;
			
		case PacSatFrame.PSF_REQ_DIR:
			Log.infoDialog("Ignored", "Wait until the Spacecraft PB status has been heard before requesting a directory");
			break;
		case PacSatFrame.PSF_REQ_FILE:
			Log.infoDialog("Ignored", "Wait until the Spacecraft PB status has been heard before requesting a file");
			break;
					
		}
		
	}
	
	private void statePbOpen(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_REQ_DIR:
			RequestDirFrame dirFrame = (RequestDirFrame)frame;
			KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, dirFrame.getBytes());
			//ta.append("TX: " + dirFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = dirFrame;
				tncDecoder.sendFrame(kss.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				Log.infoDialog("NO TNC", "Nothing was transmitted as no TNC is connected\n ");
			}
			break;

		case PacSatFrame.PSF_REQ_FILE:
			RequestFileFrame fileFrame = (RequestFileFrame)frame;
			KissFrame kssFile = new KissFrame(0, KissFrame.DATA_FRAME, fileFrame.getBytes());
			ta.append("TX: " + fileFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = DL_WAIT;
				waitTimer = 0;
				lastCommand = fileFrame;
				tncDecoder.sendFrame(kssFile.getDataBytes(), TncDecoder.NOT_EXPEDITED);
			} else {
				Log.infoDialog("NO TNC", "Nothing was transmitted as no TNC is connectedt\n ");
			}
			break;
	
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList =  Ax25Frame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
			break;
			
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we must now be on the PB
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
			Log.infoDialog("Ignored", "Wait until your current PB ssession has completed before requesting another directory");
			break;
		case PacSatFrame.PSF_REQ_FILE: // Requested a FILE but we are already on the PB, so ignore
			Log.infoDialog("Ignored", "Wait until your current PB ssession has completed before requesting a file");
			break;
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList =  Ax25Frame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
			break;
		}
	}

	private void stateWait(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we stop sending command
			state = DL_ON_PB;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			break;
			
		case PacSatFrame.PSF_RESPONSE_ERROR: // we have an ERR response, this is echoed to the screen, tell user.  Abandon automated action!
			ResponseFrame sf = (ResponseFrame)frame;
			if (sf.getErrorCode() == ResponseFrame.FILE_MISSING) {
				if (lastCommand.frameType == PacSatFrame.PSF_REQ_FILE) {
					RequestFileFrame rf = (RequestFileFrame)lastCommand;
					// we are requesting a file that does not exist on the server
					// Mark it to no longer be downloaded
					Config.mainWindow.setPriority(rf.fileId, -2);
				}
			}
			state = DL_LISTEN;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			
			break;
			
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) { // looks like we missed the OK response, stop sending
				state = DL_ON_PB;
				pbList =  Ax25Frame.makeString(frame.getBytes());
			} else {
				// we dont change state, stay in WAIT
				pbList = Ax25Frame.makeString(frame.getBytes());
			}
			MainWindow.setPBStatus(pbList);
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
			Log.println("First pass since starting. Requesting dir ..");
			return true;
		}
		
		// Otherwise we get the timestamp of the last time we checked
		// If it is some time ago then we ask for another directory
		Date timeNow = new Date();
		long minsNow = timeNow.getTime() / 60000;
		long minsLatest = lastChecked.getTime() / 60000;
		long diff = minsNow - minsLatest;
		if (diff > DIR_CHECK_INTERVAL) {
			Log.println("Have not checked for an hour. Requesting dir ..");
			lastChecked = new Date();
			return true;
		}
		return false;
	}

	public void stopRunning() {
		running = false;
	}

	@Override
	public void run() {
		Log.println("STARTING DL Thread");
		while (running) {
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

				if (needDir()) {
					SortedArrayList<DirHole> holes = spacecraft.directory.getHolesList();
					if (holes != null) {
						Log.println("Requesting "+ holes.size() +" holes for directory");
						RequestDirFrame dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, holes);
						processEvent(dirFrame);
					} else {
						Log.errorDialog("ERROR", "Something has gone wrong and the directory holes file is missing or corrupt\nCan't request the directory\n");
					}
				} else if (spacecraft.directory.needFile()) {
					long fileId = spacecraft.directory.getMostUrgentFile();
					if (fileId != 0) {
						PacSatFile pf = new PacSatFile(Config.spacecraft.directory.dirFolder, fileId);
						SortedArrayList<FileHole> holes = pf.getHolesList();
						Log.println("Requesting file " + Long.toHexString(fileId));
						RequestFileFrame fileFrame = new RequestFileFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, fileId, holes);
						Config.downlink.processEvent(fileFrame);
					}	
				} else if (spacecraft.directory.hasHoles()) {
					SortedArrayList<DirHole> holes = spacecraft.directory.getHolesList();
					Log.println("We have dir holes. Requesting dir ..");
					Log.println("Requesting "+ holes.size() +" holes for directory");
					RequestDirFrame dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, holes);
					processEvent(dirFrame);
				}
			}

			try {
				Thread.sleep(LOOP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Config.mainWindow != null)
				Config.mainWindow.setDownlinkStatus(states[state]);
		}
		Log.println("EXIT DL Thread");
	}
	
}
