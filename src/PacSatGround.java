import gui.InitalSettings;
import gui.MainWindow;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.g0kla.telem.gui.ProgressPanel;

import common.Config;
import common.Log;

public class PacSatGround {
	static int REQUIRED_JAVA_VERSION = 8;
	

	public static String HELP = "AMSAT PacSat Ground Station. Version " + Config.VERSION +"\n\n"
			+ "Usage: PacSatGround [-version][-s] \n";
	static String seriousErrorMsg;
	static String logFileDir = null;
    
	public static void main(String[] args) {
		try {
		String javaVersion = System.getProperty("java.specification.version");
		int release = Integer.parseInt(javaVersion.split("\\.")[1]);  // need to escape the period and take the second part
		if (release < REQUIRED_JAVA_VERSION) {
			Log.errorDialog("Java Version Error", "PacsatGround needs Java Version "+REQUIRED_JAVA_VERSION+" or higher.  You are using Java Version: " + release  +"\n"
					+ "Please install a later version of Java.");
			System.exit(1);
		}
		} catch (Exception e) {
			// Failing this check should not be fatal.  Some versions of java return the version in 
			// an unknown way.  Just carry on and crash later if needed :)
		}
		
		ProgressPanel initProgress = new ProgressPanel(MainWindow.frame, "Initializing AMSAT Pacsat Ground station, please wait ...", false);
		initProgress.setVisible(true);

		int arg = 0;
		while (arg < args.length) {
			if (args[arg].startsWith("-")) { // this is a switch
				if ((args[arg].equalsIgnoreCase("-h")) || (args[arg].equalsIgnoreCase("-help")) || (args[arg].equalsIgnoreCase("--help"))) {
					System.out.println(HELP);
					System.exit(0);
				}
				if ((args[arg].equalsIgnoreCase("-v")) || (args[arg].equalsIgnoreCase("-version"))) {
					System.out.println("AMSAT PacSat Ground Station. Version " + Config.VERSION);
					System.exit(0);
				}
			} else {
				// we have no more switches, so start reading command line paramaters
				logFileDir = args[arg];
			}
			arg++;

		}

		Config.init("PacSatGround.properties");
		File current = new File(System.getProperty("user.dir"));
		Config.currentDir = current.getAbsolutePath();
		if (logFileDir == null) {
			File home = new File(System.getProperty("user.home") + File.separator + "PacsatGroundConfig");
			Config.homeDir = home.getAbsolutePath();
		} else {
			Config.logDirFromPassedParam = true;
			File log = new File(logFileDir);
			Config.set(Config.LOGFILE_DIR, logFileDir);
			Config.homeDir = log.getAbsolutePath();
		}
		
		PacSatGround pg = new PacSatGround();
		if (Config.missing()) {
			// Then this is the first time we have run FoxTelem on this computer
			Config.setHome();
			pg.initialRun();
		}
		Config.load();
		Log.init(Config.get(Config.LOGFILE_DIR) + File.separator + "PacSatGround");
		try {
			Config.start();
		} catch (com.g0kla.telem.data.LayoutLoadException e) {
			Log.errorDialog("ERROR", "Could not load the layout, telem DB has not been initiailized.  No telemetry can be stored.\n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.errorDialog("ERROR", "File error during initialization:\n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
		invokeGUI();
		initProgress.updateProgress(100);

	}

	public void initialRun() {

		try { //to set the look and feel to be the same as the platform it is run on
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// Silently fail if we can't do this and go with default look and feel
		}

		try {
			JFrame window = new JFrame();
			window.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/pacsat.jpg")));
			// INITIAL RUN
			InitalSettings f = new InitalSettings(window, true);
			f.setVisible(true);

		} catch (Exception e) {
			e.printStackTrace();
			Log.errorDialog("SERIOUS ERROR - Uncaught and thrown from Initial Setup", e.getMessage());

		}
	}

	/**
	 * Start the GUI
	 */
	public static void invokeGUI() {
		
		// Need to set the apple menu property in the main thread.  This is ignored on other platforms
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		//System.setProperty("apple.awt.fileDialogForDirectories", "true");  // fix problems with the file chooser
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
	    System.setProperty("sun.awt.exception.handler",
	                       ExceptionHandler.class.getName());
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try { //to set the look and feel to be the same as the platform it is run on
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					// Silently fail if we can't do this and go with default look and feel
					//e.printStackTrace();
				}

				try {
					Config.mainWindow = new MainWindow(); // a handle for other classes
					try {
						Config.mainWindow.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/pacsat.jpg")));
					} catch (Exception e) { }; // ignore, means we have no icon
					Config.mainWindow.setVisible(true);
				} catch (Exception e) {
					Log.println("SERIOUS ERROR - Uncaught and thrown from GUI");
					seriousErrorMsg = "Something is preventing the AMSAT Ground Station from running: " + e.getMessage() + "\n"
							+ "If you recently changed something\n"
							+ "try reverting to an older version, or install the standard files.  \n"
							+ "If that does not work then you can try deleting the PacSatGround.properties\n"
							+ "file in your home directory though this will delete your settings\n";
					e.printStackTrace();
					e.printStackTrace(Log.getWriter());
					EventQueue.invokeLater(new Runnable() {
				        @Override
				        public void run(){
				        	Frame frm = new Frame();
				        	JOptionPane.showMessageDialog(frm,
				        			seriousErrorMsg,
									"SERIOUS ERROR - Uncaught and thrown from GUI",
									JOptionPane.ERROR_MESSAGE) ;
				        	System.exit(99);
							
				        }
					});
				
					
				}
			}
		});		
	}

	/**
	 * Inner class to handle exceptions in the Event Dispatch Thread (EDT)
	 * @author chris.e.thompson
	 *
	 */
	public static class ExceptionHandler
	implements Thread.UncaughtExceptionHandler {

		public void handle(Throwable thrown) {
			// for EDT exceptions
			handleException(Thread.currentThread().getName(), thrown);
		}

		public void uncaughtException(Thread thread, Throwable thrown) {
			// for other uncaught exceptions
			handleException(thread.getName(), thrown);
		}

		protected void handleException(String tname, Throwable thrown) {
			thrown.printStackTrace(Log.getWriter());
			String stacktrace = Log.makeShortTrace(thrown.getStackTrace());  
			Log.errorDialog("SERIOUS EDT ERROR", "Error: " + thrown + "\n"
					+ "Exception on " + tname + "\n" + stacktrace);
		}
	}

	
	
}
