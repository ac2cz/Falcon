import gui.MainWindow;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import common.Config;
import common.LayoutLoadException;
import common.Log;

public class PacSatGround {
	public static String HELP = "AMSAT PacSat Ground Station. Version " + Config.VERSION +"\n\n"
			+ "Usage: PacSatGround [-version][-s] \n";
	static String seriousErrorMsg;
	static Config config;
    
	public static void main(String[] args) {
		Config.load();
		Log.init("PacSatGround");
		Config.init();
		invokeGUI();
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
					seriousErrorMsg = "Something is preventing the AMSAT Ground Station from running.  If you recently changed something\n"
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
			Log.errorDialog("SERIOUS EDT ERROR", "Exception on " + tname + "\n" + stacktrace);
		}
	}

	
	
}
