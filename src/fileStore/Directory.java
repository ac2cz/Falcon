package fileStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

import ax25.KissFrame;
import pacSat.frames.BroadcastDirFrame;
import pacSat.frames.BroadcastFileFrame;
import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.telem.LogFileWE;
import gui.DirEquationTableModel;
import gui.FileHeaderTableModel;
import gui.MainWindow;

public class Directory  {
	List<PacSatFileHeader> files;
	public static final String DIR_FILE_NAME = "directory.db";
	public static final int PRI_NONE = 0;
	public static final int PRI_NEVER = 9;
	public String dirFolder = "directory";	
	String satname;
	//private SortedArrayList<DirHole> holes;
	HashMap<String, DirSelectionEquation> selectionList;
	static SpacecraftSettings spacecraftSettings;
	boolean needsSaving = false;
	boolean showUserFiles = true;
	
	public Directory(String satname, SpacecraftSettings spacecraftSettings) {
		this.satname = satname;
		dirFolder = Config.get(Config.LOGFILE_DIR) + File.separator + satname;
		Directory.spacecraftSettings = spacecraftSettings;
		files = Collections.synchronizedList(new SortedPfhArrayList()); // need this to be thread safe
		selectionList = new HashMap<String, DirSelectionEquation>();
		
		File dir = new File(dirFolder);
		if (!dir.exists()) {
			// new to try to make the dir
			if (!makeDir(dirFolder))
				System.exit(1);
		}
		try {
			load();
		} catch (ClassNotFoundException e) {
			Log.errorDialog("ERROR", "Could not load the directory from disk, file is corrupt: " + e.getMessage());
		} catch (IOException e) {
			Log.infoDialog("New Directory", "Could not load the directory from disk, making new directory in: \n" + dirFolder 
					+ "\n" + e.getMessage());
			// This is not fatal.  We create a new dir
		}
		
		try {
			loadDirSelections();
		} catch (ClassNotFoundException e) {
			Log.errorDialog("ERROR", "Could not load the directory select equations from disk, file is corrupt: " + e.getMessage());
		} catch (IOException e) {
			// This is not fatal.  We create a new hole list	
		}
	}
	
	public boolean hasHoles() {
		if (files.size() < 1) return false;
		if (getHolesList().size() <= 1) return false; // we always have a hole at the start for new files, not a real hole
		return true;
	}
	
	/**
	 * Return the age of the directory in days.  This is the number of days to the oldest hole
	 * @return
	 */
	public int getAge() {
		SortedArrayList<DirHole> holes = getHolesList();
		if (files.size() < 1) return 0;
		if (holes.size() < 1) return 0;
		DirHole oldestHole = holes.get(holes.size()-1);
		Date now = new Date();
		long first = oldestHole.getFirst();
		Date firstDate = new Date(first*1000);
		long diff = now.getTime() - firstDate.getTime();
		long diffDays = (int) (diff / (24 * 60 * 60 * 1000));
		return (int) diffDays;
	}
	
	public SortedArrayList<DirHole> getHolesList() {
		SortedArrayList<DirHole> holes = updateHoles();
		SortedArrayList<DirHole> agedHoles = ageHoleList(holes);
		return agedHoles;
	}
		
	public String getHolFileName() {
		return dirFolder + File.separator + DIR_FILE_NAME + ".hol";
	}
	
	public String getDirSelectionsFileName() {
		return dirFolder + File.separator + DIR_FILE_NAME + ".equ";
	}
	
	private SortedArrayList<DirHole> initEmptyHolesList() {
		SortedArrayList<DirHole> holes = new SortedArrayList<DirHole>();
		Date frmDate, toDate;
		int[] toBy = {0xff,0xff,0xff,0x7f}; // end of time, well 2038.. This is the max date for a 32 bit in Unix Timestamp
		long to = KissFrame.getLongFromBytes(toBy);
		toDate = new Date(to*1000);
		frmDate = agedDirDate();
		DirHole hole = new DirHole(frmDate,toDate);
		holes.add(hole); // initialize with one hole that is maximum length 
		return holes;
	}
	
