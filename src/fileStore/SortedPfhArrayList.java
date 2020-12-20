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
	 * Add this item or delete the existing item and insert a new one if it already exists
	 * @param img
	 * @return
	 */
	public boolean addOrReplace(PacSatFileHeader pfh) {
		if (this.size() > 0) {
			int i = Collections.binarySearch(this, pfh);
			if (i >= 0) { // we found it
				//		for (int i=0; i < this.size(); i++) {
				if (get(i).getFileId() == pfh.getFileId()) {
					if (pfh != null) {
						// log the size and reset the state if we now have partial download
						long existingDownloadedBytes = get(i).getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
						long downloadedBytes = pfh.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
						pfh.copyMetaData(get(i));
						// Reset the status if we thought this was fully downloaded.  Dont change otherwise
						if ((pfh.state == PacSatFileHeader.MSG || pfh.state == PacSatFileHeader.NEWMSG)&& existingDownloadedBytes < downloadedBytes) {
							pfh.state = PacSatFileHeader.PARTIAL;
							pfh.downloadedBytes = existingDownloadedBytes;
						}
					}
					// Delete the old one
					this.remove(i);
					return super.add(pfh);
					//return super.add(pfh);
				}
			}
		}
		return super.add(pfh);
	}

}
