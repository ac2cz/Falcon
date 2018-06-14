package fileStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import pacSat.frames.BroadcastFileFrame;

public class PacSatFile  {

	PacSatFileHeader pfh;
	RandomAccessFile fileOnDisk;
	
	public PacSatFile()  {
		
	}
	
	public static String makeFileName(long id) {
		return ""+Long.toHexString(id) + ".act";
	}
	
	public void saveFrame(BroadcastFileFrame bf) throws IOException {
		String name = makeFileName(bf.fileId);
		try {
			fileOnDisk = new RandomAccessFile(name, "rw"); // opens file and creates if needed
			fileOnDisk.seek(bf.offset);
			for (int i : bf.data)
				fileOnDisk.write(i);
		} finally {
			fileOnDisk.close();
		}
	}

}
