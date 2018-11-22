package passControl;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import common.Spacecraft;
import pacSat.TncDecoder;
import pacSat.frames.PacSatFrame;

public abstract class PacsatStateMachine {

	protected int state;
	protected TncDecoder tncDecoder;
	protected ConcurrentLinkedQueue<PacSatFrame> frameEventQueue;
	protected boolean running = true;
	JTextArea ta;
	Spacecraft spacecraft;

	public static final int WAIT_TIME = 3000; // length of time in ms to wait for a response from spacecraft
	public static final int MAX_RETRIES = 5; // max number of times we will send command
	
	int waitTimer = 0; // time how long we are in the wait state
	int retries = 0;
	PacSatFrame lastCommand = null;

	PacsatStateMachine(Spacecraft sat) {
		this.spacecraft = sat;
		frameEventQueue = new ConcurrentLinkedQueue<PacSatFrame>();
	}
	
	public abstract void processEvent(PacSatFrame frame);
	protected abstract void nextState(PacSatFrame frame);
	
	public void setTncDecoder(TncDecoder tnc, JTextArea ta) {
		tncDecoder = tnc;
		this.ta = ta;
	}
	
}
