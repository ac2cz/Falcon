package fileStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import common.Config;
import common.Log;

public class DirSelectionCriteriaList {
	ArrayList<DirSelectionCriteria> selectionCriteria = new ArrayList<DirSelectionCriteria>();
	String dirFolder;
	public static final String LIST_FILE_NAME = "dirEquations";
	
	public DirSelectionCriteriaList(String satname) {
		dirFolder = Config.get(Config.LOGFILE_DIR) + satname;
		try {
			File select = new File(dirFolder + File.separator + LIST_FILE_NAME);
			if (select.exists()) {
				load();
				System.out.println(this);
			}
		} catch (ClassNotFoundException e) {
			Log.errorDialog("ERROR", "Could not load the directory selection criteria from disk, file is corrupt: " + e.getMessage());
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not load the directory selection criteria from disk: " + e.getMessage());
		}
	}
	
	public void add(DirSelectionCriteria criteria) {
		selectionCriteria.add(criteria);
	}
	
	/**
	 * Given a list of selection criteria and a PFH, do we select this?
	 * Iterate over the list seeing if any of them match this PFH
	 * @param pfh
	 * @return
	 */
	public boolean matchesPFH(PacSatFileHeader pfh) {
		for (DirSelectionCriteria criteria : selectionCriteria) {
			int key = criteria.getPfhFieldKey();
			PacSatField field = pfh.getFieldById(key);
			if (criteria.matches(field.getStringValue())) {
				return true;
			}
		}
		return false;
	}
	
	public void save() throws IOException {
		FileOutputStream fileOut = null;
		ObjectOutputStream objectOut = null;
		try {
			fileOut = new FileOutputStream(dirFolder + File.separator + LIST_FILE_NAME);
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(selectionCriteria);
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
			streamIn = new FileInputStream(dirFolder + File.separator + LIST_FILE_NAME);
			objectIn = new ObjectInputStream(streamIn);

			selectionCriteria = (ArrayList<DirSelectionCriteria>) objectIn.readObject();

		} finally {
			if (objectIn != null) try { objectIn.close(); } catch (Exception e) {};
			if (streamIn != null) try { streamIn.close(); } catch (Exception e) {};
		}
	}
	
	public String toString() {
		String s = "";
		for (DirSelectionCriteria crit : selectionCriteria) {
			s = s + crit + "\n";
		}
		return s;
	}
}
