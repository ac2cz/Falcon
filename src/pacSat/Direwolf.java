package pacSat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Line.Info;

import common.Config;
import common.Log;
import gui.MainWindow;

public class Direwolf {

	static Mixer[] mixerList;
	static String[] outputDeviceNames;
	static String[] inputDeviceNames;

	private static String direwolfExePath = Config.currentDir + "/direwolf/direwolf.exe";
	private static String direwolfLogPath = "direwolf.log";
	private static String direwolfConfPath = "direwolf.conf";

	/** How long direwolf must stay up before a start counts as "healthy" and the failure count resets */
	private static final long MIN_HEALTHY_RUNTIME_MS = 30_000;
	/** Give up (and warn the user) after this many rapid, consecutive failures with no healthy run in between */
	private static final int MAX_RAPID_FAILURES = 5;

	// Written by the monitor thread, read by exit()/isAlive() on other threads -> volatile
	private volatile Process direwolfProcess = null;
	private volatile boolean stopRequested = false;
	private Thread monitorThread = null;

	public Direwolf() {
		if (Config.get(Config.DIREWOLF_PTT).equals(Config.NONE)) {
			Log.errorDialog("PTT Not Defined", "Could not create direwolf config file\n");
			return;
		}
		if (Config.get(Config.DIREWOLF_INPUT_DEV).equals(Config.NONE)) {
			Log.errorDialog("Input audio device not defined", "Could not create direwolf config file\n");
			return;
		}
		if (Config.get(Config.DIREWOLF_OUTPUT_DEV).equals(Config.NONE)) {
			Log.errorDialog("Output audio device not defined", "Could not create direwolf config file\n");
			return;
		}

		File direwolfConf = new File(getConfFilePath());
		if (!direwolfConf.exists())
			makeConfigFile();

		// The monitor thread owns the process lifecycle: launch, wait, restart on failure.
		monitorThread = new Thread(this::monitorLoop, "Direwolf-Monitor");
		monitorThread.setDaemon(true);
		monitorThread.start();
	}

