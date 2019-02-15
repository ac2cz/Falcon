package fileStore;

import java.util.Collections;

public class SortedPfhArrayList extends SortedArrayList<PacSatFileHeader> {
	private static final long serialVersionUID = 1L;
	
	public boolean add(PacSatFileHeader img) {
		// make sure the same id is not anywhere in the list
		for (int i=0; i < this.size(); i++) {
			if (get(i).getFileId() == img.getFileId())
				return false;
		}
		return super.add(img);
	}
	
	/**
	 * Add this item or replace the existing item if it already exists
	 * @param img
	 * @return
	 */
	public boolean addOrReplace(PacSatFileHeader img) {
		for (int i=0; i < this.size(); i++) {
			if (get(i).getFileId() == img.getFileId()) {
				PacSatFileHeader old = set(i,img); // replace at this position TOD - only if newer??
				if (old == null) return false;
				return true;
			}
		}
		return super.add(img);
	}

}
