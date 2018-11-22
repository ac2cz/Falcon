package passControl;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

import ax25.Ax25Frame;
import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import jssc.SerialPortException;
import pacSat.TncDecoder;
import pacSat.frames.FTL0Frame;
import pacSat.frames.KissFrame;
import pacSat.frames.PacSatFrame;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.StatusFrame;
import pacSat.frames.ULCmdFrame;

public class UplinkStateMachine extends PacsatStateMachine implements Runnable {
	public static final int UL_UNINIT = 0;
	public static final int UL_OPEN = 1;
	public static final int UL_CMD_OK = 2;
	public static final int UL_WAIT = 3;
	public static final int UL_DATA = 4;
	public static final int UL_END = 5;
	
	public static final int LOOP_TIME = 1; // length of time in ms to process responses
	
	int state = UL_UNINIT;
	int sendStateVariable = 0; // V(S) - the next nuber to be assigned to the next transmitted i frame
	int receiveStateVariable = 0; //V(R) - the next expected if frame to be received
	int ackStateVariable = 0; // V(A) - sequence number of last frame ack-ed by other end  V(A)-1 = N(S) of last ACK-ed frame
	
	boolean connected = false;
	
	public static final String[] states = {
			"Idle",
			"Open",
			"Logged in",
			"Waiting",
			"Uploading",
			"Full",
			"Shut"
	};
	
	String pgList = "";
	
	public UplinkStateMachine(Spacecraft sat) {
		super(sat);
		state = UL_UNINIT;
	}
	
	
	@Override
	public void processEvent(PacSatFrame frame) {
		Log.println("Adding UP LINK Event: " + frame.toString());
		frameEventQueue.add(frame);
	}

	@Override
	protected void nextState(PacSatFrame frame) {
		// Special cases for Frames that are state independant
		// This prevents them being repeated in every state
		switch (frame.frameType) {
		case PacSatFrame.PSF_STATUS_BBSTAT:
			pgList =  Ax25Frame.makeString(frame.getBytes());
			if (((StatusFrame)frame).containsCall()) {
				// This is a note that we are logged into the BB, but it is just for info.  Don't change the state
				//state = UL_DATA;
			} else {
				state = UL_OPEN;
			}
			if (MainWindow.frame != null)
				MainWindow.setPGStatus(pgList);
			break;
		default:
			break;
		}
	
		switch (state) {
		case UL_UNINIT:
			stateInit(frame);
			break;
		case UL_OPEN:
			stateOpen(frame);
			break;
		case UL_WAIT:
			stateWait(frame);
			break;
		case UL_CMD_OK:
			state_UL_OK(frame);
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
		default:
			break;
		}
	}
	
	private void stateOpen(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.TYPE_U_SABM:
			ULCmdFrame cmdFrame = (ULCmdFrame)frame;
			KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, cmdFrame.getBytes());
			ta.append("TX: " + cmdFrame.toString() + " ... ");
			if (tncDecoder != null) {
				state = UL_WAIT;
				waitTimer = 0;
				lastCommand = cmdFrame;
				tncDecoder.sendFrame(kss.getDataBytes());
			} else {
				Log.infoDialog("NO TNC", "Nothing was transmitted as no TNC is connected\n ");
			}
			break;
		
		default:
			break;
		}
	}

	private void stateWait(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.TYPE_UA:
			// We got an ACK for the last command, likely the login request
			if (lastCommand instanceof ULCmdFrame && ((ULCmdFrame)lastCommand).frameType == PacSatFrame.TYPE_U_SABM) {
				// We are connected
				connected = true;
				retries = MAX_RETRIES +1;  // expire the current command?? Or put in another state???
			}
			break;
		case PacSatFrame.PSF_LOGIN_RESP:
			if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
				pgList = frame.toString();
				state = UL_CMD_OK;
			} else {
				// we don't change the state, this was someone else
			}
			if (MainWindow.frame != null)
				MainWindow.setPGStatus(pgList);
			break;
		default:
			break;
		}
	}

	
	// In this state we can initiate an UPLOAD
	private void state_UL_OK(PacSatFrame frame) {
		switch (frame.frameType) {
		default:
			break;
		}
		
	}
		
	public void stopRunning() {
		running = false;
	}

	@Override
	public void run() {
		Log.println("STARTING UPLINK Thread");
		while (running) {
			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
			} else if (state == UL_WAIT) {
				waitTimer++;
				if (waitTimer * LOOP_TIME >= WAIT_TIME) {
					waitTimer = 0;
					retries++;
					if (retries > MAX_RETRIES) {
						state = UL_UNINIT; // end the wait state.  Assume we lost the spacecraft.  Listen again. Wait to see PB Status.
	//					if (lastCommand instanceof RequestDirFrame)
	//						lastChecked = null; // we failed with our DIR request, so try this again next time spaceacraft avail
						retries = 0;
					} else {
						// retry the command
						state = UL_OPEN;    /////// This depends on the last command.  Need to store the state to fall back to???
						processEvent(lastCommand);
					}
				}
			} else if (state == UL_OPEN && !connected) {
				// Do we have any files that need to be uploaded
				// They are in the sat directory and end with .OUT
				File folder = new File(Config.spacecraft.directory.dirFolder);
				File[] targetFiles = folder.listFiles();
				boolean found = false;
				if (targetFiles != null) {
					for (int i = 0; i < targetFiles.length; i++) {
						if (targetFiles[i].isFile() && targetFiles[i].getName().endsWith(".out")) {
							// We issue LOGIN REQ event
							Log.println("Ready to upload file: "+ targetFiles[i].getName());
							// Create a connection request.  This is an U frame of type SABM.  We then wait for a UA response frame
							ULCmdFrame frame = new ULCmdFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BBS_CALLSIGN),Ax25Frame.TYPE_U_SABM);
							processEvent(frame);
							found = true;
						}
						if (found)
							break;
					}
				}
			}
			try {
				Thread.sleep(LOOP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Config.mainWindow != null)
				Config.mainWindow.setUplinkStatus(states[state]);
		}
		Log.println("EXIT UPLINK Thread");

	}

}
