package passControl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import ax25.Ax25Frame;
import ax25.Ax25Request;
import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;
import gui.EditorFrame;
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

	public static final int LOOP_TIME = 10; // length of time in ms to process responses

	int state = UL_UNINIT;

	public File fileUploading = null;  // when set, this is the current file that we are uploading
	public long fileIdUploading = 0; // set to non zero once we have a file number
	public long fileContinuationOffset = 0; // set to non zero if this is a continuation
	public long fileUploadingLength = 0; 
	// because the 2 byte header is a small overhead, lets keep 1 FTL0 packet in 1 Iframe.  
	// So the max size of the packet is the max size of an Iframe
	//public static final int PACKET_SIZE = 256-2; // max bytes to send , per UoSAT notes, but subtract header?
	public static final int PACKET_SIZE = 190; // max bytes to send , to fit	 in FX25 frame?

	public static final int TIMER_T3 = 60*100; // 60 secs for T3 - Reset Open if we have not heard "Open" from the spacecraft
	//Timer t3_timer; 
	int t3_timer;
	public static final DateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd.HHmm");
	public static final String ERR = "err";
	public static final String UL = "ul";

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

	public UplinkStateMachine(SpacecraftSettings sat) {
		super(sat);
		state = UL_UNINIT;
	}

	public void setSpacecraft(SpacecraftSettings spacecraftSettings) {
		// TODO - drain and process the event list before switching
		spacecraft = spacecraftSettings;
	}
	
	@Override
	public void processEvent(PacSatPrimative frame) {
		if (Config.getBoolean(Config.DEBUG_EVENTS))
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
				PRINT("REFUSED: Can't upload a file until the Spacecraft is Open");
				break;
			case PacSatEvent.UL_TIMER_T3_EXPIRY:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
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
					// but we won't get this event here as the DL state machine only forwards frames to us
				}
				if (MainWindow.frame != null)
					MainWindow.setLoggedin(spacecraft.name, pgList);
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
				PRINT("REFUSED: Can't upload a file until Logged in");
				break;
			case PacSatEvent.UL_DISCONNECTED:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
				stopT3(); // stop T3
				break;
			case PacSatEvent.UL_TIMER_T3_EXPIRY:
				PRINT("Nothing heard from spacecraft ... ");
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
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
					MainWindow.setLoggedin(spacecraft.name, pgList);
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
				ULCmdFrame cmd = new ULCmdFrame(spacecraft, Config.get(Config.CALLSIGN), spacecraft.get(SpacecraftSettings.BBS_CALLSIGN), 
						req);
				DEBUG("UL_CMD: " + cmd);
				Ax25Request lay2req = new Ax25Request(cmd.iFrame);
				spacecraft.layer2data.processEvent(lay2req);
				state = UL_WAIT;
				break;
			case PacSatEvent.UL_DISCONNECTED:
				state = UL_UNINIT;
				fileUploading = null;
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
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
					MainWindow.setPGStatus(spacecraft.name, pgList);
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
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
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
				// Here we get the file number to use and potentially a continuation
				FTL0Frame ftl = (FTL0Frame)frame;
				DEBUG("GO FILE>" + ftl);
				try {
					PacSatFile psf = new PacSatFile(spacecraft, fileUploading.getPath());
					psf.setFileId(ftl.getFileId());
					psf.save();
					if (Config.mainWindow != null)
						Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());
				} catch (MalformedPfhException e) {
					PRINT("ERROR: The Pacsat File Header is corrupt for Upload file"+fileUploading.getPath()+"\n"+e.getMessage());
					terminateDataLink();
					renameExtension(fileUploading, ERR);
					//File newFile = new File(fileUploading.getPath()+".err");
					//fileUploading.renameTo(newFile);
					e.printStackTrace(Log.getWriter());
				} catch (IOException e) {
					// File was renamed under us or the OS disk is full?
					PRINT("ERROR: Could not write the new file Id to the Upload file\n"+e.getMessage());
					terminateDataLink();
					//File newFile = new File(fileUploading.getPath()+".err");
					renameExtension(fileUploading, ERR);
					//fileUploading.renameTo(newFile);
					e.printStackTrace(Log.getWriter());
				}
				fileIdUploading = ftl.getFileId();
				fileContinuationOffset = ftl.getContinuationOffset();
				state = UL_DATA;
				startT3(); // start T3
				break;
			case PacSatFrame.PSF_UL_ERROR_RESP:
				DEBUG("UL_ERROR_RESP: " + frame);
				//TODO - is the error unrecoverable - then mark file impossible
				//Here we have sent the upload request and are waiting for the GO.  Instead we got an error
				//Possible values
				FTL0Frame err = (FTL0Frame) frame;
				if (err.getErrorCode() == FTL0Frame.ER_NO_SUCH_FILE_NUMBER || err.getErrorCode() == FTL0Frame.ER_BAD_CONTINUE) {
					// we passed a continue file number and it is not valid.  Or it is valid but file length now different
					// Need to set fileId to zero and start upload again.
					PacSatFile psf;
					try {
						PRINT("Bad File Number, will ask Pacsat for a new number for file: " +fileUploading.getPath());
						psf = new PacSatFile(spacecraft, fileUploading.getPath());
						psf.setFileId(0);
						psf.save();
						state = UL_CMD_OK;
						fileContinuationOffset = 0;
						processEvent(new PacSatEvent(psf));
					} catch (MalformedPfhException e) {
						PRINT("ERROR: The Pacsat File Header is corrupt for Upload file"+fileUploading.getPath()+"\n"+e.getMessage());
						terminateDataLink();
						renameExtension(fileUploading, ERR);
						fileUploading = null;
						fileContinuationOffset = 0;
						e.printStackTrace(Log.getWriter());
					} catch (IOException e) {
						// File was renamed under us or the OS disk is full?
						PRINT("ERROR: Could not write the new file Id to the Upload file\n"+e.getMessage());
						terminateDataLink();
						renameExtension(fileUploading, ERR);
						fileUploading = null;
						fileContinuationOffset = 0;
						e.printStackTrace(Log.getWriter());
					}
					if (Config.mainWindow != null)
						Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());
					state = UL_CMD_OK;
				} else if (err.getErrorCode() == FTL0Frame.ER_FILE_COMPLETE) {
					// This is already uploaded
					PRINT("Already Uploaded: " +fileUploading.getPath());
					renameExtension(fileUploading, UL);
					//File newFile = new File(fileUploading.getPath()+".ul");
					//boolean renamed = fileUploading.renameTo(newFile);
					if (Config.mainWindow != null)
						Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());
					fileUploading=null;
					fileContinuationOffset = 0;
					state = UL_CMD_OK;
				} else if (err.getErrorCode() == FTL0Frame.ER_NO_ROOM) {
					// This file will not fit, but another smaller one may.  So mark this file as an error but CMD_OK
					PRINT("ERROR: Not enough room on Pacsat to Upload: "+fileUploading.getPath()+"\n");
					renameExtension(fileUploading, ERR);
					//File newFile = new File(fileUploading.getPath()+".err");
					//fileUploading.renameTo(newFile);
					fileUploading=null;
					fileContinuationOffset = 0;
					state = UL_CMD_OK;
				} else {
					// unrecoverable error
					PRINT("ERROR: Can't Upload: "+fileUploading.getPath()+"\n");
					renameExtension(fileUploading, ERR);
					//File newFile = new File(fileUploading.getPath()+".err");
					//fileUploading.renameTo(newFile);
					terminateDataLink();
				}
				startT3(); // start T3
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				pgList =  Ax25Frame.makeString(frame.getBytes());
				// We are already open, don't need to change the status, just display
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(spacecraft.name, pgList);
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
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
				stopT3(); // stop T3
				break;
			case PacSatEvent.UL_DATA:
				PRINT("Uploading file: " + Long.toHexString(this.fileIdUploading) + ": " + req);
				if (MainWindow.frame != null)
					MainWindow.setFileUploading(spacecraft.name, fileIdUploading, fileContinuationOffset, fileUploadingLength);
				ULCmdFrame cmd = new ULCmdFrame(spacecraft, Config.get(Config.CALLSIGN), 
						spacecraft.get(SpacecraftSettings.BBS_CALLSIGN), req);
				Ax25Request lay2req = new Ax25Request(cmd.iFrame);
				spacecraft.layer2data.processEvent(lay2req);
				state = UL_DATA;
				startT3();
				break;
			case PacSatEvent.UL_DATA_END:
				PRINT("Sending DATA END for file: " + this.fileIdUploading);
				cmd = new ULCmdFrame(spacecraft, Config.get(Config.CALLSIGN), 
						spacecraft.get(SpacecraftSettings.BBS_CALLSIGN), req);
				lay2req = new Ax25Request(cmd.iFrame);
				spacecraft.layer2data.processEvent(lay2req);
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
					DEBUG("UL_NAK_RESP: " + frame);
				FTL0Frame err = (FTL0Frame)frame;
				PRINT("ERROR: "+err.getErrorString() +" NAK received while uploading: "+fileUploading.getPath()+"\n");
				//TODO - is the error unrecoverable - then mark file impossible
				renameExtension(fileUploading, ERR);
				//File newFile = new File(fileUploading.getPath()+".err");
				//fileUploading.renameTo(newFile);
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());
				// Must send Data end if we receive a NAK
				ULCmdFrame cmd = new ULCmdFrame(spacecraft, Config.get(Config.CALLSIGN), 
						spacecraft.get(SpacecraftSettings.BBS_CALLSIGN), new PacSatEvent(PacSatEvent.UL_DATA_END));
				Ax25Request lay2req = new Ax25Request(cmd.iFrame);
				spacecraft.layer2data.processEvent(lay2req);

				fileUploading=null;
				state = UL_CMD_OK;
				startT3(); // start T3
				break;	
			case PacSatFrame.PSF_UL_ERROR_RESP:
				DEBUG("UL_ERROR_RESP: " + frame);
				err = (FTL0Frame)frame;
				PRINT("ERROR: "+err.getErrorString() + " while uploading: "+fileUploading.getPath()+"\n");
				if (((FTL0Frame)frame).sentToCallsign(Config.get(Config.CALLSIGN))) {
					//TODO - is the error unrecoverable - then mark file impossible
					renameExtension(fileUploading, ERR);
					//newFile = new File(fileUploading.getPath()+".err");
					//fileUploading.renameTo(newFile);
					if (Config.mainWindow != null)
						Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());

					fileUploading=null;
					state = UL_CMD_OK;
					startT3(); // start T3
				}
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				pgList =  Ax25Frame.makeString(frame.getBytes());
				// We are already open, don't need to change the status, just display
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(spacecraft.name, pgList);
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
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
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
				FTL0Frame err = (FTL0Frame)frame;
				PRINT("NAK: "+err.getErrorString() +" received at DATA END: "+fileUploading.getPath()+"\n");
				// These errors are recoverable.  They mean the file was corrupted in transmission or the sat is full.  
				// So we try again from the start with this file.
				// ERR_BAD HEADER - no header or badly formed
				// ERR_HEADER_CHECK - PFH checksum failed
				// ER_BODY_CHECK - body checksum failed
				// ER_NO_ROOM - out of space
				
				// reset the fileId to 0.
				try {
					PacSatFile psf = new PacSatFile(spacecraft, fileUploading.getPath());
					psf.setFileId(0);
					psf.save();
				} catch (MalformedPfhException e) {
					PRINT("ERROR: The Pacsat File Header is corrupt for Upload file"+fileUploading.getPath()+"\n"+e.getMessage());
					terminateDataLink();
					renameExtension(fileUploading, ERR);
					//File newFile = new File(fileUploading.getPath()+".err");
					//fileUploading.renameTo(newFile);
					e.printStackTrace(Log.getWriter());
				} catch (IOException e) {
					// File was renamed under us or the OS disk is full?
					PRINT("ERROR: Could not write the new file Id to the Upload file\n"+e.getMessage());
					terminateDataLink();
					//File newFile = new File(fileUploading.getPath()+".err");
					renameExtension(fileUploading, ERR);
					//fileUploading.renameTo(newFile);
					e.printStackTrace(Log.getWriter());
				}
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());

				fileUploading=null; // Let the algorithm find this again when we get an Open message
				fileContinuationOffset = 0;
				state = UL_CMD_OK;
				startT3(); // start T3
				break;
			case PacSatFrame.PSF_UL_ACK_RESP:
				DEBUG("UL_ACK_RESP: " + frame);
				PRINT("SUCCESSFULLY UPLOADED: " + fileUploading.getPath());
				try {
					PacSatFile psf = new PacSatFile(spacecraft, fileUploading.getPath());
					psf.getPfh(spacecraft).setFileUploadDate();
					psf.save();
					if (Config.mainWindow != null) {
						Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());
						MainWindow.setFileUploading(spacecraft.name, fileIdUploading, fileUploadingLength, fileUploadingLength);
					}
				} catch (MalformedPfhException e) {
					PRINT("ERROR: The Pacsat File Header is corrupt for Upload file"+fileUploading.getPath()+"\n"+e.getMessage());
					terminateDataLink();
					renameExtension(fileUploading, ERR);
					//File newFile = new File(fileUploading.getPath()+".err");
					//fileUploading.renameTo(newFile);
					e.printStackTrace(Log.getWriter());
				} catch (IOException e) {
					// File was renamed under us or the OS disk is full?
					PRINT("ERROR: Could not write the new file Id to the Upload file\n"+e.getMessage());
					terminateDataLink();
					//File newFile = new File(fileUploading.getPath()+".err");
					renameExtension(fileUploading, ERR);
					//fileUploading.renameTo(newFile);
					e.printStackTrace(Log.getWriter());
				}
				renameExtension(fileUploading, UL);
				//newFile = new File(fileUploading.getPath()+".ul");
				//fileUploading.renameTo(newFile);
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());

				fileUploading=null;
				state = UL_CMD_OK;
				startT3(); // start T3
				break;
			case PacSatFrame.PSF_UL_ERROR_RESP:
				DEBUG("UL_ERROR_RESP: " + frame);
				err = (FTL0Frame)frame;
				PRINT("ERROR: "+err.getErrorString() +" received while uploading: "+fileUploading.getPath()+"\n");
				//TODO - is the error unrecoverable - then mark file impossible
				renameExtension(fileUploading, ERR);
				//newFile = new File(fileUploading.getPath()+".err");
				//fileUploading.renameTo(newFile);
				if (Config.mainWindow != null)
					Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());

				fileUploading=null;
				state = UL_CMD_OK;
				startT3(); // start T3
				break;
			case PacSatFrame.PSF_STATUS_BBSTAT:
				pgList =  Ax25Frame.makeString(frame.getBytes());
				// We are already open, don't need to change the status, just display
				if (MainWindow.frame != null)
					MainWindow.setPGStatus(spacecraft.name, pgList);
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
			MainWindow.setPGStatus(spacecraft.name, pgList);
	}

	public void attemptLogin() {
		if (state == UL_UNINIT)
			state = UL_OPEN;
	}

	private void terminateDataLink() {
		// close the connection
		Ax25Request req = new Ax25Request(Config.get(Config.CALLSIGN), spacecraft.get(SpacecraftSettings.BBS_CALLSIGN), Ax25Request.DL_DISCONNECT);
		spacecraft.layer2data.processEvent(req);
		state = UL_UNINIT; 
		fileUploading = null;
		if (MainWindow.frame != null) {
			MainWindow.setPGStatus(spacecraft.name, "");
			MainWindow.setLoggedin(spacecraft.name, "Disconnected");
		}
		if (Config.mainWindow != null)
			Config.mainWindow.setOutboxData(spacecraft.name, spacecraft.outbox.getTableData());
		stopT3(); // stop T3
	}

	/**
	 * We have a file like AC2CZ10.txt.out and it is to be renamed to AC2CZ10.txt.20190103.ul to show that is was uploaded
	 * The existing extension will always be .out
	 * 
	 * @param from
	 * @param ext
	 */
	private void renameExtension(File fromFile, String ext) {
		Date today = Calendar.getInstance().getTime();
		fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = fileDateFormat.format(today);

		String to = fromFile.getPath().replace(EditorFrame.OUT, "."+reportDate+"."+ext);
		File newFile = new File(to);

		if(fromFile.renameTo(newFile))
			return;
		
		///////////////// need a better check here to see if the rename works.  Otherwise the user may have to manually rename the file.
		
		// We could not rename to this file name
		Log.errorDialog("ERROR", "Could not rename the outbox file from: " + fromFile.getPath() + "\n"
				+ "to: " + newFile.getPath() + "\n"
				+ "You must rename this file manually or it may accidently block future uploads.");
	}

	private void DEBUG(String s) {
		s = "DEBUG 3: " + states[state] + ": " + s;
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

		File nextFile = spacecraft.outbox.getNextFile();
		if (nextFile != null && (fileUploading == null)) { // we have a file and we are not already attempting to upload

			// We issue LOGIN REQ event
			PRINT("Ready to upload file: "+ nextFile.getName());
			fileUploading = nextFile;
			// Create a connection request.  
			//Ax25Request.DL_CONNECT
			Ax25Request req = new Ax25Request(Config.get(Config.CALLSIGN), spacecraft.get(SpacecraftSettings.BBS_CALLSIGN));
			spacecraft.layer2data.processEvent(req);
			state = UL_OPEN; // we stay in open until actually logged in, then we are in CMD_OK

		}
	}

	private void requestIfFile() {
		if (fileUploading.exists()) {
			PacSatFile psf;
			try {
				psf = new PacSatFile(spacecraft, fileUploading.getPath());
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
		t3_timer = 1;
	}

	private void stopT3() {
		t3_timer = 0;
	}

	@Override
	public void run() {
		Log.println("STARTING UPLINK Thread");
		Thread.currentThread().setName("UplinkStateMachine");

		while (running) {
			
			if (t3_timer > 0) {
				// we are timing something
				t3_timer++;
				if (t3_timer > TIMER_T3) {
					t3_timer = 0;
					DEBUG(">>>>>>>>>>T3 expired");
					nextState(new PacSatEvent(PacSatEvent.UL_TIMER_T3_EXPIRY));
				}				
			}
			// This should not be needed but it copes with a subtle bug where the data link can be disconnected
			// but we still think we are uploading a file
			if (spacecraft.layer2data != null && spacecraft.layer2data.isDisconnected() && fileUploading != null) {
				fileUploading = null;
				if (MainWindow.frame != null) {
					MainWindow.setPGStatus(spacecraft.name, "");
					MainWindow.setLoggedin(spacecraft.name, "Disconnected");
				}
				stopT3(); // stop T3, clearly it was running
			}
			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
			} else if (state == UL_OPEN) {
				if (spacecraft != null)
				if (spacecraft.getBoolean(SpacecraftSettings.UPLOAD_FILES))
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
			} else if (state == UL_DATA && fileUploading != null && spacecraft.layer2data.isReadyForData()) {
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
						fileUploadingLength = fileOnDisk.length();
						
						fileOnDisk.seek(fileContinuationOffset);
						int[] bytes = new int[(int) length];
						int i = 0;
						while (i < length)
							bytes[i++] = fileOnDisk.readUnsignedByte();
						fileOnDisk.close(); // Explicitly close file to make sure it is not open if we process an error and need to rename it
						processEvent(new PacSatEvent(bytes, fileContinuationOffset, fileUploadingLength));						
						fileContinuationOffset = fileContinuationOffset + PACKET_SIZE; // rather than add length we add the packet size, so it overflows for DATA_END
					} else {
						fileOnDisk.close(); // Explicitly close file to make sure it is not open if we process an error and need to rename it
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
				Config.mainWindow.setUplinkStatus(spacecraft.name, states[state]);
		}
		Log.println("EXIT UPLINK Thread");

	}

	//	class Timer3Task extends TimerTask {
	//		public void run() {
	//			DEBUG("UL_T3 expired");
	//			nextState(new PacSatEvent(PacSatEvent.UL_TIMER_T3_EXPIRY));
	//		}
	//	}

}
