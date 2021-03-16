package passControl;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import common.SpacecraftSettings;
import gui.MainWindow;
import pacSat.TncDecoder;
import pacSat.frames.PacSatFrame;
import pacSat.frames.PacSatPrimative;

public abstract class PacsatStateMachine {

	protected int state;
	protected TncDecoder tncDecoder;
	protected ConcurrentLinkedQueue<PacSatPrimative> frameEventQueue;
	protected boolean running = true;
	MainWindow ta;
	SpacecraftSettings spacecraft;

	public static final int WAIT_TIME = 3000; // length of time in ms to wait for a response from spacecraft
	public static final int MAX_RETRIES = 10; // max number of times we will send command
	
	int waitTimer = 0; // time how long we are in the wait state
	int retries = 0;
	PacSatFrame lastCommand = null;

	PacsatStateMachine(SpacecraftSettings sat) {
		this.spacecraft = sat;
		frameEventQueue = new ConcurrentLinkedQueue<PacSatPrimative>();
	}

	
	public abstract void processEvent(PacSatPrimative frame);
	protected abstract void nextState(PacSatPrimative frame);
	
	public void setTncDecoder(TncDecoder tnc, MainWindow ta) {
		tncDecoder = tnc;
		this.ta = ta;
	}

	
}
