package passControl;

import java.util.concurrent.ConcurrentLinkedQueue;

import common.Log;
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
 * This is event driven, only updating when we receive a new frame.  However it also needs to have a background 
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
	public static final int DL_PB_FULL = 3; // PB is full, we need to wait
	public static final int DL_PB_SHUT = 4; // PB is shut, we need to wait

	public static final String[] states = {
			"Listening",
			"Heard",
			"On",
			"Full",
			"Shut"
	};
	
	String pbList = "";
	String pgList = "";
	
	public DownlinkStateMachine() {
		state = DL_LISTEN;
		frameEventQueue = new ConcurrentLinkedQueue<PacSatFrame>();
	}
	
	public void setTncDecoder(TncDecoder tnc) {
		tncDecoder = tnc;
	}
	
	public void processEvent(PacSatFrame frame) {
		frameEventQueue.add(frame);
	}
	
	private void nextState(PacSatFrame frame) {
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
			MainWindow.setPBStatus(pbList);
			break;
			
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response when we don't think we are in a pass, we ignore this
			break;
		case PacSatFrame.PSF_RESPONSE_ERROR: // we have an ERR response when we don't think we are in a pass, we ignore this
			break;
			
		case PacSatFrame.PSF_REQ_DIR:
			Log.infoDialog("Ignored", "Wait until the Spacecraft PB status has been heard before requesting a directory");
			break;
		case PacSatFrame.PSF_REQ_FILE:
			Log.infoDialog("Ignored", "Wait until the Spacecraft PB status has been heard before requesting a file");
			break;
		case PacSatFrame.PSF_STATUS_BBSTAT:
			state = DL_ON_PB;
			pgList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPGStatus(pgList);
			break;
		case PacSatFrame.PSF_STATUS_PBFULL:
			state = DL_PB_FULL;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_PBSHUT:
			state = DL_PB_SHUT;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
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
			MainWindow.setPBStatus(pbList);
			break;
			
		case PacSatFrame.PSF_RESPONSE_OK: // we have an OK response, so we must now be on the PB
			state = DL_ON_PB;
			pbList =  "ON THE PB: " + UiFrame.makeString(frame.getBytes());
			break;
			
		case PacSatFrame.PSF_STATUS_BBSTAT:
			state = DL_ON_PB;
			pgList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPGStatus(pgList);
			break;
		case PacSatFrame.PSF_STATUS_PBFULL:
			state = DL_PB_FULL;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_PBSHUT:
			state = DL_PB_SHUT;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
			break;
		default:
			break;
		}
	}
	
	private void stateOnPb(PacSatFrame frame) {
		switch (frame.frameType) {
		case PacSatFrame.PSF_REQ_DIR: // Requested a DIR but we are already on the PB, so ignore
			break;
		case PacSatFrame.PSF_REQ_FILE: // Requested a FIEL but we are already on the PB, so ignore
			break;
		case PacSatFrame.PSF_STATUS_PBLIST:
			if (((StatusFrame)frame).containsCall()) {
				state = DL_ON_PB;
				pbList =  UiFrame.makeString(frame.getBytes());
			} else {
				state = DL_PB_OPEN;
				pbList = UiFrame.makeString(frame.getBytes());
			}
			MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_BBSTAT:
			state = DL_ON_PB;
			pgList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPGStatus(pgList);
			break;
		case PacSatFrame.PSF_STATUS_PBFULL:
			state = DL_PB_FULL;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_PBSHUT:
			state = DL_PB_SHUT;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
			break;
		default:
			break;
		}
	}

	@Override
	public void run() {
		while (running) {
			if (frameEventQueue.size() > 0)
				nextState(frameEventQueue.poll());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
}
