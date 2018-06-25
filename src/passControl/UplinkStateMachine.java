package passControl;

import common.Config;
import common.Log;
import common.Spacecraft;
import jssc.SerialPortException;
import pacSat.TncDecoder;
import pacSat.frames.KissFrame;
import pacSat.frames.RequestDirFrame;

public class UplinkStateMachine {
	public static final int UL_UNINIT = 0;
	public static final int UL_CMD_OK = 1;

	
	int state = UL_CMD_OK;
	
	public UplinkStateMachine() {
		state = UL_CMD_OK;
	}
	
	
	
	public void processEvent(PacSatEvent event) {
		nextState(event);
	}

	private void nextState(PacSatEvent event) {
		switch (state) {
		case UL_CMD_OK:
			ul_cmd_ok(event);
			break;
			
		default:
			break;
		}
	}
	
	private void ul_cmd_ok(PacSatEvent event) {
		
	}

}
