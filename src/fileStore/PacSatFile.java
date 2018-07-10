package fileStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

import common.Config;
import common.Log;
import pacSat.frames.BroadcastFileFrame;

public class PacSatFile  {

	PacSatFileHeader pfh;
	RandomAccessFile fileOnDisk;
	String directory;
	long fileid;
	String filename;
	SortedArrayList<FileHole> holes;
	
	public PacSatFile(String dir, long id)  {
		directory = dir;
		fileid = id;
		filename = makeFileName();
		try {
			loadHoleList();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			holes = new SortedArrayList<FileHole>();
			holes.add(new FileHole(0, 0xFFFFFF)); // initialize with one hole that is maximum length for a file with a 24 bit length
		}
	}
	
	public long getFileId() { return fileid; }
	
	public long getActualLength() {
		File file = new File(filename);
		long actualSize = file.length();
		return actualSize;
	}
	
	public String makeFileName() {
		return directory + File.separator + Long.toHexString(fileid) + ".act";
	}
	
	public void addFrame(BroadcastFileFrame bf) throws IOException {
		saveFrame(bf);
		updateHoles(bf);
	}
	
	/**
	 * This is based on the algorithm for IP Packet re-assembly RFC 815 here:
	 * https://tools.ietf.org/html/rfc815
	 * 
	 * A couple of implementation issues to address.  The List needs to be sorted. The holes need to be in order.
	 * 
	 * Then we can't add items to the collection we are iterating, so we copy the list to a new list as we iterate and add/delete
	 * as we go.  Then replace the old hole list with the new and save to disk
	 * 
	 * Finally the list needs to be loaded from disk when we hit here unless it is already in memory...
	 * 
	 * @param fragment
	 */
	private void updateHoles(BroadcastFileFrame fragment) {
		SortedArrayList<FileHole> newHoles = new SortedArrayList<FileHole>();
		for (Iterator<FileHole> iterator = holes.iterator(); iterator.hasNext();) {
			FileHole hole = iterator.next();
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
				FileHole newHole = new FileHole(hole.getFirst(),(int) (fragment.getFirst()-1-hole.getFirst()));
				newHoles.add(newHole);
			}
			if (fragment.getLast() < hole.getLast() && !fragment.hasLastByteOfFile()) {
				FileHole newHole = new FileHole(fragment.getLast()+1, (int) (hole.getLast()-fragment.getLast()+1));
				newHoles.add(newHole);
			}		
		}
		holes = newHoles;
		try {
			saveHoleList();
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not write hole file to disk: " + filename + ".hol\n" + e.getMessage());
			e.printStackTrace();
		}
	}

	private void saveHoleList() throws IOException {
		FileOutputStream fileOut = null;
		ObjectOutputStream objectOut = null;
		try {
			fileOut = new FileOutputStream(filename + ".hol");
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
			streamIn = new FileInputStream(filename + ".hol");
			objectIn = new ObjectInputStream(streamIn);

			holes = (SortedArrayList<FileHole>) objectIn.readObject();

		} finally {
			if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
			if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
		}
	}
	
	public int getNumOfHoles() {
		return holes.size();
	}
	
	public String getHoleListString() {
		String s = "";
		for (FileHole hole : holes)
			s = s + hole + "\n";
		return s;
	}
	
	private void saveFrame(BroadcastFileFrame bf) throws IOException {
		try {
			fileOnDisk = new RandomAccessFile(filename, "rw"); // opens file and creates if needed
			fileOnDisk.seek(bf.offset);
			for (int i : bf.data)
				fileOnDisk.write(i);
		} finally {
			fileOnDisk.close();
		}
	}

}
