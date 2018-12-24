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
import java.util.Iterator;

import common.Config;
import common.Log;
import pacSat.frames.BroadcastFileFrame;


/**
 * 
 * @author chris g0kla
 * 
 * Guiding principles for the PSF:
 * - We do not load the file until we need to. If we ask for the text then we load it
 * - If we are passed the PFH then we use that, otherwise we try to get it from the DIR
 *
 * There are several ways that this class can be instantiated. We need constructors for each of these:
 * 
 * 1) We want to query something about a file, such as the holes list.  We may or may not have a PFH.  All we need is the id
 * 2) We want to load a file into the Message Viewer to see it.  We may or may not have the whole file.  All we need is the id
 * 3) We have a FileBroadcast packet, the fileId and the PFH in the Directory.  
 *    The Constructor is called, the PFH is initialized from the Directory.  There is no need to load the file from Disk. We
 *    just need to be able to write to it
 * 4) We have received a FileBroadcast packet and we want to save it.  We know the fileId.   We are missing the PFH in the 
 *    Directory but it may be stored in the file already.
 *    We call the constructor with the fileId and load the file from Disk.  If the PFH is present we initiate it from that.  We do 
 *    not need to store the file contents, we just need to check for the header.
 * 5) We have created a new PFH and some data in the Message Editor.  We create a new PSF and save it to disk.  The FileID is zero. 
 *    The filename is CALLSIGN<NUM>.OUT
 * 6) We open the message editor for a past message that the user wants to edit.  We need to load the whole file from disk and store
 *    the PFH. The PFH needs to be editable, as does the loaded data.  We should be able to save and overwrite the file once it is
 *    edited.
 *    
 */
public class PacSatFile  {

	PacSatFileHeader pfh;
	RandomAccessFile fileOnDisk;
	String directory;
	long fileid;
	String filename;
	SortedArrayList<FileHole> holes;
	byte[] bytes;
	
	/**
	 * If we have the directory path and an id then we can query things about the file, like the holes list
	 * @param dir
	 * @param id
	 */
	public PacSatFile(String dir, long id)  {
		directory = dir;
		fileid = id;
		filename = makeActFileName();
		try {
			loadHoleList();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			
		}
	}
	
	/** 
	 * Load the file from disk, including the header.
	 * We don't need to know the id.  It may be blank if this was created by the editor
	 * @param fileName
	 * @throws IOException 
	 * @throws MalformedPfhException 
	 */
	public PacSatFile(String fileName) throws MalformedPfhException, IOException {
		File f = new File(fileName);
		filename = fileName;
		loadFile();
	}
	
	public PacSatFile(String file, PacSatFileHeader h, byte[] b) {
		filename = file;
		pfh = h;
		bytes = b;
	}
	
	public void setFileId(long id) { 
		fileid = id; 
		pfh.setFileId(id);
	}
	public long getFileId() { return fileid; }
	
	public void setPfh(PacSatFileHeader p) {
		pfh = p;
	}
	
	public void setBytes(byte[] b) {
		bytes = b;
	}
	
	public PacSatFileHeader getPfh() {
		if (pfh == null) // then try to get it from the directroy
			pfh = Config.spacecraft.directory.getPfhById(fileid);
		return pfh;
	}
	
	public long getActualLength() {
		File file = new File(filename);
		long actualSize = file.length();
		return actualSize;
	}
	
	private String makeActFileName() {
		return directory + File.separator + Long.toHexString(fileid) + ".act";
	}
	
	public String getFileName() {
		return filename;
	}
	
	public String getHolFileName() {
		return filename  + ".hol";
	}
	
