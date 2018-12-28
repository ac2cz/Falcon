package fileStore;

import java.io.Serializable;
import java.util.ArrayList;

public class DirSelectionEquation implements Serializable {
	private static final long serialVersionUID = 1L;
	
	ArrayList<DirSelectionCriteria> selectionCriteria;
	String dirFolder;
	
	public DirSelectionEquation(String satname) {
		selectionCriteria = new ArrayList<DirSelectionCriteria>();
	}
	
	public void add(DirSelectionCriteria criteria) {
		selectionCriteria.add(criteria);
	}
	
	// Create a hash key so we can avoid duplicate equations
	public String getHashKey() {
		String s = "";
		for (DirSelectionCriteria crit : selectionCriteria)
			s = s + crit.getHashKey() + "|";
		return s;
	}
	
	/**
	 * Given a list of selection criteria and a PFH, do we select this?
	 * Iterate over the list seeing if all of them match this PFH
	 * @param pfh
	 * @return
	 */
	public boolean matchesPFH(PacSatFileHeader pfh) {
		for (DirSelectionCriteria equation : selectionCriteria) {
			int key = equation.getPfhFieldKey();
			PacSatField field = pfh.getFieldById(key);
			if (field == null)
				return false;
			if (!equation.matches(field)) {
				// if any part does not match then we are done
				return false;
			}
		}
		return true; // all parts matched
	}
	
	public String toString() {
		String s = "";
		int j = 0;
		for (DirSelectionCriteria crit : selectionCriteria) {
			if (j > 0) s = s + " AND ";
			s = s + crit;
			j++;
		}
		return s;
	}
}
