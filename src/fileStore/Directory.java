package fileStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFileFrame;
import pacSat.frames.KissFrame;
import common.Config;
import common.Log;
import gui.FileHeaderTableModel;

public class Directory  {
	SortedArrayList<PacSatFileHeader> files;
	public static final String DIR_FILE_NAME = "directory.db";
	public String dirFolder = "directory";
	
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
		try {
			loadHoleList();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			
		}
	}
	
	public boolean hasHoles() {
		if (files.size() < 1) return false;
		if (holes.size() < 3) return false; // we always have 2 holes at the start and end for new files
		return true;
	}
	
	public SortedArrayList<DirHole> getHolesList() {
		if (files.size() < 1) return null;
		return holes;
	}
	
	public String getHolFileName() {
		return dirFolder + File.separator + DIR_FILE_NAME + ".hol";
	}
	
	/**
	 * This is based on the algorithm for IP Packet re-assembly RFC 815 here:
	 * https://tools.ietf.org/html/rfc815
	 * 
	 * @param fragment
	 */
	private void updateHoles(BroadcastDirFrame fragment) {
		PacSatFileHeader pfh = fragment.pfh;
		if (pfh != null && ( pfh.state == PacSatFileHeader.NEWMSG || pfh.state == PacSatFileHeader.MSG)) return; // we already have this
		if (holes == null) {
			holes = new SortedArrayList<DirHole>();
			Date frmDate, toDate;
			int[] toBy = {0xff,0xff,0xff,0x7f}; // end of time, well 2038.. This is the max date for a 32 bit in Unix Timestamp
			long to = KissFrame.getLongFromBytes(toBy);
			toDate = new Date(to*1000);
			frmDate = new Date(0); // the begining of time
			DirHole hole = new DirHole(frmDate,toDate);
			holes.add(hole); // initialize with one hole that is maximum length 
		}
		Log.println("FRAG: " + Long.toHexString(fragment.fileId) + " " + PacSatField.getDateString(new Date(fragment.getFirst()*1000)) + " - " + PacSatField.getDateString(new Date(fragment.getLast()*1000))+ "  ->  ");
		
		SortedArrayList<DirHole> newHoles = new SortedArrayList<DirHole>();
		for (Iterator<DirHole> iterator = holes.iterator(); iterator.hasNext();) {
			DirHole hole = iterator.next();
			if (fragment.getFirst() > hole.getLast()) {
				newHoles.add(hole);
				continue;
			}
			if (fragment.getLast() < hole.getFirst()) {
				newHoles.add(hole);
				continue;
			}
			// Otherwise the current hole is no longer valid we will remove it once we work out how it should be replaced
			if (fragment.getFirst() > hole.getFirst()) {
				DirHole newHole = new DirHole(hole.getFirst(),fragment.getFirst() - 1);
				newHoles.add(newHole);
				Log.println("MADE HOLE1: " + newHole);
			}
//			if (fragment.getLast() < hole.getLast() && fragment.hasLastByteOfFile()) 
//				continue; // we can discard the last hole, we have the last bytes in the file
//			else {
			if (fragment.getLast() < hole.getLast() /*&& !fragment.hasLastByteOfFile()*/) {  // last byte of file is always set so we cant use that check
				DirHole newHole = new DirHole(fragment.getLast() +1, hole.getLast());
				newHoles.add(newHole);
				Log.println("MADE HOLE2: " + newHole);
			}		
		}
		holes = newHoles;
		try {
			saveHoleList();
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not write hole file to disk: " + this.getHolFileName() +"\n" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void saveHoleList() throws IOException {
		FileOutputStream fileOut = null;
		ObjectOutputStream objectOut = null;
		try {
			fileOut = new FileOutputStream(getHolFileName());
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(holes);
			//Log.println(filename + ": Saved holes to disk");
		} finally {
			if (objectOut != null) try { objectOut.close(); } catch (Exception e) {};
			if (fileOut != null) try { fileOut.close(); } catch (Exception e) {};
		}
	}
	
	public void loadHoleList() throws IOException, ClassNotFoundException {
		ObjectInputStream objectIn = null;
		FileInputStream streamIn = null;
		try {
			streamIn = new FileInputStream(getHolFileName());
			objectIn = new ObjectInputStream(streamIn);

			holes = (SortedArrayList<DirHole>) objectIn.readObject();

		} finally {
			if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
			if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
		}
	}
	
	/**
	 * Rebuild the holes list for the directory
	 */
	private void DEPECIATEDrefreshHolesList() {
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
					Log.println(" DIR HOLE: " + Long.toHexString(id) + " " + Long.toHexString(prevId));
					DirHole hole = new DirHole(pfh.getDate(PacSatFileHeader.UPLOAD_TIME),prevPfh.getDate(PacSatFileHeader.UPLOAD_TIME));
					holes.add(hole);
				}
			}
			prevPfh = pfh;
		}
		// Leave this to first DIR of pass only?
//		if (prevPfh != null) {
//			// Add a final hole for historical files we have missed
//			Date historical = new Date(1);
//			DirHole hole = new DirHole(historical,prevPfh.getUploadTime());
//			holes.add(hole);
//		}
	}
	

	
	public Date getLastHeaderDate() {
		if (files.size() < 1) return new Date(1); // return 1970 as we have not files, so we want them all
		Date lastDate = null;
		for (int i=files.size()-1; i>=0; i--) {
			lastDate = files.get(files.size()-1).getDate(PacSatFileHeader.UPLOAD_TIME);
			
			if (lastDate != null) {
				break;
			}
		}
		if (lastDate == null)
			return new Date(1);
		return lastDate;
	}
	
	public boolean needFile() {
		long fileId = getMostUrgentFile();
		if (fileId != 0) return true;
		return false;
	}
	
	/**
	 * We search the directory for the most urgent file.  This is the file with the highest priority and that is the most recent
	 * This means we search backward and store the file with the highest priority, only replacing it if there is another
	 * file with a higher priority
	 * @return 0 if no file to download, otherwise the file ID
	 */
	public long getMostUrgentFile() {
		if (files.size() < 1) return 0; // we have no files, so we can't request any
		PacSatFileHeader fileToDownload = null;
		for (int i=files.size()-1; i >=0; i--) {	
			PacSatFileHeader pfh = files.get(i);
			if (pfh.getState() != PacSatFileHeader.MSG && pfh.getState() != PacSatFileHeader.NEWMSG && pfh.userDownLoadPriority > 0) // then this has a priority, 0 means ignore
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
		
	public boolean add(BroadcastDirFrame dir) throws IOException {
		PacSatFileHeader pfh = dir.pfh;
		if (pfh != null)
			if (files.add(pfh)) {
				save();
				updateHoles(dir);
				return true;
			}
		return false;
	}
	
	public boolean add(BroadcastFileFrame bf) throws IOException, MalformedPfhException {
		PacSatFile psf = new PacSatFile(dirFolder, bf.fileId);
		PacSatFileHeader pfh;
		if (bf.offset == 0) {
			// extract the header, this is the first chunk
			//pfh = new PacSatFileHeader(bf.data);
			//add(pfh); ///////////////////////////////// If we add this we won't be able to update the holes.  This is a DUMMY entry. 
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
			long fileSize = pfh.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
			long holesLength = psf.getHolesSize();
			float percent = 100.0f;
			if (holesLength <= fileSize)
				percent = holesLength/(float)fileSize;
			String p = String.format("%2.0f", percent) ;
			data[files.size() - i][FileHeaderTableModel.HOLES] = "" + " " + psf.getNumOfHoles() + "/" + p + "%";
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
