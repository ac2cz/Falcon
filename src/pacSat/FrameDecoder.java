package pacSat;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JTextArea;

import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.server.STP;

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
import pacSat.frames.TlmFrame;


public class FrameDecoder implements Runnable {
	KissFrame kissFrame;
	ConcurrentLinkedQueue<Integer> buffer = new ConcurrentLinkedQueue<Integer>();
	JTextArea log;
	boolean running = true;
	int byteCount = 0;
	int byteCountAtFrameStart = 0;  // This stores the byte count at the end of a frame
	int byteRead = 0;
	int seq = 0;
	
	public FrameDecoder(JTextArea ta) {
		kissFrame = new KissFrame();
		log = ta;
	}
	
	public void decode(String data) {
		int p=0;
		decodeByte(0xc0);
		while (p < data.length()) {
			String s = ""+data.charAt(p) + data.charAt(p+1);
			int b = Integer.valueOf(s,16);
			byteRead++;
			decodeByte(b);
			p +=2;
		} 
		decodeByte(0xc0);
		try { Thread.sleep(0,1); } catch (InterruptedException e) { }
			
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
		boolean broadcastBytes = false;
		String s = "";
		try {
			if (!kissFrame.add(b)) {
				boolean echoFrame = false;
				// frame is full, process it
				frame = new Ax25Frame(kissFrame);
				
				// UI FRAMEs - DISCONNECTED MODE - DOWNLINK SESSION FRAMES
				if (frame.isBroadcastFileFrame()) {
					broadcastBytes = true;
					BroadcastFileFrame bf = new BroadcastFileFrame(frame);
					boolean updated = false;
					try {
						updated = Config.spacecraftSettings.directory.add(bf);
					} catch (NumberFormatException e) {
						s = "ERROR: Number Format issue with telemetry " + e.getMessage();
					} catch (com.g0kla.telem.data.LayoutLoadException e) {
						s = "ERROR: Opening Layout " + e.getMessage();
					} catch (DataLoadException e) {
						s = "ERROR: Loading Data " + e.getMessage();
					}
					if (updated) {
						String[][] data = Config.spacecraftSettings.directory.getTableData();
						if (data.length > 0)
							if (Config.mainWindow != null)
								Config.mainWindow.setDirectoryData(data);
					}
					s = bf.toString();
					echoFrame = true;
				} else if (frame.isDirectoryBroadcastFrame()) {
					broadcastBytes = true;
					BroadcastDirFrame bf = new BroadcastDirFrame(frame);
					if (Config.getBoolean(Config.DEBUG_DOWNLINK))
						s = bf.toString();
					boolean updated = Config.spacecraftSettings.directory.add(bf);
					if (updated) {
						String[][] data = Config.spacecraftSettings.directory.getTableData();
						if (data.length > 0)
							if (Config.mainWindow != null)
								Config.mainWindow.setDirectoryData(data);
					}
					s = bf.toString();
					echoFrame = true;
				} else if (frame.isStatusFrame()) {
					StatusFrame st = new StatusFrame(frame);
					if (st.frameType == PacSatFrame.PSF_STATUS_BBSTAT) {
						if (Config.uplink != null)
							Config.uplink.processEvent(st);
					} else {
						if (st.frameType == PacSatFrame.PSF_STATUS_BYTES) {
							st.bytesReceivedOnGround = byteCountAtFrameStart;  // use the count from just before this frame
							byteCountAtFrameStart=0;
						}
						if (Config.downlink != null)
							Config.downlink.processEvent(st);
					}
					s = st.toString();
				} else if (frame.isResponseFrame()) {
					ResponseFrame st = new ResponseFrame(frame);
					if (Config.downlink != null)
						Config.downlink.processEvent(st);
					s = st.toString();
				} else if (frame.isTlmFrame()) {
					TlmFrame st = null;
					try {
						st = new TlmFrame(frame);
						Config.db.add(st.record);
						s = st.toString();
					} catch (com.g0kla.telem.data.LayoutLoadException e) {
						s = "ERROR: Opening Layout " + e.getMessage();
					} catch (NumberFormatException e) {
						s = "ERROR: Number parse " + e.getMessage();
					} catch (DataLoadException e) {
						s = "ERROR: Loading data " + e.getMessage();
					}						
					echoFrame = true;
				// NON UI FRAMES - UPLINK SESSION FRAMES - Data Link Frames	
				} else if (frame.isSFrame()) {
					s = "S>> " + frame.toString();
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
					s = "U>> " + frame.toString();
					
				// TELEMETRY	
				} else if (frame.isLstatFrame()) {
					s = "LSTAT: " + frame.toString();
					echoFrame = true;
				} else if (frame.isTimeFrame()) {
					s = "TIME-1: " + frame.toString();
					echoFrame = true;
				} else { // we don't know what it is, just print it out for information and forward to server as likely telem
					// TLMS, BCR, TLMC
					s = "" + frame.toString();
					echoFrame = true;
				}
				if (Config.getBoolean(Config.SEND_TO_SERVER) && echoFrame) {
					// add to the queue to be sent to the server
					long seq = Config.sequence.getNextSequence();
					STP stp = new STP(Config.spacecraft.satId, Config.get(Config.CALLSIGN), Config.get(Config.LATITUDE), 
							Config.get(Config.LONGITUDE), Config.get(Config.ALTITUDE), Config.get(Config.STATION_DETAILS), 
							"PacsatGround V" + Config.VERSION, Config.spacecraftSettings.SOURCE, seq, kissFrame);
					Config.stpQueue.add(stp);
					Config.totalFrames++;
//					Config.mainWindow.setFrames(Config.totalFrames); // need timestamps to be to the millisecond for this to work
				}
				if (broadcastBytes && frame != null && frame.getDataBytes() != null)
					byteCountAtFrameStart += frame.getDataBytes().length;
				kissFrame = new KissFrame();
			}
		} catch (FrameException fe) {
			if (frame != null && Config.getBoolean(Config.DEBUG_LAYER2)) {
				s = s + frame.fromCallsign  + " to " + frame.toCallsign + " ";
				s = "ERROR: " + fe.getMessage();
			}
			kissFrame = new KissFrame();
		} catch (MalformedPfhException e) {
			if (frame != null && Config.getBoolean(Config.DEBUG_LAYER3)) {
				s = "ERROR: Bad PFH - " + e.getMessage() + ": " + frame.toString();
			}
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
		
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(10);
		ses.scheduleAtFixedRate(new Runnable() {
		    @Override
		    public void run() {
		        Config.spacecraftSettings.directory.saveIfNeeded();
		    }
		}, 0, 60, TimeUnit.SECONDS);  // execute every 60 seconds
		
		
		while (running) {
			if (buffer.size() > 0) {
				int i = buffer.poll();
				String response = decodeFrameByte(i);
				if (response != "") {
					if (log == null)
						System.out.println(response  + "\n"); // this is for test sat
					else
						log.append(response + "\n");
					Log.println(response);
				}
				if (Config.mainWindow != null)
					Config.mainWindow.setDCD(true);
			} else {
				try { Thread.sleep(0,1); } catch (InterruptedException e) { }
				if (Config.mainWindow != null)
					Config.mainWindow.setDCD(false);
			}
		}
		Log.println("EXIT Frame Decoder Thread");
		System.out.println("Read " + byteRead + " bytes");
		System.out.println("Decoded " + byteCount + " bytes");
	}

	// Test routine
	public static final void main(String[] argc) throws IOException, LayoutLoadException {
		Config.init("test");
		JTextArea ja = null;
//		FrameDecoder dec = new FrameDecoder(ja);
//		Thread background = new Thread(dec);
//		background.start();
//		dec.decode("A8989A64404002A08CA66640400303F07E2F5F5C4884094950064AB60A4BB2094CB7084DE7014EF4004F1F005000005100005200005300005400005500005600005700005800005900005A00005B00005C00005D00005E00005F00006000006100006200006300006400006500006600006700006800006900006A00006B00006C00006D00006E00006F000070000071000072F20773FE0774190475B90476A304779F04788204797F047A85047B8F047C47047D7E047EB6047FAD04800000810000820000830000840000850000860000");
		String data = "A8989A64404002A08CA66640400303F07E2F5F5C4884094950064AB60A4BB2094CB7084DE7014EF4004F1F005000005100005200005300005400005500005600005700005800005900005A00005B00005C00005D00005E00005F00006000006100006200006300006400006500006600006700006800006900006A00006B00006C00006D00006E00006F000070000071000072F20773FE0774190475B90476A304779F04788204797F047A85047B8F047C47047D7E047EB6047FAD04800000810000820000830000840000850000860000";
		int p = 0;
		String value = "";
//		int[] kissFrame = new int[data.length()/2];
		while (p*2 < data.length()) {
			String s = ""+data.charAt(p) + data.charAt(p+1);
			int b = Integer.valueOf(s,16);
			value = value + (char)b;
			p +=2;
		}
		
		System.out.println(value);
		
		//		dec.decode("test_data/fs3_pass_20180606.raw", null);
//		dec.close();

	}
}
