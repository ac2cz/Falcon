package fileStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import ax25.Ax25Request;
import common.Config;
import common.Log;
import common.SpacecraftSettings;
import gui.FileHeaderTableModel;

public class Outbox {

	String dirFolder;

	public Outbox(String satname) {
		dirFolder = Config.get(Config.LOGFILE_DIR) + File.separator + satname;
	}

	/**
	 * We want the oldest file.  The one we modified last.  That should be the next one to send
	 * @return
	 */
	public File getNextFile(){
	    File dir = new File(dirFolder);
	    File[] files = dir.listFiles();
	    if (files == null || files.length == 0) {
	        return null;
	    }

	    File lastModifiedFile = files[0];
	    for (int i = 1; i < files.length; i++) {
	    	if (isOutFile(files[i])) {
	    		if (!isOutFile(lastModifiedFile))
	    			lastModifiedFile = files[i];
	    		else if (lastModifiedFile.lastModified() > files[i].lastModified()) {
	    			lastModifiedFile = files[i];
	    		}
	    	}
	    }
	    if (!isOutFile(lastModifiedFile))
	    	return null;
	    return lastModifiedFile;
	}
	
	boolean isOutFile(File file) {
		if (file.isFile() &&
    			file.getName().endsWith(".out"))
			return true;
		return false;
	}

	boolean isOutboxFile(File file) {
		if (file.isFile() && (
    			file.getName().endsWith(".out")) 
    			|| file.getName().endsWith(".err")
    			|| file.getName().endsWith(".ul"))
			return true;
		return false;
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
		Arrays.sort(targetFiles, new Comparator<File>(){
	    public int compare(File f1, File f2)
	    {
	        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
	    } });
		boolean found = false;
		if (targetFiles != null ) { 
			for (int i = 0; i < targetFiles.length; i++) {
				if (isOutboxFile(targetFiles[i])) {
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
					data[files.size() - i][FileHeaderTableModel.STATE] = PacSatFileHeader.states[PacSatFileHeader.SENT];
				else if (filename.endsWith(".err"))
					data[files.size() - i][FileHeaderTableModel.STATE] = PacSatFileHeader.states[PacSatFileHeader.REJ];
				else
					data[files.size() - i][FileHeaderTableModel.STATE] = PacSatFileHeader.states[PacSatFileHeader.QUE];

				data[files.size() - i][FileHeaderTableModel.FILENAME] = filename;
			}
			return data;
		}
		return null;
	}
	
	public void delete(File f) throws IOException {
		Directory.remove(f.getPath());
	}
	
	public static void main(String[] args) {
		Config.load();
		Log.init("PacSatGround");
		Config.init("PacSatGround.properties");
		Outbox box = new Outbox("FalconSat-3");
		File s = box.getNextFile();
		System.out.println(s.getName());
		Config.close();
	}
}
