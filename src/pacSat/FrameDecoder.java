package pacSat;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import common.Config;
import common.LayoutLoadException;
import common.Log;
import fileStore.MalformedPfhException;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFileFrame;
import pacSat.frames.FTL0Frame;
import pacSat.frames.FrameException;
import pacSat.frames.KissFrame;
import pacSat.frames.PacSatFrame;
import pacSat.frames.ResponseFrame;
import pacSat.frames.StatusFrame;
import pacSat.frames.UiFrame;


public class FrameDecoder implements Runnable {
	KissFrame kissFrame;
	ConcurrentLinkedQueue<Integer> buffer = new ConcurrentLinkedQueue<Integer>();
	JTextArea log;
	boolean running = true;
	int byteCount = 0;
	int byteRead = 0;
	
	public FrameDecoder(JTextArea ta) {
		kissFrame = new KissFrame();
		log = ta;
	}
	
	public void decode(String file, JTextArea ta) throws IOException {
		log = ta;
		FileInputStream byteFile = null;
		try {
			byteFile = new FileInputStream(file);
			while (byteFile.available() > 0) {
				if (buffer.size() < Integer.MAX_VALUE) {
					int b = byteFile.read();
					byteRead++;
					decodeByte(b);
				} 
				try { Thread.sleep(0,1); } catch (InterruptedException e) { }
			}
		} finally {
			if (byteFile != null) byteFile.close();
		}
	}

	UiFrame ui = null;
	
	public void decodeByte(int b) {
		buffer.add(b);
	}
	
	
	private String decodeFrameByte(int b) {
		byteCount++;
		String s = "";
		try {
			if (!kissFrame.add(b)) {
				// frame is full, process it
				ui = new UiFrame(kissFrame);
				
				// DOWNLINK SESSION FRAMES
				if (ui.isBroadcastFileFrame()) {
					BroadcastFileFrame bf = new BroadcastFileFrame(ui);
					Config.spacecraft.directory.add(bf);
					if (Config.spacecraft.directory.getTableData().length > 0)
						if (Config.mainWindow != null)
							Config.mainWindow.setDirectoryData(Config.spacecraft.directory.getTableData());
					s = bf.toString();
				} else if (ui.isDirectoryBroadcastFrame()) {
					BroadcastDirFrame bf = new BroadcastDirFrame(ui);
					Config.spacecraft.directory.add(bf.pfh);
					if (Config.spacecraft.directory.getTableData().length > 0)
						if (Config.mainWindow != null)
							Config.mainWindow.setDirectoryData(Config.spacecraft.directory.getTableData());
					//s = bf.toString();
				} else if (ui.isStatusFrame()) {
					StatusFrame st = new StatusFrame(ui);
					if (Config.downlink != null)
						Config.downlink.processEvent(st);
					s = st.toString();
				} else if (ui.isResponseFrame()) {
					ResponseFrame st = new ResponseFrame(ui);
					if (Config.downlink != null)
						Config.downlink.processEvent(st);
					s = st.toString();
					
				// UPLINK SESSION FRAMES	
				} else if (ui.isSFrame()) {
					s = "UPLINK SESSION: " + ui.toString();
				} else if (ui.isIFrame()) {
					FTL0Frame ftl = new FTL0Frame(ui);
					s = "UPLINK INFO: " + ftl.toString();
				} else if (ui.isUFrame()) {
					s = "UPLINK U RESP: " + ui.toString();
				} else // we don't know what it is, just print it out for information
					s = ui.toString();
				
				kissFrame = new KissFrame();
			}
		} catch (FrameException fe) {
			if (ui != null)
				s = s + ui.fromCallsign  + " to " + ui.toCallsign + " ";
			s = "ERROR: " + fe.getMessage();
			kissFrame = new KissFrame();
		} catch (MalformedPfhException e) {
			if (ui != null)
				s = "ERROR: Bad PFH - " + e.getMessage() + " " + ui.toString();
			else
				s = "ERROR: Bad PFH - " + e.getMessage() + " - Empty UI";
			kissFrame = new KissFrame();
		} catch (FileNotFoundException e) {
			if (ui != null)
				s = s + ui.fromCallsign  + " to " + ui.toCallsign + " ";
			s = "ERROR: Opening file " + e.getMessage();
			kissFrame = new KissFrame();
		} catch (IOException e) {
			if (ui != null)
				s = s + ui.fromCallsign  + " to " + ui.toCallsign + " ";
			s = "ERROR: Writing received file chunk" + e.getMessage();
			kissFrame = new KissFrame();
		}
		return s;
	}
	
	public void close() {
		running = false;
	}

	@Override
	public void run() {

		Log.println("START Frame Decoder Thread");
		while (running) {
			if (buffer.size() > 0) {
				int i = buffer.poll();
				String response = decodeFrameByte(i);
				if (response != "") {
					if (log == null)
						System.out.println(response  + "\n");
					else
						log.append(response + "\n");
				}
			} else
				try { Thread.sleep(0,1); } catch (InterruptedException e) { }
		}
		Log.println("EXIT Frame Decoder Thread");
		System.out.println("Read " + byteRead + " bytes");
		System.out.println("Decoded " + byteCount + " bytes");
	}

	// Test routine
	public static final void main(String[] argc) throws IOException, LayoutLoadException {
		Config.load();
		JTextArea ja = null;
		FrameDecoder dec = new FrameDecoder(ja);
		Thread background = new Thread(dec);
		background.start();
		dec.decode("test_data/fs3_pass_20180606.raw", null);
		dec.close();

	}
}
