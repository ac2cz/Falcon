import java.io.File;
import java.io.IOException;

import javax.swing.JTextArea;

import common.Config;
import common.Log;
import pacSat.FrameDecoder;
import pacSat.TcpTncDecoder;
import pacSat.TcpTncServer;

public class PacSatServer {

	static String logFileDir = null;
	
	public static void main(String[] args) {
		if (args != null && args[0] != null)
			logFileDir = args[0];
		Config.init("PacSatServer.properties");
		Config.currentDir = System.getProperty("user.dir");
		if (logFileDir == null)
			Config.homeDir = System.getProperty("user.dir");
		else {
			Config.homeDir = logFileDir;
			
		}
		
		Config.load();
		if (logFileDir != null)
			Config.set(Config.LOGFILE_DIR, logFileDir); // make sure this is set
		
		Log.init(logFileDir + File.separator + "PacSatServer");
		Log.showGuiDialogs = false;
		try {
			Config.simpleStart();
		} catch (com.g0kla.telem.data.LayoutLoadException e) {
			Log.println("ERROR - Could not load the layout, telem DB has not been initiailized.  No telemetry can be stored.\n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.println("ERROR - Could not load the layout, telem DB has not been initiailized.  No telemetry can be stored.\n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
		

		String hostname = Config.get(Config.TNC_TCP_HOSTNAME);
		if (hostname == null) return;
		int port = Config.getInt(Config.TNC_TCP_PORT);
		if (hostname == null) return;

		Log.println("Starting PacSatServer on port: "+port+"...");
		
		FrameDecoder frameDecoder = new FrameDecoder(null);
		Thread frameDecoderThread = new Thread(frameDecoder);
		frameDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		frameDecoderThread.setName("Frame Decoder");
		frameDecoderThread.start();
		
		TcpTncServer tncDecoder = new TcpTncServer(port,frameDecoder, null);
		Thread serverThread = new Thread(tncDecoder);
		serverThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		serverThread.setName("Pacsat Server");
		serverThread.start();
	}

}
