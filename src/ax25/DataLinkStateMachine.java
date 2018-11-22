package ax25;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import common.Config;
import common.Log;
import common.Spacecraft;
import fileStore.DirHole;
import fileStore.FileHole;
import fileStore.PacSatFile;
import fileStore.SortedArrayList;
import gui.MainWindow;
import pacSat.TncDecoder;
import pacSat.frames.PacSatFrame;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.RequestFileFrame;
import pacSat.frames.StatusFrame;

public class DataLinkStateMachine implements Runnable {

	public static final int DISCONNECTED = 0;
	public static final int AWAITING_CONNECTION = 1;
	public static final int AWAITING_RELEASE = 2;
	public static final int CONNECTED = 3;
	public static final int TIMER_RECOVERY = 4;
	
	public static final String[] states = {
			"Disc",
			"Wait Conn",
			"Wait Rel",
			"Connected",
			"Timer Rec"
	};
	protected int state;
	protected TncDecoder tncDecoder;
	protected ConcurrentLinkedQueue<Ax25Frame> frameEventQueue;
	protected ConcurrentLinkedQueue<Iframe> iFrameQueue;
	protected boolean running = true;
	JTextArea ta;
	Spacecraft spacecraft;

	public static final int WAIT_TIME = 3000; // length of time in ms to wait for a response from spacecraft
	public static final int MAX_RETRIES = 5; // max number of times we will send command
	
	int waitTimer = 0; // time how long we are in the wait state
	int retries = 0;

	DataLinkStateMachine(Spacecraft sat) {
		this.spacecraft = sat;
		iFrameQueue = new ConcurrentLinkedQueue<Iframe>();
		frameEventQueue = new ConcurrentLinkedQueue<Ax25Frame>();
		state = DISCONNECTED;
	}
	
	public void processEvent(Ax25Frame frame) {
		Log.println("Adding DOWN LINK Event: " + frame.toString());
		frameEventQueue.add(frame);
	}
	
	protected void nextState(Ax25Frame frame) {

		switch (state) {
		case DISCONNECTED:
			stateDisc(frame);
			break;
		case AWAITING_CONNECTION:
			stateWaitConn(frame);
			break;
		case AWAITING_RELEASE:
			stateWaitRelease(frame);
			break;
		case CONNECTED:
			stateConnected(frame);
			break;
		case TIMER_RECOVERY:
			stateTimerRec(frame);
			break;
		default:
			break;
		}

	}
	
	private void stateDisc(Ax25Frame frame) {
		switch (frame.type) {
		case Ax25Frame.TYPE_U_SABM:
			break;
		default:
			break;
		}
		
	}

	private void stateWaitConn(Ax25Frame frame) {
		switch (frame.type) {
		case Ax25Frame.TYPE_U_SABM:
			break;
			
			default:
				break;
		}
		
	}

	private void stateWaitRelease(Ax25Frame frame) {
		switch (frame.type) {
		case Ax25Frame.TYPE_U_SABM:
			break;
			
			default:
				break;
		}
		
	}
	
	private void stateConnected(Ax25Frame frame) {
		switch (frame.type) {
		case Ax25Frame.TYPE_U_SABM:
			break;
			
			default:
				break;
		}
		
	}

	private void stateTimerRec(Ax25Frame frame) {
		switch (frame.type) {
		case Ax25Frame.TYPE_U_SABM:
			break;
			
			default:
				break;
		}
		
	}

	
	public void setTncDecoder(TncDecoder tnc, JTextArea ta) {
		tncDecoder = tnc;
		this.ta = ta;
	}

	@Override
	public void run() {
		Log.println("STARTING DL Thread");
		while (running) {
			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
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
