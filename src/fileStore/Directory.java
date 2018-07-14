package fileStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

import pacSat.frames.BroadcastFileFrame;
import pacSat.frames.KissFrame;
import common.Config;
import common.Log;
import gui.FileHeaderTableModel;

public class Directory  {
	SortedArrayList<PacSatFileHeader> files;
	public static final String DIR_FILE_NAME = "directory.db";
	public String dirFolder = "directory";
	boolean needDir = true;
	Date lastChecked = null;
	public static final int DIR_CHECK_INTERVAL = 60; // mins between directory checks;
	SortedArrayList<DirHole> holes;
	
	public Directory(String satname) {
		dirFolder = Config.get(Config.LOGFILE_DIR) + satname;
		files = new SortedArrayList<PacSatFileHeader>();
		File dir = new File(dirFolder);
		if (!dir.exists()) {
			// new to try to make the dir
			makeDir(dirFolder);
		}
		try {
			load();
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
	
	public SortedArrayList<DirHole> getHolesList() {
		if (files.size() < 1) return null;
		refreshHolesList();
		return holes;
	}
	
	private void refreshHolesList() {
		holes = new SortedArrayList<DirHole>();
		
		PacSatFileHeader prevPfh = null;
		for (int i=files.size()-1; i>=0; i--) {
			PacSatFileHeader pfh = files.get(i);
			long id = pfh.getFileId();
			if (prevPfh == null) {
				// Add the first hole to get new files
				int[] toBy = {0xff,0xff,0xff,0x7f}; // end of time, well 2038.. This is the max date for a 32 bit in Unix Timestamp
				long to = KissFrame.getLongFromBytes(toBy);
				Date toDate = new Date(to*1000);
				Date fromDate = getLastHeaderDate(); // just in case the latest does not have a date, we search for the most recent
				DirHole hole = new DirHole(fromDate,toDate);
				holes.add(hole);
			} else {
				long prevId = prevPfh.getFileId();
				if (id != prevId - 1) {
					// we have a hole
					//Log.println("ADD HOLE: " + Long.toHexString(id) + " " + Long.toHexString(prevId));
					DirHole hole = new DirHole(pfh.getUploadTime(),prevPfh.getUploadTime());
					holes.add(hole);
				}
			}
			prevPfh = pfh;
		}
		if (prevPfh != null) {
			// Add a final hole for historical files we have missed
			Date historical = new Date(1);
			DirHole hole = new DirHole(historical,prevPfh.getUploadTime());
			holes.add(hole);
		}
	}
	
	/**
	 * Decide if we need a directory.  
	 * How fresh is our data?  
	 *     If we have no dir headers for 60 minutes then assume this is a new pass and ask for headers
	 *     
	 * Do we have holes?  Is this the dates where we have gaps between the files?  Should the File IDs be contiguous, at least 
	 * for the headers?  We dont have to download them all.
	 *     
	 * 
	 * @return
	 */
	public boolean needDir() {
		if (lastChecked == null) {
			lastChecked = new Date();
			return true;
		}
		// We get the timestamp of the last time we checked
		// If it is some time ago then we ask for another directory
		Date timeNow = new Date();
		long minsNow = timeNow.getTime() / 60000;
		long minsLatest = lastChecked.getTime() / 60000;
		long diff = minsNow - minsLatest;
		if (diff > DIR_CHECK_INTERVAL)
			return true;
		lastChecked = new Date();
		return false;
	}
	
	public Date getLastHeaderDate() {
		if (files.size() < 1) return new Date(1); // return 1970 as we have not files, so we want them all
		Date lastDate = null;
		for (int i=files.size()-1; i>=0; i--) {
			lastDate = files.get(files.size()-1).getUploadTime();
			
			if (lastDate != null) {
				break;
			}
		}
		if (lastDate == null)
			return new Date(1);
		return lastDate;
	}
	
	/**
	 * We search the directory for the most urgent file.  This is the file with the highest priority and that is the most recent
	 * This means we search forward and store the file with the highest priority, only replacing it if there is another
	 * file with a higher priority
	 * @return 0 if no file to download, otherwise the file ID
	 */
	public long needFile() {
		if (files.size() < 1) return 0; // we have no files, so we can't request any
		PacSatFileHeader fileToDownload = null;
		for (PacSatFileHeader pfh : files) {
			if (pfh.userDownLoadPriority > 0) // then this has a priority, 0 means ignore
				if (fileToDownload == null)
					fileToDownload = pfh;
				else if (pfh.userDownLoadPriority < fileToDownload.userDownLoadPriority)
					fileToDownload = pfh; // then this is higher priority, store this one instead
		}
		if (fileToDownload == null) 
			return 0;
		else 
			return fileToDownload.getFileId();
	}
	
	public PacSatFileHeader getPfhById(long id) {
		for (PacSatFileHeader pfh : files)
			if (pfh.getFileId() == id)
				return pfh;
		return null;
	}
		
	public boolean add(PacSatFileHeader pfh) throws IOException {
		if (files.add(pfh)) {
			save();
			return true;
		}
		return false;
	}
	
	public boolean add(BroadcastFileFrame bf) throws IOException, MalformedPfhException {
		PacSatFile psf = new PacSatFile(dirFolder, bf.fileId);
		PacSatFileHeader pfh;
		if (bf.offset == 0) {
			// extract the header, this is the first chunk
			pfh = new PacSatFileHeader(bf.data);
			add(pfh);
		}
		pfh = getPfhById(psf.getFileId());
	
		if (psf.addFrame(bf))
			if (pfh.state != PacSatFileHeader.MSG)
				pfh.setState(PacSatFileHeader.NEWMSG);
		else if (pfh != null) {
			// we have the header and some data
			pfh.setState(PacSatFileHeader.PARTIAL);
		}
		return true;
	}
	
	public String[][] getTableData() {
		String[][] data = new String[files.size()][FileHeaderTableModel.MAX_TABLE_FIELDS];
		int i=0;
		// Put most recent at the top, which is opposite order
		for (PacSatFileHeader pfh : files) {
			PacSatFile psf = new PacSatFile(dirFolder, pfh.getFileId());
			data[files.size() -1 - i++] = pfh.getTableFields();
			data[files.size() - i][7] = "" + " " + psf.getNumOfHoles(); // +psf.getActualLength()
		}
		return data;

	}
	
	public void save() throws IOException {
		FileOutputStream fileOut = null;
		ObjectOutputStream objectOut = null;
		try {
			fileOut = new FileOutputStream(dirFolder + File.separator + DIR_FILE_NAME);
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(files);
			//Log.println("Saved directory to disk");
		} finally {
			if (objectOut != null) try { objectOut.close(); } catch (Exception e) {};
			if (fileOut != null) try { fileOut.close(); } catch (Exception e) {};
		}
	}
	
	public void load() throws IOException, ClassNotFoundException {
		ObjectInputStream objectIn = null;
		FileInputStream streamIn = null;
		try {
			streamIn = new FileInputStream(dirFolder + File.separator + DIR_FILE_NAME);
			objectIn = new ObjectInputStream(streamIn);

			files = (SortedArrayList<PacSatFileHeader>) objectIn.readObject();

		} finally {
			if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
			if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
		}
	}
	
	/**
	 * Make the database directory if needed.  Check to see if we have existing legacy data and run the conversion if we do
	 * @param dir
	 */
	private boolean makeDir(String dir) {
		
		File aFile = new File(dir);
		if(!aFile.isDirectory()){
			Log.println("Making new database: " + dir);
			aFile.mkdir();
			if(!aFile.isDirectory()){
				Log.errorDialog("ERROR", "ERROR can't create the directory.  Check the path is writable:\n " + aFile.getAbsolutePath() + "\n");
				System.exit(1);
				return false;
			}
			return true;
		}
		
		return false;
	}
}