	private Date agedDirDate() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		int lookback = spacecraftSettings.getInt(SpacecraftSettings.DIR_AGE);
		cal.add(Calendar.DATE, -1 * (lookback));
		return cal.getTime();
	}
	
	/**
	 * This updates the DIR holes based on the DIR headers.  It starts with the oldest and then proceeds to the most recent.
	 * DIR age is applied when we ask for a list of holes.  This is the complete set.
	 */
	private SortedArrayList<DirHole> updateHoles() {
		if (files == null || files.size() == 0)
			return initEmptyHolesList();
		PacSatFileHeader prev = null;
		SortedArrayList<DirHole> holes = new SortedArrayList<DirHole>();
		// Iterate over all the headers
		synchronized(files) {
			for (Iterator<PacSatFileHeader> iterator = files.iterator(); iterator.hasNext();) {
				PacSatFileHeader current = iterator.next();
				if (prev == null) {
					prev = current; // then this is the oldest file
				} else {
					if (prev.timeNew +1 < current.timeOld) {
						// we have a hole between the dates.  Prev is the oldest
						DirHole hole = new DirHole(prev.timeNew+1, current.timeOld-1);
						holes.add(hole);
					}
					prev = current;
				}
			}
		}
		// Add one last hole for files at start
		int[] toBy = {0xff,0xff,0xff,0x7f}; // end of time, well 2038.. This is the max date for a 32 bit in Unix Timestamp
		long to = KissFrame.getLongFromBytes(toBy);
		long from = prev.timeNew+1;
		DirHole hole = new DirHole(from,to);
		holes.add(hole); // initialize with one hole from most recent file to end of time 
		return holes;
	}
	
	/**
	 * Check the holes list vs the requested AGE that the user wants the directory to be.  Then only request holes that
	 * fit in the period.
	 * Each hole has two dates: from and to or first and last.  
	 * Last needs to be within the period
	 * If Last is within the period, but first is not, then first is set to the maximum amount.
	 * @param holes
	 * @return
	 */
	SortedArrayList<DirHole> ageHoleList(SortedArrayList<DirHole> holes) {
		SortedArrayList<DirHole> newHoles = new SortedArrayList<DirHole>();
		Date age = agedDirDate();
		Date now = new Date();
		boolean moreHoles = true;
		for (Iterator<DirHole> iterator = holes.iterator(); iterator.hasNext() && moreHoles;) {
			DirHole hole = iterator.next();
			long last = hole.getLast();
			Date lastDate = new Date(last*1000);
			long diff = now.getTime() - lastDate.getTime();
			long days = diff/(24*60*60*1000);
			if ( days > spacecraftSettings.getInt(SpacecraftSettings.DIR_AGE) ) {
				moreHoles = false;
				if (newHoles.size() == 0)
					hole = new DirHole(hole.getFirstDate(), age); 
				else 
					hole = null;
			} 
			if (hole != null) {// this is valid and within the lookback period (age)
				// Now check the first date and set it to the age if it is too early
				long first = hole.getFirst();
				Date firstDate = new Date(first*1000);
				long firstDiff = now.getTime() - firstDate.getTime();
				long firstDays = firstDiff/(24*60*60*1000);
				if ( firstDays > spacecraftSettings.getInt(SpacecraftSettings.DIR_AGE) ) {
					hole = new DirHole(age, hole.getLastDate());
				}
				newHoles.add(hole);
			}
		}
		return newHoles;
	}
	
	private void saveDirSelections() throws IOException {
		FileOutputStream fileOut = null;
		ObjectOutputStream objectOut = null;
		try {
			fileOut = new FileOutputStream(getDirSelectionsFileName());
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(selectionList);
			//Log.println(filename + ": Saved holes to disk");
		} finally {
			if (objectOut != null) try { objectOut.close(); } catch (Exception e) {};
			if (fileOut != null) try { fileOut.close(); } catch (Exception e) {};
		}
	}
	public void loadDirSelections() throws IOException, ClassNotFoundException {
		ObjectInputStream objectIn = null;
		FileInputStream streamIn = null;
		try {
			streamIn = new FileInputStream(getDirSelectionsFileName());
			objectIn = new ObjectInputStream(streamIn);

			selectionList = (HashMap<String, DirSelectionEquation>) objectIn.readObject();
			
		} finally {
			if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
			if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
		}
	}
	
	public void add(DirSelectionEquation equation) throws IOException {
		selectionList.put(equation.getHashKey(), equation); // hopefully takes care of dupes
		saveDirSelections();
		
	}
	
	public void deleteEquations() throws IOException {
		remove(getDirSelectionsFileName());
		selectionList = new HashMap<String, DirSelectionEquation>();
	}
	
	public String getEquationsString() {
		String s = "";
		for (String key : selectionList.keySet()){
	        //iterate over keys
	        s = s + (selectionList.get(key) + "\n");
	    }
		return s;
	}

	public DirSelectionEquation getEquation(String key) {
		return selectionList.get(key);
	}
	
	public void deleteEquation(String key) {
		selectionList.remove(key);
		try {
			saveDirSelections();
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not save the Directory Equations File: " + e.getMessage());
		}
		
	}
	
	public String[][] getEquationsData() {
		String[][] data = new String[selectionList.size()][DirEquationTableModel.MAX_TABLE_FIELDS];
		if (selectionList.size() == 0) {
			return (DirEquationTableModel.BLANK);
		}
		int k = 0;
		for (String key : selectionList.keySet()){
	        //iterate over keys
			data[k][0] = ""+key;
	        data[k++][1] = ""+selectionList.get(key);
	    }
		return data;
	}
	
	public DirSelectionEquation hasMatchingEquation(PacSatFileHeader pfh) {
		for (String key : selectionList.keySet()){
	        //iterate over keys
	        DirSelectionEquation eq = selectionList.get(key);
	        if (eq.matchesPFH(pfh))
	        	return eq;
	    }
		return null;
	}
		
	public Date getLastHeaderDate() {
		Date lastDate = null;
		synchronized(files) {
			if (files.size() < 1) return new Date(1); // return 1970 as we have not files, so we want them all
			for (int i=files.size()-1; i>=0; i--) {
				lastDate = files.get(files.size()-1).getDate(PacSatFileHeader.UPLOAD_TIME);

				if (lastDate != null) {
					break;
				}
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
		synchronized(files) {
			for (int i=files.size()-1; i >=0; i--) {	
				PacSatFileHeader pfh = files.get(i);
				if (pfh.getState() != PacSatFileHeader.MISSING && pfh.getState() != PacSatFileHeader.MSG 
						&& pfh.getState() != PacSatFileHeader.NEWMSG 
						&& pfh.userDownLoadPriority > PRI_NONE && pfh.userDownLoadPriority < PRI_NEVER) // then this has a priority, 0 means ignore
					if (fileToDownload == null)
						fileToDownload = pfh;
					else if (pfh.userDownLoadPriority < fileToDownload.userDownLoadPriority)
						fileToDownload = pfh; // then this is higher priority, store this one instead
			}
		}
		if (fileToDownload == null) 
			return 0;
		else 
			return fileToDownload.getFileId();
	}
	
	public PacSatFileHeader getPfhById(long id) {
		synchronized(files) {
			for (PacSatFileHeader pfh : files)
				if (pfh.getFileId() == id)
					return pfh;
		}
		return null;
	}
	
	/**
	 * Add a new received Broadcast Dir packet to the directory.  This will add a PFH if we do not already have it.
	 * The directory holes list will be updated
	 * 
	 * If the E bit is not set, or if we have an offset and the E Bit then this does not have the whole PFH.  We need 
	 * to store the part we have and make the whole PFH later
	 * 
	 * If we already have the PFH then it is essential that we update it with the latest values as they shift on the 
	 * spacecraft!
	 * 
	 * @param dir
	 * @return - true if the dir needs to be updated on the screen
	 * @throws IOException
	 */
	public boolean add(BroadcastDirFrame dir) throws IOException {
		PacSatFileHeader existingPfh = getPfhById(dir.fileId);
		
		if (dir.hasEndOfFile() && dir.getOffset() == 0) {
			// We have the whole PFH in this Broadcast Frame, so pfh will already be generated
			PacSatFileHeader pfh = dir.pfh;
			if (pfh != null) {
				synchronized(files) {
					if (((SortedPfhArrayList) files).addOrReplace(pfh)) {
						//save();
						needsSaving = true;
						if (existingPfh != null) {
							PacSatFile psf = new PacSatFile(dirFolder, pfh.getFileId());
							psf.addFrame(dir); // this will replace the header and update holes etc
						}
						return true;
					}
				}
			}
		} else {
			// This is part of a PFH, which we will save in a temp file as the offset is a file byte offset
			// If the file is complete then we add the PFH
			PacSatFile psf = new PacSatFile(dirFolder, dir.fileId);
			psf.addFrame(dir);

			RandomAccessFile fileOnDisk = new RandomAccessFile(psf.getFileName(), "r"); // opens file 
			try {
				PacSatFileHeader pfh = new PacSatFileHeader(fileOnDisk);
				if (pfh != null) {
					// Need to set the old and new dates as these are not stored in the file
					pfh.timeNew = dir.getLast();
					pfh.timeOld = dir.getFirst();
					synchronized(files) {
						if (((SortedPfhArrayList) files).addOrReplace(pfh)) {
							//save();
							needsSaving = true;
							if (existingPfh != null) {
								PacSatFile psf2 = new PacSatFile(dirFolder, pfh.getFileId());
								psf2.addFrame(dir); // this will replace the header and update holes etc
							}
							return true;
						}
					}
				}
			} catch (MalformedPfhException e) {
				// Nothing to do, this is likely a partially populated header
			}
		}

		return false;
	}

	/**
	 * Add a new fragment of a file in a Broadcast File packet.  Use the received information to update the state of the file on
	 * the PacSat Header if we have it.
	 * 
	 * FIXME:
	 * We can have the situation where the PacSat Header exists in the file but we have not received it from the spacecraft in a Dir
	 * Broadcast.  We can also have the situation where we receive the whole file and never get another fragment before we receive the
	 * dir broadcast.  This implies that the state of the file and its holes should be updated by either type of received data.
	 * 
	 * @param bf
	 * @return true if the was added to a file.  False if no update was made because file is already complete.
	 * @throws IOException
	 * @throws MalformedPfhException
	 * @throws LayoutLoadException 
	 * @throws DataLoadException 
	 * @throws NumberFormatException 
	 */
	public boolean add(BroadcastFileFrame bf) throws IOException, MalformedPfhException, LayoutLoadException, NumberFormatException, DataLoadException {
		PacSatFileHeader pfh;
		pfh = getPfhById(bf.fileId);
		if (pfh != null) {
			if (pfh.state == PacSatFileHeader.MSG || pfh.state == PacSatFileHeader.NEWMSG) {
				// Note that if that state is MISSING we will accept it.  Perhaps data was a recording
				pfh.userDownLoadPriority = 0; // we have this.  Don't attempt to download it anymore
				return false;
			}
		}
		PacSatFile psf = new PacSatFile(dirFolder, bf.fileId);

		if (psf.addFrame(bf)) { // returns true if this was the final frame and we are complete
			if (pfh.state != PacSatFileHeader.MSG) {
				pfh.setState(PacSatFileHeader.NEWMSG);
				pfh.userDownLoadPriority = 0; // we have this.  Don't attempt to download it anymore
			}
			
			// This is where post processing hooks go. Ideally this is configurable.
			// For now this is where we process a WE file
			postFileProcessing(pfh, psf);
		} else if (pfh != null) {
			// we have the header and some data
			pfh.setState(PacSatFileHeader.PARTIAL);
		}
		return true;
	}
	
	public void postFileProcessing(PacSatFileHeader pfh, PacSatFile psf) throws IOException, MalformedPfhException, LayoutLoadException, NumberFormatException, DataLoadException {
		
		if (pfh.getType() == 3) { // WOD
			// Extract the telemetry
			File file = psf.extractSystemFile();
			if (file != null) {
				LogFileWE we = new LogFileWE(file.getPath());
				if (we.records != null)
					for (DataRecord d : we.records) {
						Config.db.add(d);
					}
			}
		} else {
			//File file = psf.extractUserFile();
		}
	}
	
	public void setPriority(long id, int pri) {
		PacSatFileHeader pfh = getPfhById(id);
		pfh.userDownLoadPriority = pri;
		if (pri < 0)// error
			pfh.state = PacSatFileHeader.MISSING;
	}
	
	public void setShowFiles(boolean show) { showUserFiles = show; }
	
	/**
	 * We get the Directory table data ready for display in the GUI.  This is called whenever it changes.
	 * This is also the point where we can apply automatic priority criteria to downloads.
	 * e.g. Download all files TO my callsign
	 *      Download messages to ALL under 20k etc
	 *      
	 * @return
	 */
	public String[][] getTableData() {
		boolean changedPriorities = false;
		String[][] filtered = new String[files.size()][FileHeaderTableModel.MAX_TABLE_FIELDS];
		String[][] data = null;
		int i=0;
		synchronized(files) {
			// Put most recent at the top, which is opposite order
			for (PacSatFileHeader pfh : files) {
				//	if (pfh.getFileId() == 0x3a3) // debug one file
				//		Log.println("STOP");
				// Set the priority automatically
				if ((pfh.userDownLoadPriority != PacSatFileHeader.PRI_N && pfh.state != PacSatFileHeader.NEWMSG && pfh.state != PacSatFileHeader.MSG )) {
					DirSelectionEquation equ = hasMatchingEquation(pfh);
					if (equ != null) {
						pfh.userDownLoadPriority = equ.priority;
						changedPriorities = true;
					}
				}

				PacSatFile psf = new PacSatFile(dirFolder, pfh.getFileId());
				
				// This populates all the fields
				String header[] = pfh.getTableFields();
				String toCall = header[FileHeaderTableModel.TO];
				boolean added = false;
				if (toCall != null)
					if (showUserFiles) {
						if (!toCall.equalsIgnoreCase("") ) {
							filtered[i++] = header; 
							added = true;
						}
					} else { // we show everything
						filtered[i++] = header; 
						added = true;
					}

				if (added) {
					// Now update just the holes column
					long fileSize = pfh.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
					long holesLength = psf.getHolesSize();
					float percent = 1.0f;
					if (holesLength > 0 && holesLength <= fileSize)
						percent = holesLength/(float)fileSize;
					else if (pfh.state == 0) // no state
						percent = 0;
					String p = String.format("%2.0f", percent*100);
					int h = psf.getNumOfHoles();
					filtered[i-1][FileHeaderTableModel.HOLES] = "" + " " + h + "/" + p + "%";
				}
			}
		}
		data = new String[i][];
		for (int j1=0; j1<i; j1++)
			data[i - j1 - 1] = filtered[j1];
		if (changedPriorities)
			needsSaving = true;
//			try {
//				save();
//			} catch (IOException e) {
//				Log.errorDialog("ERROR", "Can't save the directory.  Check the path is writable:\n " + dirFolder + File.separator + DIR_FILE_NAME + "\n");				
//			}
		return data;

	}
	
	public void save() throws IOException {
		FileOutputStream fileOut = null;
		ObjectOutputStream objectOut = null;

		try {
			synchronized(files) {
				fileOut = new FileOutputStream(dirFolder + File.separator + DIR_FILE_NAME);
				objectOut = new ObjectOutputStream(fileOut);
				objectOut.writeObject(files);
				//Log.println("Saved directory to disk");
				needsSaving = false;
			}
		} finally {
			if (objectOut != null) try { objectOut.close(); } catch (Exception e) {};
			if (fileOut != null) try { fileOut.close(); } catch (Exception e) {};
		}
	}
	
	public void saveIfNeeded() {
		if (needsSaving)
			try {
				save();
				Log.println("Directory was saved in the background");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
	}

	public void load() throws IOException, ClassNotFoundException {
		synchronized(files) {
			files = new SortedPfhArrayList();
			ObjectInputStream objectIn = null;
			FileInputStream streamIn = null;
			try {
				streamIn = new FileInputStream(dirFolder + File.separator + DIR_FILE_NAME);
				objectIn = new ObjectInputStream(streamIn);
				SortedPfhArrayList tmpFiles = (SortedPfhArrayList) objectIn.readObject();

				for (PacSatFileHeader pfh : tmpFiles) {
					if (!files.add(pfh)) {
						// this takes care of duplicates
						Log.println("ERROR: Duplicate PFH.  Ignored on load: " + pfh);
					}
				}

			} catch (ClassCastException e1) {
				Log.println("ClassFormatError:  Trying to convert old deirectory format");
				// we have the old file format.  Close, reopen and try again
				if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
				if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};

				streamIn = new FileInputStream(dirFolder + File.separator + DIR_FILE_NAME);
				objectIn = new ObjectInputStream(streamIn);

				SortedArrayList<PacSatFileHeader> tmpFiles = (SortedArrayList<PacSatFileHeader>) objectIn.readObject();
				for (PacSatFileHeader pfh : tmpFiles) {
					if (!files.add(pfh)) {
						// this takes care of duplicates
						Log.println("ERROR: Duplicate PFH.  Ignored on load: " + pfh);
					}
				}
			} finally {
				if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
				if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
			}
		}
	}
	
	/**
	 * Make the database directory if needed.  Check to see if we have existing legacy data and run the conversion if we do
	 * @param dir
	 */
	public static boolean makeDir(String dir) {
		
		File aFile = new File(dir);
		if(!aFile.isDirectory()){
			Log.println("Making new database: " + dir);
			aFile.mkdir();
			if(!aFile.isDirectory()){
				Log.errorDialog("ERROR", "ERROR can't create the directory.  Check the path is writable:\n " + aFile.getAbsolutePath() + "\n");
				
				return false;
			}
			return true;
		}
		
		return false;
	}
	
	public static void remove(String f) throws IOException {
		try {
			File file = new File(f);
			if (file.exists())
				if(file.delete()){
					Log.println(file.getName() + " is deleted!");
				}else{
					Log.println("Delete operation failed for: "+ file.getName());
					throw new IOException("Could not delete file " + file.getName() + " Check the file system and remove it manually.");
				}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					ex.toString(),
					"Error Deleting File",
					JOptionPane.ERROR_MESSAGE) ;
		}
	}
	
	/**
	 * Utility function to copy a file
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	@SuppressWarnings("resource") // because we have a finally statement and the checker does not seem to realize that
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
	/**
	 * Copy the headers into an archive
	 * Copy all of the underlying files
	 * Purge from this dir
	 * 
	 * We want to keep a certain number of files that are most recent.  So traverse the directory backwards.
	 * 
	 * @param archiveDir
	 */
	public void archiveDir(String archiveDir) {
		SortedPfhArrayList dirFiles = new SortedPfhArrayList(); 
		String archiveDirFolder = Config.get(Config.ARCHIVE_DIR) + File.separator + satname;
		File dir = new File(archiveDirFolder);
		if (!dir.exists()) {
			// new to try to make the dir
			if (!makeDir(archiveDirFolder))
				return;
		}
		boolean error = false;
		boolean atLeastOneArchived = false;

		int keepNumber = this.spacecraftSettings.getInt(SpacecraftSettings.NUMBER_DIR_TABLE_ENTRIES);
		try {
			for (int i = files.size()-keepNumber-1; i >=0; i--) {
				PacSatFileHeader pfh = files.get(i);

				// First move any file on disk
				String id = pfh.getFileName();
				File f = new File(Config.spacecraftSettings.directory.dirFolder + File.separator + id + ".act");
				if (f.exists()) {	
					File f2 = new File(archiveDirFolder + File.separator + id + ".act");
					copyFile(f,f2);
					if (f2.exists())
						remove(f.getAbsolutePath());
				}
				File f3 = new File(Config.spacecraftSettings.directory.dirFolder + File.separator + id + ".act.hol");
				if (f3.exists()) {	
					File f4 = new File(archiveDirFolder + File.separator + id + ".act.hol");
					copyFile(f3,f4);
					if (f4.exists())
						remove(f3.getAbsolutePath());
				}
				// removed or did not exist, so copy the dir entry 
				if (!dirFiles.add(pfh)) {
					JOptionPane.showMessageDialog(MainWindow.frame,
							"File: " + pfh.getFileName(),
							"Error Copying PacSatFileHeader to archive",
							JOptionPane.ERROR_MESSAGE) ;
					error = true;
					break;
				}
				atLeastOneArchived = true;

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			error = true;
		}
		if (atLeastOneArchived && !error) {
			// Save the archived dir
			
			FileOutputStream fileOut = null;
			ObjectOutputStream objectOut = null;

			try {
				fileOut = new FileOutputStream(Config.get(Config.ARCHIVE_DIR) + File.separator + satname + File.separator + DIR_FILE_NAME);

				objectOut = new ObjectOutputStream(fileOut);
				objectOut.writeObject(dirFiles);
				Log.println("Saved archive to disk");

				// we can now purge the headers from this dir
				// Purge backwards so the index is not corrupted as we remove
				// Keep the requested amount
				for (int i = files.size()-keepNumber-1; i >=0; i--) {
					files.remove(i);
				}
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(MainWindow.frame,
						"File: " + Config.get(Config.ARCHIVE_DIR) + File.separator + satname + File.separator + DIR_FILE_NAME,
						"Error writing to archive.  File not found.",
						JOptionPane.ERROR_MESSAGE) ;
				e.printStackTrace(Log.getWriter());

			} catch (IOException e) {
				JOptionPane.showMessageDialog(MainWindow.frame,
						"File: " + Config.get(Config.ARCHIVE_DIR) + File.separator + satname + File.separator + DIR_FILE_NAME,
						"IO error writing to archive.",
						JOptionPane.ERROR_MESSAGE) ;
				e.printStackTrace(Log.getWriter());
			} finally {
				if (objectOut != null) try { objectOut.close(); } catch (Exception e) {};
				if (fileOut != null) try { fileOut.close(); } catch (Exception e) {};
			}
			
			
		}
	}
	
//	public static void main(String[] args) {
//		System.out.println("TEST");
//		String[] letters = {"A", "B", "C", "D", "E"};
//		int keepNumber = 2;
//		
//		ArrayList<String> files = new ArrayList<String>();
//		for (String s : letters)
//			files.add(s);
//
//		for (int i = files.size()-keepNumber-1; i >=0; i--) {
//			files.remove(i);
//		}
//
//		for (String s : files)
//			System.out.println(s);
//		
//	}

}
