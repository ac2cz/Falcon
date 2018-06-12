package fileStore;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class PacSatFile extends RandomAccessFile{

	// Header
	
	
	public PacSatFile(String name, String mode) throws FileNotFoundException {
		super(name, mode);
		
	}

}
