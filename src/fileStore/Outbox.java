package fileStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import ax25.Ax25Request;
import common.Config;
import common.Log;
import common.Spacecraft;
import gui.FileHeaderTableModel;

public class Outbox {

	String dirFolder;

	public Outbox(String satname) {
		dirFolder = Config.get(Config.LOGFILE_DIR) + satname;
	}

	/**
	 * The Outbox is all files with the ending .OUT that have a valid PFH in the
	 * directory
	 * @return
	 */
	public String[][] getTableData() {
		ArrayList<PacSatFileHeader> files = new ArrayList<PacSatFileHeader>();
		ArrayList<String> filenames = new ArrayList<String>();

		File folder = new File(dirFolder);
		File[] targetFiles = folder.listFiles();
		Arrays.sort(targetFiles); // this makes it alphabetical, but not by numeric order of the last 2 digits
		boolean found = false;
		if (targetFiles != null ) { 
			for (int i = 0; i < targetFiles.length; i++) {
				if (targetFiles[i].isFile() && (
						targetFiles[i].getName().endsWith(".out")) 
						|| targetFiles[i].getName().endsWith(".ul")) {
					PacSatFile psf = null;
					try {
						psf = new PacSatFile(targetFiles[i].getPath());
					} catch (MalformedPfhException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (psf != null) {
						files.add(psf.pfh);
						filenames.add(targetFiles[i].getName());
					}
				}
			}
		}
		if (files.size() > 0) {
			String[][] data = new String[files.size()][FileHeaderTableModel.MAX_TABLE_FIELDS];
			int i=0;
			// Put most recent at the top, which is opposite order
			for (PacSatFileHeader pfh : files) {
				//if (pfh.getFileId() == 0x347) // debug one file
				//	Log.println("STOP");
				PacSatFile psf = new PacSatFile(dirFolder, pfh.getFileId());
				data[files.size() -1 - i++] = pfh.getTableFields();
				long fileSize = pfh.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
//				long holesLength = psf.getHolesSize();
//				float percent = 1.0f;
//				if (holesLength > 0 && holesLength <= fileSize)
//					percent = holesLength/(float)fileSize;
//				else if (pfh.state == 0) // no state
//					percent = 0;
//				String p = String.format("%2.0f", percent*100) ;
//				data[files.size() - i][FileHeaderTableModel.HOLES] = "" + " " + psf.getNumOfHoles() + "/" + p + "%";
				
				// TODO - this is a kludge.  The userfilename should be the actual name of the file.  This is inherited from WISP
				// which had only 8.3 filenames
				String filename = filenames.get(i-1);
				if (filename.endsWith(".ul"))
					data[files.size() - i][FileHeaderTableModel.STATE] = PacSatFileHeader.states[PacSatFileHeader.PARTIAL];
				else
					data[files.size() - i][FileHeaderTableModel.STATE] = PacSatFileHeader.states[PacSatFileHeader.MSG];

				data[files.size() - i][FileHeaderTableModel.FILENAME] = filename;
			}
			return data;
		}
		return null;
	}
}
