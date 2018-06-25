package pacSat;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JTextArea;

import common.Config;
import fileStore.MalformedPfhException;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFileFrame;
import pacSat.frames.FrameException;
import pacSat.frames.KissFrame;
import pacSat.frames.ResponseFrame;
import pacSat.frames.StatusFrame;
import pacSat.frames.UiFrame;


public class FrameDecoder {
	KissFrame kissFrame;
	
	public FrameDecoder() {
		kissFrame = new KissFrame();
		boolean fillingFrame = false;
	}
	public void decode(String file, JTextArea ta) throws IOException {
		FileInputStream byteFile = null;
		try {
		byteFile = new FileInputStream(file);
		
		while (byteFile.available() > 0) {
			int b = byteFile.read();
			String response = decodeByte(b);
			if (response.equalsIgnoreCase(""))
				;
			else
				ta.append(response + "\n");
			try {
				Thread.sleep(1);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		} finally {
			if (byteFile != null) byteFile.close();
		}
	}
	
	UiFrame ui = null;
	
	public String decodeByte(int b) {
		
		String s = "";
		try {
			if (!kissFrame.add(b)) {
				// frame is full, process it
				ui = new UiFrame(kissFrame);
				if (ui.isBroadcastFileFrame()) {
					BroadcastFileFrame bf = new BroadcastFileFrame(ui);
					Config.directory.add(bf);
					if (Config.directory.getTableData().length > 0)
						Config.mainWindow.setDirectoryData(Config.directory.getTableData());
					s = bf.toString();
				} else if (ui.isDirectoryBroadcastFrame()) {
					BroadcastDirFrame bf = new BroadcastDirFrame(ui);
					Config.directory.add(bf.pfh);
					if (Config.directory.getTableData().length > 0)
						Config.mainWindow.setDirectoryData(Config.directory.getTableData());
					//s = bf.toString();
				} else if (ui.isStatusFrame()) {
					StatusFrame st = new StatusFrame(ui);
					Config.downlink.processEvent(st);
					s = st.toString();
				} else if (ui.isResponseFrame()) {
					ResponseFrame st = new ResponseFrame(ui);
					Config.downlink.processEvent(st);
					s = st.toString();
				} else
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
				s = s + ui.fromCallsign  + " to " + ui.toCallsign + " ";
			s = "ERROR: Bad PFH - " + e.getMessage();
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

	// Test routine
//	public static final void main(String[] argc) throws IOException {
//		FrameDecoder dec = new FrameDecoder();
//		dec.decode("fs3_pass_20180606.raw", null);
//	}
}
