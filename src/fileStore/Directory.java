package fileStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import pacSat.frames.BroadcastFileFrame;
import common.Config;
import common.Log;

public class Directory  {
	ArrayList<PacSatFileHeader> files;
	public static final String DIR_FILE_NAME = "directory.db";
	
	public Directory() {
		files = new SortedArrayList<PacSatFileHeader>();
		try {
			load();
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
	
	public boolean add(PacSatFileHeader pfh) throws IOException {
		if (files.add(pfh)) {
			save();
			return true;
		}
		return false;
	}
	
	public boolean add(BroadcastFileFrame bf) throws IOException, MalformedPfhException {
		PacSatFile psf = new PacSatFile();
		psf.saveFrame(bf);
		if (bf.offset == 0) {
			// extract the header, this is the first chunk
			PacSatFileHeader pfh = new PacSatFileHeader(bf.data);
			add(pfh);
		}
		return true;
	}
	
	public String[][] getTableData() {
		String[][] data = new String[files.size()][PacSatFileHeader.MAX_TABLE_FIELDS];
		int i=0;
		// Put most recent at the top, which is opposite order
		for (PacSatFileHeader pfh : files)
			data[files.size() -1 - i++] = pfh.getTableFields();
		return data;

	}
	
	public void save() throws IOException {
		FileOutputStream fileOut = null;
		ObjectOutputStream objectOut = null;
		try {
			fileOut = new FileOutputStream(DIR_FILE_NAME);
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(files);
			Log.println("Saved directory to disk");
		} finally {
			if (objectOut != null) try { objectOut.close(); } catch (Exception e) {};
			if (fileOut != null) try { fileOut.close(); } catch (Exception e) {};
		}
		
	}
	
	public void load() throws IOException, ClassNotFoundException {
		ObjectInputStream objectIn = null;
		FileInputStream streamIn = null;
		try {
			streamIn = new FileInputStream(DIR_FILE_NAME);
			objectIn = new ObjectInputStream(streamIn);

			files = (SortedArrayList<PacSatFileHeader>) objectIn.readObject();

		} finally {
			if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
			if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
		}
	}
}
