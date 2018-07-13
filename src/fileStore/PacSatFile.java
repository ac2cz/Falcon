package fileStore;

import java.io.EOFException;
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
		filename = makeBaseFileName();
		try {
			loadHoleList();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			
		}
	}
	
	public long getFileId() { return fileid; }
	
	public PacSatFileHeader getPfh() {
		pfh = Config.spacecraft.directory.getPfhById(fileid);
		return pfh;
	}
	
	public long getActualLength() {
		File file = new File(filename);
		long actualSize = file.length();
		return actualSize;
	}
	
	private String makeBaseFileName() {
		return directory + File.separator + Long.toHexString(fileid);
	}
	
	public String getFileName() {
		return filename  + ".act";
	}
	
	public String getHolFileName() {
		return filename  + ".hol";
	}
	
	public SortedArrayList getHoleList() {
		if (holes == null) return null;
		if (holes.size() == 1)
			if (holes.get(0).getFirst() == 0)
				return null; // this is the default hole
		return holes;
	}
	
	/**
	 * Add this fragment.  Save it to disk in the right place.
	 * Update the holes list.
	 * If this was the final fragment remove the holes list and return true.
	 * 
	 * @param bf
	 * @return
	 * @throws IOException
	 */
	public boolean addFrame(BroadcastFileFrame bf) throws IOException {
		saveFrame(bf);
		updateHoles(bf);
		return finalHoleCheck();
	}
	
	/**
	 * This is based on the algorithm for IP Packet re-assembly RFC 815 here:
	 * https://tools.ietf.org/html/rfc815
	 * 
	 * @param fragment
	 */
	private void updateHoles(BroadcastFileFrame fragment) {
		if (holes == null) {
			if (getPfh() == null || pfh.state != PacSatFileHeader.MSG) {
				holes = new SortedArrayList<FileHole>();
				holes.add(new FileHole(0, 0xFFFFFF)); // initialize with one hole that is maximum length for a file with a 24 bit length
			}
		}
		//Log.println("FRAG: " + Long.toHexString(fragment.fileId) + " " + fragment.getFirst() + " " + fragment.data.length + "  ->  ");
		
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
				FileHole newHole = new FileHole(hole.getFirst(),fragment.getFirst() - 1);
				newHoles.add(newHole);
				//Log.println("MADE HOLE1: " + newHole.getFirst() + " " + newHole.length);
			}
//			if (fragment.getLast() < hole.getLast() && fragment.hasLastByteOfFile()) 
//				continue; // we can discard the last hole, we have the last bytes in the file
//			else {
			if (fragment.getLast() < hole.getLast() /*&& !fragment.hasLastByteOfFile()*/) {  // last byte of file is always set so we cant use that check
				FileHole newHole = new FileHole(fragment.getLast() +1, hole.getLast());
				newHoles.add(newHole);
				//Log.println("MADE HOLE2: " + newHole.getFirst() + " " + newHole.length);
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
	
	private boolean finalHoleCheck() {
		PacSatFileHeader pfh = getPfh();
		if (pfh != null ) {
			if (pfh.getFieldById(PacSatFileHeader.FILE_SIZE) != null) {
				long len = pfh.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();

				if (holes.get(holes.size()-1).getFirst() == len) {
					// we can discard this final hole
					holes.remove(holes.size()-1);
				}
			}
		}
		if (holes.size() == 0) {
			File holeFile = new File(getHolFileName());
			holeFile.delete();
			return true;
		}
		return false;
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

			holes = (SortedArrayList<FileHole>) objectIn.readObject();

		} finally {
			if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
			if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
		}
	}
	
	public int getNumOfHoles() {
		if (holes == null) return 0;
		return holes.size();
	}
	
	public String getHoleListString() {
		if (holes == null) return "";
		String s = "";
		for (FileHole hole : holes)
			s = s + hole + "\n";
		return s;
	}
	
	private void saveFrame(BroadcastFileFrame bf) throws IOException {
		try {
			fileOnDisk = new RandomAccessFile(getFileName(), "rw"); // opens file and creates if needed
			fileOnDisk.seek(bf.offset);
			for (int i : bf.data)
				fileOnDisk.write(i);
		} finally {
			fileOnDisk.close();
		}
	}
	
	/**
	 * Get the text from the file, skipping the PFH if it exists
	 * @return
	 */
	public String getText() {
		String s = "";
		PacSatFileHeader pfh = getPfh();
		if (pfh != null) {
			try {
				fileOnDisk = new RandomAccessFile(getFileName(), "r"); // opens file and creates if needed
				fileOnDisk.seek(pfh.getFieldById(PacSatFileHeader.BODY_OFFSET).getLongValue());
				boolean readingBytes = true;
				while (readingBytes) {
					try {
						int i = fileOnDisk.readUnsignedByte();
						s = s + (char)i;
					} catch (EOFException e) {
						readingBytes = false;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try { fileOnDisk.close(); } catch (IOException e) { }
			}
		}
		
		return s;
	}

}