	public SortedArrayList<FileHole> getHolesList() {
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
		if (getPfh() != null && ( pfh.state == PacSatFileHeader.NEWMSG || pfh.state == PacSatFileHeader.MSG)) return;
		if (holes == null) {
			holes = new SortedArrayList<FileHole>();
			holes.add(new FileHole(0, 0xFFFFFF)); // initialize with one hole that is maximum length for a file with a 24 bit length
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
	
	/**
	 * Check if the file is complete.  Return true if it is. 
	 * @return
	 */
	private boolean finalHoleCheck() {
		PacSatFileHeader pfh = getPfh();
		if (pfh != null && (pfh.state == PacSatFileHeader.NEWMSG ||  pfh.state == PacSatFileHeader.MSG)) return true; // we are already complete
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
	
	public long getHolesSize() {
		pfh = getPfh();
		long l = 0;
		if (holes == null) return 0; // no holes list means we are done
		for (FileHole hole : holes) {
			if (hole.getLast() < pfh.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue())
				l = l + hole.length;
		}
		return l;
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
				fileOnDisk = new RandomAccessFile(getFileName(), "r"); // opens file 
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
	
	/**
	 * Get the bytes from the file (typically if it is an image or other binary data)
	 * skipping the PFH if it exists
	 * @return
	 */
	public byte[] getBytes() {
		byte[] b = null;
		PacSatFileHeader pfh = getPfh();
		if (pfh != null) {
			long fileSize = pfh.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
			long offset = pfh.getFieldById(PacSatFileHeader.BODY_OFFSET).getLongValue();
			int dataLength = (int) (fileSize - offset);
			b = new byte[dataLength];
			int p=0;
			try {
				fileOnDisk = new RandomAccessFile(getFileName(), "r"); // opens file
				fileOnDisk.seek(pfh.getFieldById(PacSatFileHeader.BODY_OFFSET).getLongValue());
				boolean readingBytes = true;
				while (readingBytes) {
					try {
						int i = fileOnDisk.readUnsignedByte();
						b[p++] = (byte)i;
					} catch (EOFException e) {
						readingBytes = false;
					} catch (ArrayIndexOutOfBoundsException e) {
						// This means the datalength was wrong.  We have too much data
						Log.errorDialog("ERROR", "File: "+ getFileName() + "\nseems to have too many data bytes or the header is corrupt");
						break;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try { if (fileOnDisk != null) fileOnDisk.close(); } catch (IOException e) { }
			}
		}
		
		return b;
	}
	
	/**
	 * Load this files pfh from disk and store the header
	 * Typically used for an OUT file where there is no ID and we don't have the PFH in the directory
	 * @return
	 * @throws IOException 
	 * @throws MalformedPfhException 
	 */
	public void loadFile() throws MalformedPfhException, IOException {
		try {
			fileOnDisk = new RandomAccessFile(getFileName(), "r"); // opens file 
			pfh = new PacSatFileHeader(fileOnDisk);
		} finally {
			try { if (fileOnDisk != null) fileOnDisk.close(); } catch (IOException e) { }
		}
		bytes = getBytes();
	}
	
	public void save() {
		File file = new File(getFileName());

		if (file != null) {
			FileOutputStream saveFile = null;
			try {
				saveFile = new FileOutputStream(file);
				for (int i : pfh.getBytes())
					saveFile.write(i);
				saveFile.write(bytes);
	
			} catch (FileNotFoundException e) {
				Log.errorDialog("ERROR", "Error with file name: " + file.getAbsolutePath() + "\n" + e.getMessage());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Error writing file: " + file.getAbsolutePath() + "\n" + e.getMessage());
			} finally {
				try { saveFile.close(); } catch (Exception e) {}
			}
		}

	}
	
	public static void main(String[] args) {
		try {
//			PacSatFile psf = new PacSatFile("C:\\Users\\chris\\Desktop\\Test\\DEV2\\FalconSat-3\\AC2CZ8.out");
			String f = "C:\\Users\\chris\\Desktop\\Test\\FS-3\\FalconSat-3\\AC2CZ3.txt.out";
			PacSatFile psf = new PacSatFile(f);
			//psf.setFileId(0x1234);
			System.out.println(psf.getPfh());
			System.out.println(psf.getText());
			RandomAccessFile fileOnDisk = null;
			fileOnDisk = new RandomAccessFile(f, "r"); // opens file
			fileOnDisk.seek(1774);
			int i = fileOnDisk.readUnsignedByte();
			//psf.save();
		} catch (MalformedPfhException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