	/**
	 * Build the ProcessBuilder and start a fresh direwolf process. Called for the first launch
	 * and for every restart. Throws if the process cannot be started.
	 */
	private Process launch() throws IOException {
		File direwolfExe = new File(direwolfExePath);
		ProcessBuilder pb = new ProcessBuilder(
				direwolfExe.getAbsolutePath(),   // the bundled exe, direct — no cmd /c
				"-c", new File(getConfFilePath()).getAbsolutePath(),
				"-r", "48000",
				"-B", Config.get(Config.DIREWOLF_BAUD_RATE),
				"-X", Config.get(Config.DIREWOLF_FEC),
				"-q", "d",
				"-t", "0"                        // disable ANSI color so logs parse cleanly
				);
		pb.directory(direwolfExe.getParentFile());   // so it finds its DLLs/support files
		pb.redirectErrorStream(true);                // merge stderr into stdout
		// Append rather than truncate, so restart history stays in one log file
		pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(getLogFilePath())));

		Log.println("Launching: " + pb.command());
		return pb.start();
	}

	/**
	 * Supervise direwolf. Launch it, block until it dies, and restart it unless we were asked to
	 * stop. Rapid repeated failures back off and eventually give up, so we don't spin forever on a
	 * broken config or a missing audio device. A start that stays healthy for a while resets the
	 * failure count, so a genuine one-off crash after long uptime doesn't count against that budget.
	 */
	private void monitorLoop() {
		int rapidFailures = 0;

		while (!stopRequested) {
			long startedAt = System.currentTimeMillis();

			Process p;
			try {
				p = launch();
				direwolfProcess = p;
			} catch (IOException e) {
				MainWindow.setTncConnection(false, "Direwolf error");
				Log.errorDialog("ERROR", "Could not launch direwolf. Check File > Direwolf Log for errors\n" + e.getMessage());
				if (++rapidFailures >= MAX_RAPID_FAILURES) {
					giveUp(rapidFailures);
					return;
				}
				sleepQuietly(restartDelayMs(rapidFailures));
				continue;
			}

			// Give it a moment to settle, then report the connection up. Whoever owns the KISS
			// socket should (re)connect on this "up" transition — direwolf's listener is brand new.
			sleepQuietly(1000);
			if (p.isAlive())
				MainWindow.setTncConnection(true, "Direwolf: " + Config.get(Config.DIREWOLF_BAUD_RATE) + "bps");

			// Block here until direwolf exits for any reason (crash, or our own destroy() in exit()).
			int code = -1;
			try {
				code = p.waitFor();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// fall through: the stopRequested check below decides whether to relaunch
			}

			MainWindow.setTncConnection(false, "Direwolf exit (code " + code + ")");

			if (stopRequested)
				break;

			long ranForMs = System.currentTimeMillis() - startedAt;
			if (ranForMs >= MIN_HEALTHY_RUNTIME_MS)
				rapidFailures = 0;        // ran fine then exited — restart promptly (delay 0)
			else
				rapidFailures++;          // died fast — likely config/audio; back off

			if (rapidFailures >= MAX_RAPID_FAILURES) {
				giveUp(rapidFailures);
				return;
			}

			long delay = restartDelayMs(rapidFailures);
			Log.println("Direwolf exited (code " + code + ") after " + ranForMs
					+ "ms — restarting in " + delay + "ms (consecutive failure " + rapidFailures + ")");
			sleepQuietly(delay);
		}
		Log.println("Direwolf monitor stopped");
	}

	/** Delay before the next restart: 0 after a healthy run, growing with consecutive fast failures. */
	private static long restartDelayMs(int consecutiveFailures) {
		return consecutiveFailures <= 0 ? 0 : Math.min(consecutiveFailures * 2_000L, 30_000L);
	}

	private void sleepQuietly(long ms) {
		if (ms <= 0)
			return;
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void giveUp(int failures) {
		MainWindow.setTncConnection(false, "Direwolf failed");
		Log.errorDialog("Direwolf keeps failing",
				"Direwolf failed to stay running after " + failures + " attempts.\n"
				+ "Check File > Direwolf Log, your audio devices, and PTT settings.\n");
	}

	static public String getConfFilePath() {
		return Config.get(Config.LOGFILE_DIR) + "\\" + direwolfConfPath;
	}

	static public String getLogFilePath() {
		return Config.get(Config.LOGFILE_DIR) + "\\" + direwolfLogPath;
	}

	public boolean isAlive() {
		Process p = direwolfProcess;
		return p != null && p.isAlive();
	}

	public void exit() {
		stopRequested = true;                 // tell the monitor not to relaunch
		Process p = direwolfProcess;
		if (p != null)
			p.destroy();                      // trips waitFor() in the monitor
		if (monitorThread != null)
			monitorThread.interrupt();        // in case it's mid-backoff or mid-settle sleep
	}

	public static void makeConfigFile() {
		File direwolfConf = new File(Config.get(Config.LOGFILE_DIR) + "/" + direwolfConfPath);
		/* Create the direwolf.conf file */

		String conf = "#####################################################################\r\n"
				+ "#                                                                   #\r\n"
				+ "#               Configuration file for Dire Wolf                    #\r\n"
				+ "#  Use File > Settings to set paramaters and regenerate this file   #\r\n"
				+ "#  Or edit directly if you have special settings                    #\r\n"
				+ "#                   Windows version                                 #\r\n"
				+ "#                                                                   #\r\n"
				+ "#####################################################################\r\n"
				+ "";
		String input_string = Config.get(Config.DIREWOLF_INPUT_DEV);
		if (input_string.length() > 31)
			input_string = input_string.substring(0, 30);
		String output_string = Config.get(Config.DIREWOLF_OUTPUT_DEV);
		if (output_string.length() > 31)
			output_string = output_string.substring(0, 30);
		conf = conf + "ADEVICE \""+   input_string + "\" \"" + output_string + "\"\r\n";
		conf = conf + "\r\n";
		conf = conf + "CHANNEL 0\r\n";
		conf = conf + "FULLDUP ON\r\n";
		conf = conf + "MYCALL " + Config.get(Config.CALLSIGN) + "\r\n";
		conf = conf + "MODEM " + Config.get(Config.DIREWOLF_BAUD_RATE) + "\r\n";
		conf = conf + Config.get(Config.DIREWOLF_PTT)+ "\r\n";
		conf = conf + "\r\n";
		conf = conf + "AGWPORT 8000\r\n";
		conf = conf + "KISSPORT "+ Config.get(Config.TNC_TCP_PORT)+ "\r\n";

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(direwolfConf))) {
			writer.write(conf);
			System.out.println("Direwolf Config File created successfully");
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not create direwolf config file\n" + e.getMessage());
		}
	}

	public static String[] getAudioSinks() {

		int device = 1;
			//AudioFormat audioFmt = getAudioFormat();
		    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		    mixerList = new Mixer[mixers.length+1];
			String[] devices = new String[mixers.length+1];

		    for (Mixer.Info info : mixers) 
		    {
		        Mixer mixer = AudioSystem.getMixer(info);
		        try
		        {
		        //  System.out.println(info);
		            Info sdlLineInfo = new DataLine.Info(SourceDataLine.class, getAudioFormat());

		            // test if line is assignable
		            SourceDataLine sdl = (SourceDataLine) mixer.getLine(sdlLineInfo);
		            sdl.close();
		            // if successful, add to list
		            mixerList[device] = mixer;
		            String name = info.getName();
		            if (name.length() > 50) name = name.substring(0,50);
		            //System.out.println(device + ": " + name); //.getName()+ " " + info.getDescription(); // + " <> " + info.getDescription();
		            devices[device++] = name; 
		        }
		        catch (LineUnavailableException e) 
		        {
		            //System.err.println("Mixer rejected, Line Unavailable: " + info);
		        }
		        catch (IllegalArgumentException e)
		        {
		            //System.err.println("Mixer rejected, Illegal Argument: " + info);
		        }           
		    }
		    outputDeviceNames = new String[device];
		    for (int i=0; i< device; i++)
		    	outputDeviceNames[i] = devices[i];
		return outputDeviceNames;
	}

	public static String[] getAudioSources() {

		int device = 1;
			//AudioFormat audioFmt = getAudioFormat();
		    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		    mixerList = new Mixer[mixers.length+1];
			String[] devices = new String[mixers.length+1];

		    for (Mixer.Info info : mixers) 
		    {
		        Mixer mixer = AudioSystem.getMixer(info);
		        try
		        {
		        //  System.out.println(info);
		        	Log.println("Found audio Device: " + info.getName() + " Desc:" + info.getDescription());
		            Info dataLineInfo = new DataLine.Info(TargetDataLine.class, getAudioFormat());

		            // test if line is assignable
		            TargetDataLine targetDataLine = (TargetDataLine)mixer.getLine(dataLineInfo);
	            	targetDataLine.close(); // close it again so it is available when we need it
		            // if successful, add to list
		            mixerList[device] = mixer;
		            String name = info.getName();
		            if (name.length() > 50) name = name.substring(0,50);
		            //System.out.println(device + ": " + name); //.getName()+ " " + info.getDescription(); // + " <> " + info.getDescription();
		            devices[device++] = name; 
		        }
		        catch (LineUnavailableException e) 
		        {
		            //System.err.println("Mixer rejected, Line Unavailable: " + info);
		        }
		        catch (IllegalArgumentException e)
		        {
		            //System.err.println("Mixer rejected, Illegal Argument: " + info);
		        }           
		    }
		    inputDeviceNames = new String[device];
		    for (int i=0; i< device; i++)
		    	inputDeviceNames[i] = devices[i];
		return inputDeviceNames;
	}

	/**
	 * Get the audio format for output to the speaker. This format is only used to find the sound cards initially and to test that they work with a 
	 * standard value.  The actual rate is determined when new is called.
	 * @return
	 */
	static AudioFormat getAudioFormat(){
	    float sampleRate = 48000; 
	    int sampleSizeInBits = 16;
	    int channels = 2;
	    boolean signed = true;
	    boolean bigEndian = false;
	    return new AudioFormat( sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

}