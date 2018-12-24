package passControl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import ax25.Ax25Frame;
import ax25.Ax25Request;
import common.Config;
import common.Log;
import common.Spacecraft;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;
import gui.MainWindow;
import pacSat.frames.FTL0Frame;
import pacSat.frames.PacSatEvent;
import pacSat.frames.PacSatFrame;
import pacSat.frames.PacSatPrimative;
import pacSat.frames.StatusFrame;
import pacSat.frames.ULCmdFrame;

public class UplinkStateMachine extends PacsatStateMachine implements Runnable {
	public static final int UL_UNINIT = 0;
	public static final int UL_OPEN = 1;  // state not in the spec.  We have seen the spacecraft is open but not yet logged in
	public static final int UL_CMD_OK = 2;
	public static final int UL_WAIT = 3;
	public static final int UL_DATA = 4;
	public static final int UL_END = 5;
	
	public static final int LOOP_TIME = 1; // length of time in ms to process responses
	
	int state = UL_UNINIT;
	
	File fileUploading = null;  // when set, this is the current file that we are uploading
	long fileIdUploading = 0; // set to non zero once we have a file number
	long fileContinuationOffset = 0; // set to non zero if this is a continuation
	// because the 2 byte header is a small overhead, lets keep 1 FTL0 packet in 1 Iframe.  
	// So the max size of the packet is the max size of an Iframe
	public static final int PACKET_SIZE = 256-2; // max bytes to send , per UoSAT notes, but subtract header?
	
	public static final int TIMER_T3 = 3*60000; // 3 min - milli seconds for T3 - Reset Open if we have not heard the spacecraft
	Timer t3_timer; 
	
	public static final String[] states = {
			"Idle",
			"Open",
			"Cmd Ok",
			"Waiting",
			"Data",
			"Data End",
			"Full",
			"Shut"
	};
	
	String pgList = "";
	
	public UplinkStateMachine(Spacecraft sat) {
		super(sat);
		state = UL_UNINIT;
	}
	
	
	@Override
	public void processEvent(PacSatPrimative frame) {
		DEBUG("Adding UP LINK Event: " + frame.toString());
		frameEventQueue.add(frame);
	}

	@Override
	protected void nextState(PacSatPrimative pacSatPrimative) {
		if (!Config.getBoolean(Config.UPLINK_ENABLED)) {
			state = UL_UNINIT;
			return;
		}
			
		// Special cases for Frames that are state independant
		// This prevents them being repeated in every state
//		switch (pacSatEvent.frameType) {
//		
//		default:
//			break;
//		}
	
		switch (state) {
		case UL_UNINIT:
			stateInit(pacSatPrimative);
			break;
		case UL_OPEN:
			stateOpen(pacSatPrimative);
			break;
		case UL_WAIT:
			stateWait(pacSatPrimative);
			break;
		case UL_CMD_OK:
			state_UL_CMD_OK(pacSatPrimative);
			break;
		case UL_DATA:
			state_DATA(pacSatPrimative);
			break;
		case UL_END:
			state_DATA_END(pacSatPrimative);
			break;
		default:
			break;
		}
	}
	
