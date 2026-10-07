package passControl;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import pacSat.frames.PacSatPrimative;

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
		// TODO Auto-generated method stub
	
	}
	
	@Override
	protected void nextState(PacSatPrimative frame) {
		// TODO Auto-generated method stub
	
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
			
		}
	}
}
