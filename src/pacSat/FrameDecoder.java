package pacSat;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import ax25.Ax25Frame;
import ax25.KissFrame;
import common.Config;
import common.LayoutLoadException;
import common.Log;
import fileStore.MalformedPfhException;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFileFrame;
import pacSat.frames.FTL0Frame;
import pacSat.frames.FrameException;
import pacSat.frames.PacSatFrame;
import pacSat.frames.ResponseFrame;
import pacSat.frames.StatusFrame;


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

	Ax25Frame frame = null;
	
	public void decodeByte(int b) {
		buffer.add(b);
	}
	
	/**
	 * This logic should really be in the State Machines.  It needs to be refactored.  For now Layer 2 and
	 * Layer 3 frames are parsed here and sent to the appropriate State Machine.
	 * @param b
	 * @return
	 */
	private String decodeFrameByte(int b) {
		byteCount++;
		String s = "";
		try {
			if (!kissFrame.add(b)) {
				// frame is full, process it
				frame = new Ax25Frame(kissFrame);
				
				// UI FRAMEs - DISCONNECTED MODE - DOWNLINK SESSION FRAMES
				if (frame.isBroadcastFileFrame()) {
					BroadcastFileFrame bf = new BroadcastFileFrame(frame);
					Config.spacecraft.directory.add(bf);
					if (Config.spacecraft.directory.getTableData().length > 0)
						if (Config.mainWindow != null)
							Config.mainWindow.setDirectoryData(Config.spacecraft.directory.getTableData());
					s = bf.toString();
				} else if (frame.isDirectoryBroadcastFrame()) {
					BroadcastDirFrame bf = new BroadcastDirFrame(frame);
					Config.spacecraft.directory.add(bf);
					if (Config.spacecraft.directory.getTableData().length > 0)
						if (Config.mainWindow != null)
							Config.mainWindow.setDirectoryData(Config.spacecraft.directory.getTableData());
					//s = bf.toString();
				} else if (frame.isStatusFrame()) {
					StatusFrame st = new StatusFrame(frame);
					if (st.frameType == PacSatFrame.PSF_STATUS_BBSTAT) {
						if (Config.uplink != null)
							Config.uplink.processEvent(st);
					} else {
							if (Config.downlink != null)
								Config.downlink.processEvent(st);
					}
					s = st.toString();
				} else if (frame.isResponseFrame()) {
					ResponseFrame st = new ResponseFrame(frame);
					if (Config.downlink != null)
						Config.downlink.processEvent(st);
					s = st.toString();
					
				// NON UI FRAMES - UPLINK SESSION FRAMES - Data Link Frames	
				} else if (frame.isSFrame()) {
					s = "S>>: " + frame.toString();
					if (Config.layer2data != null)
						Config.layer2data.processEvent(frame);
				} else if (frame.isIFrame()) {
					FTL0Frame f = new FTL0Frame(frame);
					s = "I>>" + f.toString();
					if (Config.layer2data != null)
						Config.layer2data.processEvent(frame);
				} else if (frame.isUFrame()) {
					if (Config.layer2data != null)
						Config.layer2data.processEvent(frame);
					s = "U>>: " + frame.toString();
					
				// TELEMETRY	
				} else if (frame.isLstatFrame()) {
					s = "LSTAT: " + frame.toString();
					
				} else // we don't know what it is, just print it out for information
					s = "DK: " + frame.toString();
				
				kissFrame = new KissFrame();
			}
		} catch (FrameException fe) {
			if (frame != null)
				s = s + frame.fromCallsign  + " to " + frame.toCallsign + " ";
			s = "ERROR: " + fe.getMessage();
			kissFrame = new KissFrame();
		} catch (MalformedPfhException e) {
			if (frame != null)
				s = "ERROR: Bad PFH - " + e.getMessage() + " " + frame.toString();
			else
				s = "ERROR: Bad PFH - " + e.getMessage() + " - Empty frame";
			kissFrame = new KissFrame();
		} catch (FileNotFoundException e) {
			if (frame != null)
				s = s + frame.fromCallsign  + " to " + frame.toCallsign + " ";
			s = "ERROR: Opening file " + e.getMessage();
			kissFrame = new KissFrame();
		} catch (IOException e) {
			if (frame != null)
				s = s + frame.fromCallsign  + " to " + frame.toCallsign + " ";
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
//	public static final void main(String[] argc) throws IOException, LayoutLoadException {
//		Config.load();
//		JTextArea ja = null;
//		FrameDecoder dec = new FrameDecoder(ja);
//		Thread background = new Thread(dec);
//		background.start();
//		dec.decode("test_data/fs3_pass_20180606.raw", null);
//		dec.close();
//
//	}
}
