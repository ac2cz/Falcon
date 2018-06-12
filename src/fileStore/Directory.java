package fileStore;

import java.util.ArrayList;

public class Directory {
	ArrayList<PacSatFileHeader> files;
	
	public Directory() {
		files = new ArrayList<PacSatFileHeader>();
	}
	
	public boolean add(PacSatFileHeader pfh) {
		return files.add(pfh);
	}
	
	public String[][] getTableData() {
		String[][] data = new String[files.size()][PacSatFileHeader.MAX_TABLE_FIELDS];
		int i=0;
		for (PacSatFileHeader pfh : files)
			data[i++] = pfh.getTableFields();
		return data;
	}
}
