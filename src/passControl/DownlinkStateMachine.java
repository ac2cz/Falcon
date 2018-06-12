package passControl;

public class DownlinkStateMachine {

	int state;
	public static final int DL_UNINIT = 0;
	public static final int DL_CMD_OK = 1;
	public static final int DL_WAIT = 2;
	public static final int DL_DATA = 3;
	public static final int DL_END = 4;
	public static final int DL_ABORT = 5;
	public static final int DL_DIR_WAIT = 6;
	public static final int DL_DIR_DATA = 7;

	public static final int EVENT_USER_REQ_DL = 0;
	public static final int EVENT_USER_REQ_DIR = 1;
	public static final int EVENT_USER_REQ_SELECT = 2;
	public static final int EVENT_USER_REQ_ABORT = 3;
	public static final int EVENT_DL_ABORTED_RESP = 4;
	public static final int EVENT_DL_COMPLETED_RESP = 5;
	public static final int EVENT_SELECT_RESP = 6;
	public static final int EVENT_LOGIN_RESP = 7;
	public static final int EVENT_DATA = 8;
	public static final int EVENT_DATA_END = 9;
	public static final int EVENT_DL_ERROR_RESP = 10;

	private void nextState(int event) {
		switch (state) {
		case DL_CMD_OK:
			dl_cmd_ok(event);
			break;
			
		case DL_WAIT:
			dl_wait(event);
			break;
			
		case DL_DATA:
			dl_data(event);
			break;
		case DL_END:
			dl_end(event);
			break;
		case DL_ABORT:
			
			break;
		case DL_DIR_WAIT:
			
			break;
		case DL_DIR_DATA:
			
			break;
		default:
			break;
		}

	}
	private void dl_cmd_ok(int event) {
		switch (event) {
		case EVENT_USER_REQ_DL:
			//transmit(DL_CMD_PACKET);
			state = DL_WAIT;
			break;
		}
	}
	
	private void dl_wait(int event) {
		switch (event) {
		case EVENT_DL_ERROR_RESP:
			state = DL_CMD_OK;
			break;
		case EVENT_DATA:
			//store data
			//mark file incomplete
			state = DL_DATA;
			break;
		case EVENT_DATA_END:
			/* if file at client is OK
				mark file data ok
				Save <continue offset> equal to file length
				transmit(DL_ACK_CMD)
			   else
			   	mark file incomplete
			   	save <continue_offset> of 0
			   	transmit(DL_NAK_CMD>
			*/
			state = DL_END;
			break;
		}
	}
	
	private void dl_data(int event) {
		switch (event) {
		case EVENT_DATA:
			//store data
			//mark file incomplete
			state = DL_DATA;
			break;
		case EVENT_DATA_END:
			/* if file at client is OK
				transmit(DL_ACK_CMD)
			   else
			   	transmit(DL_NAK_CMD>
			*/
			state = DL_END;
			break;
		}
	}
	
	private void dl_end(int event) {
		switch (event) {
		case EVENT_USER_REQ_DL:
			//transmit(DL_CMD_PACKET);
			state = DL_WAIT;
			break;
		}
	}
}
