package fileStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import pacSat.frames.BroadcastFileFrame;

public class PacSatFile  {

	PacSatFileHeader pfh;
	RandomAccessFile fileOnDisk;
	String directory;
	long fileid;
	String filename;
	
	public PacSatFile(String dir, long id)  {
		directory = dir;
		fileid = id;
		filename = makeFileName();
	}
	
	public long getFileId() { return fileid; }
	
	public String makeFileName() {
		return directory + File.separator + Long.toHexString(fileid) + ".act";
	}
	
	public void saveFrame(BroadcastFileFrame bf) throws IOException {
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
