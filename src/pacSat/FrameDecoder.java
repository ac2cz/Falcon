package pacSat;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JTextArea;

import common.Config;

import fileStore.MalformedPfhException;
import gui.MainWindow;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFrame;
import pacSat.frames.FrameException;
import pacSat.frames.KissFrame;
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
		}
		} finally {
			if (byteFile != null) byteFile.close();
		}
	}
	public String decodeByte(int b) {
		String s = "";
		try {
			if (!kissFrame.add(b)) {
				// frame is full, process it
				//System.out.println(uiFrame);
				//if (kissFrame.isDataFrame()) {
				UiFrame ui = new UiFrame(kissFrame);
				if (ui.isBroadcastFrame()) {
					BroadcastFrame bf = new BroadcastFrame(ui);
					s = bf.toString();
				} else if (ui.isDirectoryBroadcastFrame()) {
					BroadcastDirFrame bf = new BroadcastDirFrame(ui);
					Config.directory.add(bf.pfh);
					Config.mainWindow.setDirectoryData(Config.directory.getTableData());
					s = bf.toString();
				} else
					s = ui.toString();
				//}
				kissFrame = new KissFrame();
			}
		} catch (FrameException fe) {
			s = "ERROR: " + fe.getMessage();
			kissFrame = new KissFrame();
		} catch (MalformedPfhException e) {
			s = "ERROR: Bad PFH" + e.getMessage();
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
