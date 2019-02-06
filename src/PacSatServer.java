import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import common.Config;
import common.Log;
import pacSat.FrameDecoder;
import pacSatServer.TcpTncServer;

public class PacSatServer {
	static String version = "Version 0.4 - 5 Feb 2019";
	static final String usage = "PacSatServer user database [-v]\n-v - Version Information\n";
		
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help")) || (args[0].equalsIgnoreCase("--help"))) {
				System.out.println(usage);
				System.exit(0);
			} else
			if ((args[0].equalsIgnoreCase("-v")) ||args[0].equalsIgnoreCase("-version")) {
				System.out.println("Pacsat Telem Server. Version " + version);
				System.exit(0);
			} else {
				System.out.println(usage);
				System.exit(1);
			}
				
		}
		String u,db;
		if (args.length < 2) {
			System.out.println(usage);
			System.exit(1);
		}
		u = args[0];
		db = args[1];

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String p;	
	    p = in.readLine();
		if (p == null || p.isEmpty()) {
			System.out.println("Missing password");
			System.exit(2);
		}

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
		
		TcpTncServer tncDecoder = new TcpTncServer(port,frameDecoder, null, u, p, db);
		Thread serverThread = new Thread(tncDecoder);
		serverThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		serverThread.setName("Pacsat Server");
		serverThread.start();
	}

}