	/**
	 * We are not in a pass or we lost the signal during a pass.  Waiting for the spacecraft
	 * We refuse requests to upload a file and ignore all other requests.
	 * If we see the spacecraft is Open for Upload we change state
	 * @param event
	 */
	private void stateInit(PacSatPrimative prim) {
		if (prim instanceof PacSatEvent) {
			PacSatEvent req = (PacSatEvent) prim;
			switch (req.type) {
			case PacSatEvent.UL_REQUEST_UPLOAD:
				// refuse
				ta.append("REFUSED: Can't upload a file until the Spacecraft is Open");
				break;
			case PacSatEvent.UL_TIMER_T3_EXPIRY:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null)
					MainWindow.setPGStatus("");
				stopT3(); // stop T3, clearly it was running
				break;
			default:
				// Ignore
				break;
			}
		} else {
			// I think this event is in the DL State machine in the documents, but we process it here
			PacSatFrame frame = (PacSatFrame) prim;
			switch (frame.frameType) {
			case PacSatFrame.PSF_LOGIN_RESP:
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
					pgList = frame.toString();
					// We are connected
//					connected = true;
					fileUploading = null;
					state = UL_CMD_OK;
					startT3(); // start T3
				} else {
					// we don't change the state, this was someone else
				}
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(pgList);
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				setPgStatus(frame);
				startT3(); // start T3
				break;
			default:
				break;
			}
		}
	}

	/**
	 * The Uplink is open for business.
	 * @param frame
	 */
	private void stateOpen(PacSatPrimative prim) {
		if (prim instanceof PacSatEvent) {
			PacSatEvent req = (PacSatEvent) prim;
			switch (req.type) {
			case PacSatEvent.UL_REQUEST_UPLOAD:
				// refuse
				ta.append("REFUSED: Can't upload a file until Logged in");
				break;
			case PacSatEvent.UL_DISCONNECTED:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null)
					MainWindow.setPGStatus("");
				stopT3(); // stop T3
				break;
			case PacSatEvent.UL_TIMER_T3_EXPIRY:
				ta.append("Nothing heard from spacecraft ... ");
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null)
					MainWindow.setPGStatus("");
				stopT3(); // stop T3
				terminateDataLink();
				break;
			}			
		} else {
			PacSatFrame frame = (PacSatFrame) prim;

			switch (frame.frameType) {

			case PacSatFrame.PSF_LOGIN_RESP:
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
					pgList = frame.toString();
					// We are connected
//					connected = true;
					state = UL_CMD_OK;
					startT3(); // start T3
				} else {
					// we don't change the state, this was someone else
				}
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(pgList);
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				setPgStatus(frame);
				startT3(); // start T3
				break;
			default:
				break;
			}
		}
	}

	// In this state we can initiate an UPLOAD
	private void state_UL_CMD_OK(PacSatPrimative prim) {
		if (prim instanceof PacSatEvent) {
			PacSatEvent req = (PacSatEvent) prim;
			switch (req.type) {
			case PacSatEvent.UL_REQUEST_UPLOAD:
				ULCmdFrame cmd = new ULCmdFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BBS_CALLSIGN), 
						req);
				DEBUG("UL_CMD: " + cmd);
				Ax25Request lay2req = new Ax25Request(cmd.iFrame);
				Config.layer2data.processEvent(lay2req);
				state = UL_WAIT;
				break;
			case PacSatEvent.UL_DISCONNECTED:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null)
					MainWindow.setPGStatus("");
				stopT3(); // stop T3
				break;
			default: // including timer expire
				// Data link terminated
				terminateDataLink();
				DEBUG("DATA LINK TERMINATED");
				state = UL_UNINIT;
				break;
			}
		} else {
			PacSatFrame frame = (PacSatFrame) prim;

			switch (frame.frameType) {
			case PacSatFrame.PSF_STATUS_BBSTAT:
				pgList =  Ax25Frame.makeString(frame.getBytes());
				// We are already open, don't need to change the status, just display
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(pgList);
				startT3(); // start T3
				break;
			default:
				// Data link terminated
				terminateDataLink();
				DEBUG("DATA LINK TERMINATED");
				
				state = UL_UNINIT;
				break;
			}
		}		
	}
	
	private void stateWait(PacSatPrimative prim) {
		if (prim instanceof PacSatEvent) {
			PacSatEvent req = (PacSatEvent) prim;
			switch (req.type) {
			case PacSatEvent.UL_DISCONNECTED:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null)
					MainWindow.setPGStatus("");
				stopT3(); // stop T3
				break;			
			default: // including timer expire
				// Data link terminated
				terminateDataLink();
				DEBUG("DATA LINK TERMINATED");
				state = UL_UNINIT;
				break;
			}
		} else {
			PacSatFrame frame = (PacSatFrame) prim;

			switch (frame.frameType) {
			case PacSatFrame.PSF_UL_GO_RESP:
				DEBUG("UL_GO_RESP: " + frame);
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
					// Here we get the file number to use and potentially a continuation
					FTL0Frame ftl = (FTL0Frame)frame;
					DEBUG("GO FILE>" + ftl);
					try {
						PacSatFile psf = new PacSatFile(fileUploading.getPath());
						psf.setFileId(ftl.getFileId());
						psf.save();
						if (Config.mainWindow != null)
							Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());
					} catch (MalformedPfhException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					fileIdUploading = ftl.getFileId();
					fileContinuationOffset = ftl.getContinuationOffset();
					state = UL_DATA;
					startT3(); // start T3
				}
				break;
			case PacSatFrame.PSF_UL_ERROR_RESP:
				DEBUG("UL_ERROR_RESP: " + frame);
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
				//TODO - is the error unrecoverable - then mark file impossible
				File newFile = new File(fileUploading.getPath()+".err");
				fileUploading.renameTo(newFile);
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());

				fileUploading=null;
				state = UL_CMD_OK;
				startT3(); // start T3
				}
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				pgList =  Ax25Frame.makeString(frame.getBytes());
				// We are already open, don't need to change the status, just display
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(pgList);
				startT3(); // start T3
				break;
			default:
				// Data link terminated
				terminateDataLink();
				DEBUG("DATA LINK TERMINATED");
				state = UL_UNINIT;
				break;
			}
		}
	}

	private void state_DATA(PacSatPrimative prim) {
		
		if (prim instanceof PacSatEvent) {
			PacSatEvent req = (PacSatEvent) prim;
			switch (req.type) {
			case PacSatEvent.UL_DISCONNECTED:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null)
					MainWindow.setPGStatus("");
				stopT3(); // stop T3
				break;
			case PacSatEvent.UL_DATA:
				DEBUG("UL_DATA: " + req);
				ULCmdFrame cmd = new ULCmdFrame(Config.get(Config.CALLSIGN), 
						Config.spacecraft.get(Spacecraft.BBS_CALLSIGN), req);
				Ax25Request lay2req = new Ax25Request(cmd.iFrame);
				Config.layer2data.processEvent(lay2req);
				state = UL_DATA;
				break;
			case PacSatEvent.UL_DATA_END:
				DEBUG("UL_DATA_END: " + req);
				cmd = new ULCmdFrame(Config.get(Config.CALLSIGN), 
						Config.spacecraft.get(Spacecraft.BBS_CALLSIGN), req);
				lay2req = new Ax25Request(cmd.iFrame);
				Config.layer2data.processEvent(lay2req);
				state = UL_END;
				break;
			default: // including timer expire
				// Data link terminated
				terminateDataLink();
				DEBUG("DATA LINK TERMINATED");
				state = UL_UNINIT;
				break;
			}
		} else {
			PacSatFrame frame = (PacSatFrame) prim;

			switch (frame.frameType) {
			case PacSatFrame.PSF_UL_NAK_RESP:
				if (Config.getBoolean(Config.DEBUG_LAYER3))
					ta.append("UL_NAK_RESP: " + frame);
				//TODO - is the error unrecoverable - then mark file impossible
				File newFile = new File(fileUploading.getPath()+".err");
				fileUploading.renameTo(newFile);
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());
				// Must send Data end if we receive a NAK
				ULCmdFrame cmd = new ULCmdFrame(Config.get(Config.CALLSIGN), 
						Config.spacecraft.get(Spacecraft.BBS_CALLSIGN), new PacSatEvent(PacSatEvent.UL_DATA_END));
				Ax25Request lay2req = new Ax25Request(cmd.iFrame);
				Config.layer2data.processEvent(lay2req);
				
				fileUploading=null;
				state = UL_CMD_OK;
				startT3(); // start T3
				break;	
			case PacSatFrame.PSF_UL_ERROR_RESP:
				DEBUG("UL_ERROR_RESP: " + frame);
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
				//TODO - is the error unrecoverable - then mark file impossible
				newFile = new File(fileUploading.getPath()+".err");
				fileUploading.renameTo(newFile);
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());

				fileUploading=null;
				state = UL_CMD_OK;
				startT3(); // start T3
				}
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				pgList =  Ax25Frame.makeString(frame.getBytes());
				// We are already open, don't need to change the status, just display
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(pgList);
				startT3(); // start T3
				break;
			default:
				// Data link terminated
				terminateDataLink();
				DEBUG("DATA LINK TERMINATED");
				state = UL_UNINIT;
				break;
			}
		}
	}

	private void state_DATA_END(PacSatPrimative prim) {
		if (prim instanceof PacSatEvent) {
			PacSatEvent req = (PacSatEvent) prim;
			switch (req.type) {
			case PacSatEvent.UL_DISCONNECTED:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null)
					MainWindow.setPGStatus("");
				stopT3(); // stop T3
				break;

			default:
				// Data link terminated
				terminateDataLink();
				DEBUG("DATA LINK TERMINATED");
				state = UL_UNINIT;
				break;
			}
		} else {
			PacSatFrame frame = (PacSatFrame) prim;

			switch (frame.frameType) {
			case PacSatFrame.PSF_UL_NAK_RESP:
				DEBUG("UL_NAK_RESP: " + frame);
				//TODO - is the error unrecoverable - then mark file impossible
				File newFile = new File(fileUploading.getPath()+".err");
				fileUploading.renameTo(newFile);
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());

				fileUploading=null;
				state = UL_CMD_OK;
				startT3(); // start T3
				break;
			case PacSatFrame.PSF_UL_ACK_RESP:
				DEBUG("UL_ACK_RESP: " + frame);
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
					DEBUG("UPLOADED!!!>");
					newFile = new File(fileUploading.getPath()+".ul");
					fileUploading.renameTo(newFile);
					if (Config.mainWindow != null)
						Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());

					fileUploading=null;
					state = UL_CMD_OK;
					startT3(); // start T3
				}
				break;
			case PacSatFrame.PSF_UL_ERROR_RESP:
				DEBUG("UL_ERROR_RESP: " + frame);
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
					//TODO - is the error unrecoverable - then mark file impossible
					newFile = new File(fileUploading.getPath()+".err");
					fileUploading.renameTo(newFile);
					if (Config.mainWindow != null)
						Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());

					fileUploading=null;
					state = UL_CMD_OK;
					startT3(); // start T3
				}
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				pgList =  Ax25Frame.makeString(frame.getBytes());
				// We are already open, don't need to change the status, just display
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(pgList);
				startT3(); // start T3
				break;
			default:
				break;
			}
		}
	}
	
	private void setPgStatus(PacSatFrame frame) {
		pgList =  Ax25Frame.makeString(frame.getBytes());
		String call = ((StatusFrame)frame).getCall();
		if (call == null) {
			state = UL_OPEN;
		} else if (call.equalsIgnoreCase(Config.get(Config.CALLSIGN))) {
			// This is a note that we are logged into the BB already, so we are in the wrong state
			Log.println("PG has US: " + call);
			state = UL_OPEN;
		} else {
			// someone is on the PG, so not open
			Log.println("PG FULL: " + call);
			state = UL_UNINIT;
		}
		if (MainWindow.frame != null)
			MainWindow.setPGStatus(pgList);
	}
	
	public void attemptLogin() {
		if (state == UL_UNINIT)
			state = UL_OPEN;
	}

	private void terminateDataLink() {
		// close the connection
		Ax25Request req = new Ax25Request(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BBS_CALLSIGN), Ax25Request.DL_DISCONNECT);
		Config.layer2data.processEvent(req);
		state = UL_UNINIT; 
		fileUploading = null;
		if (MainWindow.frame != null)
			MainWindow.setPGStatus("");
		stopT3(); // stop T3
	}
	
	private void DEBUG(String s) {
		s = "DEBUG 3: " + s;
		if (Config.getBoolean(Config.DEBUG_LAYER3)) {
			if (ta != null)
				ta.append(s + "\n");
			Log.println(s);
		}
	}
	
	private void PRINT(String s) {
		if (ta != null)
			ta.append(s + "\n");
		else
			Log.println(s);
	}
	
	public void stopRunning() {
		running = false;
	}
	
	private void loginIfFile() {
		// Do we have any files that need to be uploaded
		// They are in the sat directory and end with .OUT
		File folder = new File(Config.spacecraft.directory.dirFolder);
		File[] targetFiles = folder.listFiles();
		Arrays.sort(targetFiles); // this makes it alphabetical, but not by numeric order of the last 2 digits
		boolean found = false;
		if (targetFiles != null && fileUploading == null) { // we have a file and we are not already attempting to upload
			for (int i = 0; i < targetFiles.length; i++) {
				if (targetFiles[i].isFile() && targetFiles[i].getName().endsWith(".out")) {
					// We issue LOGIN REQ event
					PRINT("Ready to upload file: "+ targetFiles[i].getName());
					fileUploading = targetFiles[i];
					// Create a connection request.  
					//Ax25Request.DL_CONNECT
					Ax25Request req = new Ax25Request(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BBS_CALLSIGN));
					Config.layer2data.processEvent(req);
					state = UL_OPEN; // we stay in open until actually logged in, then we are in CMD_OK
					found = true;
				}
				if (found)
					break;
			}
		}
	}
	
	private void requestIfFile() {
		if (fileUploading.exists()) {
			PacSatFile psf;
			try {
				psf = new PacSatFile(fileUploading.getPath());
				processEvent(new PacSatEvent(psf));
			} catch (MalformedPfhException e) {
				Log.errorDialog("ERROR", "Can't open file in OUTBOX: " + fileUploading.getPath() + "\n" + e.getMessage());
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Can't open file in OUTBOX: " + fileUploading.getPath() + "\n" + e.getMessage());
				e.printStackTrace(Log.getWriter());
			}
		}

	}
	
	private void startT3() {
		if (t3_timer != null)
			t3_timer.cancel();
		t3_timer = new Timer();
		TimerTask t3expire = new Timer3Task();
		t3_timer.schedule(t3expire, TIMER_T3); // start T3
	}
	
	private void stopT3() {
		if (t3_timer != null)
			t3_timer.cancel();
	}

	@Override
	public void run() {
		Log.println("STARTING UPLINK Thread");
		while (running) {
//			if (t3_timer > 0) {
//				// we are timing something
//				t3_timer++;
//				if (t3_timer > TIMER_T3) {
//					t3_timer = 0;
//					DEBUG("T3 expired");
//					nextState(new PacSatEvent(PacSatEvent.UL_TIMER_T3_EXPIRY));
//				}				
//			}
			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
			} else if (state == UL_OPEN) {
				loginIfFile(); // TODO - this should happen in the state process, not here
				
			} else if (state == UL_CMD_OK) {
				if (fileUploading != null) {
					// We Request File Upload
					// TODO - this is valid as soon as UL_CMD_OK, so should be in the state
					requestIfFile();
				} else {
					// nothing more to do we should log out
					terminateDataLink();
				}
			} else if (state == UL_DATA && fileUploading != null) {
				// IF there is data to send. send the data...
				// else transmit data end
				RandomAccessFile fileOnDisk = null;
				try {
					fileOnDisk = new RandomAccessFile(fileUploading.getPath(), "r"); // opens file
					if (fileContinuationOffset < fileOnDisk.length()) {
						long length = fileOnDisk.length() - fileContinuationOffset;
						if (length > PACKET_SIZE) 
							length = PACKET_SIZE;
						// more to upload
						fileOnDisk.seek(fileContinuationOffset);
						int[] bytes = new int[(int) length];
						int i = 0;
						while (i < length)
							bytes[i++] = fileOnDisk.readUnsignedByte();
						processEvent(new PacSatEvent(bytes));						
						fileContinuationOffset = fileContinuationOffset + PACKET_SIZE; // rather than add length we add the packet size, so it overflows for DATA_END
					} else {
						processEvent(new PacSatEvent(PacSatEvent.UL_DATA_END));
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try { fileOnDisk.close(); } catch (Exception e) { }
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
	
	class Timer3Task extends TimerTask {
		public void run() {
			DEBUG("UL_T3 expired");
			nextState(new PacSatEvent(PacSatEvent.UL_TIMER_T3_EXPIRY));
		}
	}

}
