package passControl;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import jssc.SerialPortException;
import pacSat.TncDecoder;
import pacSat.frames.KissFrame;
import pacSat.frames.PacSatFrame;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.RequestFileFrame;
import pacSat.frames.StatusFrame;
import pacSat.frames.UiFrame;

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
public class DownlinkStateMachine extends StateMachine implements Runnable {

	int state;
	private TncDecoder tncDecoder;
	ConcurrentLinkedQueue<PacSatFrame> frameEventQueue;
	boolean running = true;
	
	// These are the states of the State Machine
	public static final int DL_LISTEN = 0; // No heard the spacecraft yet
	public static final int DL_PB_OPEN = 1; // We heard it and the PB is Empty or has a list that does not include us
	public static final int DL_ON_PB = 2; // We are on the PB
	public static final int DL_WAIT = 3; // We are waiting for the result of a command we sent
	public static final int DL_PB_FULL = 4; // PB is full, we need to wait
	public static final int DL_PB_SHUT = 5; // PB is shut, we need to wait
	
	public static final int LOOP_TIME = 1; // length of time in ms to process respones
	public static final int WAIT_TIME = 3000; // length of time in ms to wait for a response from spacecraft
	public static final int MAX_RETRIES = 5; // max number of times we will send command
	
	int waitTimer = 0; // time how long we are in the wait state
	int retries = 0;
	PacSatFrame lastCommand = null;
	
	Spacecraft spacecraft;
			
	public static final String[] states = {
			"Listening",
			"PB Avail",
			"ON PB",
			"Waiting",
			"PB Full",
			"PB Shut"
	};
	
	String pbList = "";
	String pgList = "";
	
	public DownlinkStateMachine(Spacecraft sat) {
		spacecraft = sat;
		state = DL_LISTEN;
		frameEventQueue = new ConcurrentLinkedQueue<PacSatFrame>();
	}
	
	public void setTncDecoder(TncDecoder tnc) {
		tncDecoder = tnc;
	}
	
	public void processEvent(PacSatFrame frame) {
		Log.println("Adding Event: " + frame.toString());
		frameEventQueue.add(frame);
	}
	
	private void nextState(PacSatFrame frame) {

		// Special cases for Frames that are state independant
		// This prevents them being repeated in every state
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_BYTE:
			Log.println("BYTES");
			break;

		case PacSatFrame.PSF_STATUS_PBFULL:
			state = DL_PB_FULL;
			pbList =  UiFrame.makeString(frame.getBytes());
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_PBSHUT:
			state = DL_PB_SHUT;
			pbList =  UiFrame.makeString(frame.getBytes());
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
			break;
			
		case PacSatFrame.PSF_STATUS_BBSTAT:
			state = DL_ON_PB;
			pgList =  UiFrame.makeString(frame.getBytes());
			if (MainWindow.frame != null)
				MainWindow.setPGStatus(pgList);
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
	 * We are not in a pass.  Waiting for the spacecraft
	 * @param event
	 */
	private void stateInit(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList = UiFrame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = UiFrame.makeString(frame.getBytes());
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
			Log.println(dirFrame.toString());
			if (tncDecoder != null) {
				try {
					tncDecoder.sendFrame(kss.getDataBytes());
					state = DL_WAIT;
					waitTimer = 0;
					lastCommand = dirFrame;
				} catch (SerialPortException e) {
					Log.errorDialog("ERROR", "Could not write Kiss Frame to Serial Port\n " + e.getMessage());
				}
			} else {
				Log.infoDialog("NO TNC", "Nothing was transmitted as no TNC is connectedt\n ");
			}
			break;

		case PacSatFrame.PSF_REQ_FILE:
			RequestFileFrame fileFrame = (RequestFileFrame)frame;
			KissFrame kssFile = new KissFrame(0, KissFrame.DATA_FRAME, fileFrame.getBytes());
			Log.println(fileFrame.toString());
			if (tncDecoder != null) {
				try {
					tncDecoder.sendFrame(kssFile.getDataBytes());
					state = DL_WAIT;
					waitTimer = 0;
					lastCommand = fileFrame;
				} catch (SerialPortException e) {
					Log.errorDialog("ERROR", "Could not write Kiss Frame to Serial Port\n " + e.getMessage());
				}
			} else {
				Log.infoDialog("NO TNC", "Nothing was transmitted as no TNC is connectedt\n ");
			}
			break;
	
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList =  UiFrame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = UiFrame.makeString(frame.getBytes());
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
				pbList =  UiFrame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = UiFrame.makeString(frame.getBytes());
			}
			if (MainWindow.frame != null)
				MainWindow.setPBStatus(pbList);
			break;
		}
	}

	private void stateWait(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we must now be on the PB
			state = DL_ON_PB;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			break;
			
		case PacSatFrame.PSF_RESPONSE_ERROR: // we have an ERR response, this is echoed to the screen, tell user.  Abandon automated action?
			state = DL_LISTEN;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			break;
			
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList =  UiFrame.makeString(frame.getBytes());
			} else {
				// we dont change state, stay in WAIT
				pbList = UiFrame.makeString(frame.getBytes());
			}
			MainWindow.setPBStatus(pbList);
			break;
			
		default:
			break;
		}
	}
	
	public void stopRunning() {
		running = false;
	}
	
	@Override
	public void run() {
		Log.println("STARTING DL Thread");
		while (running) {
			if (frameEventQueue.size() > 0)
				nextState(frameEventQueue.poll());
			else if (state == DL_WAIT) {
				waitTimer++;
				if (waitTimer * LOOP_TIME >= WAIT_TIME) {
					waitTimer = 0;
					retries++;
					if (retries > MAX_RETRIES) {
						state = DL_LISTEN; // end the wait state.  Assume we lost the spacecraft.  Listen again.  Maybe we lost it.
						retries = 0;
					} else {
						// retry the command
						state = DL_PB_OPEN;
						processEvent(lastCommand);
					}
				}
			}
			
			// Here we decide if we should request the DIR or a FILE depending on the status of the Directory
			// The PB must be open and we must need one or the other according to the Directory
			if (state == DL_PB_OPEN) {
				if (spacecraft.directory.needDir()) {
					Date fromDate = Config.spacecraft.directory.getLastHeaderDate();
					RequestDirFrame dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, fromDate);
					processEvent(dirFrame);
				}
//				} else {
//					long fileId = spacecraft.directory.needFile();
//					if (fileId != 0) {
//						RequestFileFrame dirFrame = new RequestFileFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, fileId, null);
//						Config.downlink.processEvent(dirFrame);
//					}
//				}
				
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
