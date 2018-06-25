package passControl;

import java.nio.charset.StandardCharsets;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import jssc.SerialPortException;
import pacSat.TncDecoder;
import pacSat.frames.KissFrame;
import pacSat.frames.PacSatFrame;
import pacSat.frames.RequestDirFrame;
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
 */
public class DownlinkStateMachine extends StateMachine {

	int state;
	private TncDecoder tncDecoder;
	
	public static final int DL_UNINIT = 0;
	public static final int DL_PB_LIST = 2;
	public static final int DL_PB_FULL = 3;
	public static final int DL_PB_SHUT = 4;

	String pbList = "";
	String pgList = "";
	
	public DownlinkStateMachine() {
		state = DL_UNINIT;
	}
	
	public void setTncDecoder(TncDecoder tnc) {
		tncDecoder = tnc;
	}
	
	public void processEvent(PacSatFrame frame) {
		nextState(frame);
	}
	
	private void nextState(PacSatFrame frame) {
		switch (state) {
		case DL_UNINIT:
			stateInit(frame);
			break;
			
		case DL_PB_LIST:
			statePbList(frame);
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
			state = DL_PB_LIST;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_BBSTAT:
			state = DL_PB_LIST;
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
	
	private void statePbList(PacSatFrame frame) {
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
		case PacSatFrame.PSF_STATUS_PBLIST:
			state = DL_PB_LIST;
			pbList =  UiFrame.makeString(frame.getBytes());
			MainWindow.setPBStatus(pbList);
			break;
		case PacSatFrame.PSF_STATUS_BBSTAT:
			state = DL_PB_LIST;
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
	
	
}
