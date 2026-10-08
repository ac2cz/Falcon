package passControl;

import ax25.KissFrame;
import common.Config;
import common.Log;
import common.SpacecraftSettings;
import pacSat.TncDecoder;
import pacSat.frames.PacSatFrame;
import pacSat.frames.PacSatPrimative;
import pacSat.frames.ResponseFrame;

/**
 * THIS IS UNIMPLEMNTED and will probablly remain so.  The commands are handed in PB - the downlink state
 * machine.  The idea was to seperate out command processing so that it could be automated in a pass, but
 * that adds complexity and may not be needed.  This remains in case.
 * 
 * @author Chris Thompson VE2TCP
 *
 */
public class CommandStateMachine extends PacsatStateMachine implements Runnable {

	public static final int CMD_LISTEN = 0; // Not heard the spacecraft yet.  Triggered by STATUS frames
	public static final int CMD_IDLE = 1; // We heard it and the spacecraft.  No commands in flight.
	public static final int CMD_WAIT = 2; // We are waiting for the result of a command we sent
	public static final int CMD_SAFE = 3; // Spacecraft heard but it is in safe mode.  Restrict commanding
	public static final int CMD_BUSY = 4; // Spacecraft heard but it is busy sending SSTV or similar.  Restrict commanding
	public static final long TIMER_T4_LIMIT = 60_000L * 1_000_000L;; // 60 seconds
	public static final long TIMER_T1_LIMIT = 10_000L  * 1_000_000L;; // 10 seconds
					
	long retry_timer; // retry timer
	long heard_timer; // the last heard timer
	
	
	public static final String[] states = {
			"Listening",
			"CMD Idle",
			"CMD Wait",
			"CMD Safe",
			"CMD Busy"
	};

	public CommandStateMachine(SpacecraftSettings sat) {
		super(sat);
		state = CMD_LISTEN;
	}

	@Override
	public void processEvent(PacSatPrimative frame) {
		if (frame == null) return;
		DEBUG("Adding COMMAND Event: " + frame.toString());
		frameEventQueue.add(frame);
	}
	
	@Override
	protected void nextState(PacSatPrimative prim) {
		if (!(prim instanceof PacSatFrame)) return;
		PacSatFrame frame = (PacSatFrame) prim;
		
		switch (frame.frameType) {
		case PacSatFrame.PSF_COMMAND:
			if (state == CMD_WAIT) return; // command already in flight - drop
			sendCommand(frame);
			return;
		
		default:
			break;
		}
		
		
		switch (state) {
		case CMD_LISTEN:
			//stateInit(frame);
			break;
		case CMD_WAIT:
			stateWait(frame);
			break;
		default:
			break;
		}
	}
	
	private void stateWait(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_COMMAND_STOP:
			state = CMD_LISTEN;
			lastCommand = null;
			retries = 0;
			break;
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we stop sending command
			//startT4();
			state = CMD_LISTEN;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			break;
			
		case PacSatFrame.PSF_RESPONSE_ERROR: // we have an ERR response, this is echoed to the screen, tell user.  Abandon automated action!
			//startT4();
			ResponseFrame sf = (ResponseFrame)frame;
			if (sf.getErrorCode() == ResponseFrame.FILE_MISSING ||
					sf.getErrorCode() == ResponseFrame.FILE_MARKED_NOT_TO_DOWNLOAD) {
				if (lastCommand.frameType == PacSatFrame.PSF_REQ_FILE) {
//					RequestFileFrame rf = (RequestFileFrame)lastCommand;
//					// we are requesting a file that does not exist on the server
//					// Mark it to no longer be downloaded
//					// This should not call the GUI directly!!  Update the directory.
//					//Config.mainWindow.dirPanel.setPriority(rf.fileId, -2);
//					spacecraft.directory.setPriority(rf.fileId, sf.getErrorCode());
//					String[][] data = spacecraft.directory.getTableData();
//					if (data.length > 0)
//						if (Config.mainWindow != null)
//							MainWindow.setDirectoryData(spacecraft.name, data);
				}
			} else if (	sf.getErrorCode() == ResponseFrame.TEMPORARY_ERROR) {
				// requesting a file that is temporarily not available
				// We will abandon the action but we do not mark the file as unavailable
			}
			state = CMD_LISTEN;
			waitTimer = 0;
			lastCommand = null;
			retries = 0;
			
			break;
			
		default:
			break;
		}
	}
	
	private void PRINT(String s) {
		if (ta != null)
			ta.append(s + "\n");
		Log.println(s);
	}
	
	/**
	 * Transmit a command to the spacecraft and enter DL_WAIT so we pick up the
	 * OK/ERR response.  If no TNC is connected nothing is transmitted and we do not
	 * change state.
	 */
	private void sendCommand(PacSatFrame frame) {
		//startT4();
		KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, frame.getBytes());
		PRINT(frame.toString() + " ... ");
		if (tncDecoder != null) {
			state = CMD_WAIT;
			waitTimer = 0;
			lastCommand = frame;
			tncDecoder.sendFrame(kss.getDataBytes(), TncDecoder.NOT_EXPEDITED);
		} else {
			PRINT("Nothing was transmitted as no TNC is connected\n ");
		}
	}
	
	private void DEBUG(String s) {
		s = "DEBUG CMD: " + states[state] + ": " + s;

		if (Config.getBoolean(Config.DEBUG_COMMANDING)) {
			if (ta != null)
				ta.append(s + "\n");
			Log.println(s);
		}
	}


	private void start_heard_timer() {
		heard_timer = System.nanoTime() + TIMER_T4_LIMIT;
	}
	
	private void stop_heard_timer() {
		heard_timer = 0;
	}
		
	private boolean heard_timer_expired() {
		long now = System.nanoTime();
		if (now - heard_timer >= 0) return true;
		return false;
	}

	private void start_rety_timer() {
		retry_timer = System.nanoTime();
	}
	
	private void stop_retry_timer() {
		retry_timer = 0;
	}

	
	@Override
	public void run() {
		DEBUG("STARTING Commanding Thread");
		Thread.currentThread().setName("CommandStateMachine: " + spacecraft.name);
		
		while (running) {
			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
			} else if (state == CMD_WAIT) {
//				serviceWaitState();
			} 
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
