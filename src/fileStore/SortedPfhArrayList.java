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
	public boolean addOrReplace(PacSatFileHeader img) {
		for (int i=0; i < this.size(); i++) {
			if (get(i).getFileId() == img.getFileId()) {
				if (img != null) {
					// log the size and reset the state if we now have partial download
					long existingDownloadedBytes = get(i).getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
					long downloadedBytes = img.getFieldById(PacSatFileHeader.FILE_SIZE).getLongValue();
					img.copyMetaData(get(i));
					if (existingDownloadedBytes < downloadedBytes) {
						img.state = PacSatFileHeader.PARTIAL;
						img.downloadedBytes = existingDownloadedBytes;
					}
				}
				// Delete the old one
				this.remove(i);
				return super.add(img);
			}
		}
		return super.add(img);
	}

}
