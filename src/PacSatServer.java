import java.io.File;
import java.io.IOException;

import common.Config;
import common.Log;
import pacSat.FrameDecoder;
import pacSat.TcpTncServer;

public class PacSatServer {
	static String version = "Version 0.2";
	
	public static void main(String[] args) {
		
		Config.init("PacSatServer.properties");
		File current = new File(System.getProperty("user.dir"));
		Config.currentDir = current.getAbsolutePath();
		Config.set(Config.LOGFILE_DIR, Config.currentDir);
		Config.homeDir = Config.currentDir;
		
		Config.load();
		
		Log.init(Config.get(Config.LOGFILE_DIR) + File.separator + "PacSatServer");
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
		

		int port = Config.getInt(Config.TELEM_SERVER_PORT);

		Log.println("Starting PacSatServer "+PacSatServer.version +" on port: "+port+"...");
		
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
