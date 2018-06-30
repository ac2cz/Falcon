package fileStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

import pacSat.frames.BroadcastFileFrame;

public class PacSatFile  {

	PacSatFileHeader pfh;
	RandomAccessFile fileOnDisk;
	String directory;
	long fileid;
	String filename;
	ArrayList<FileHole> holes;
	
	public PacSatFile(String dir, long id)  {
		directory = dir;
		fileid = id;
		filename = makeFileName();
		holes = new ArrayList<FileHole>();
		holes.add(new FileHole(0, 0xFFFFFF)); // initialize with one hole that is maximum length for a file with a 24 bit length
	}
	
	public long getFileId() { return fileid; }
	
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
	 * Should use TreeSet
	 * 
	 * Then we can't add items to the collection we are iterating, so we copy the list to a new list as we iterate and add/delete
	 * as we go.  Then replace the old hole list with the new and save to disk
	 * 
	 * Finally the list needs to be loaded from disk when we hit here unless it is already in memory...
	 * 
	 * @param fragment
	 */
	private void updateHoles(BroadcastFileFrame fragment) {
		ArrayList<FileHole> newHoles = new ArrayList<FileHole>();
		for (Iterator<FileHole> iterator = holes.iterator(); iterator.hasNext();) {
			FileHole hole = iterator.next();
			if (fragment.getFirst() > hole.getLast()) {
				continue;
			}
			if (fragment.getLast() < hole.getFirst())
				continue;
			// The current hole is no longer valid we will remove it once we work out how it should be replaced
			if (fragment.getFirst() > hole.getFirst()) {
				FileHole newHole = new FileHole(hole.getFirst(),(int) (fragment.getFirst()-1-hole.getFirst()));
				
			}
			if (fragment.getLast() < hole.getLast() && !fragment.hasLastByteOfFile()) {
				FileHole newHole = new FileHole(fragment.getLast()+1, (int) (hole.getLast()-fragment.getLast()+1));
				
			}
			iterator.remove();			
		}
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
