package fileStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import common.Config;


/**
 * Zip example from https://www.baeldung.com/java-compress-and-uncompress
 * @author chris
 *
 */
public class ZipFile {
    public ZipFile(String sourceFile, String destZipFile) throws IOException {
    	compressFile(sourceFile, destZipFile);
    }
    
    public static byte[] zipBytes(byte[] inBytes) throws IOException {
    	File sourceFile = new File(Config.spacecraftSettings.directory.dirFolder + File.separator + "compressed.tmp");
    	FileOutputStream fos = new FileOutputStream(sourceFile);
    	fos.write(inBytes);
    	File destZipFile = new File(sourceFile.getPath() + ".zip");
    	compressFile(sourceFile.getPath(), destZipFile.getPath());
    	sourceFile.delete(); // not fatal if this fails, overwritten next time
    	byte[] outBytes = Files.readAllBytes(destZipFile.toPath());
    	destZipFile.delete(); // not fatal if this fails, overwritten next time
    	return outBytes;
    }
     
    private static void compressFile(String sourceFile, String destZipFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(destZipFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.close();
        fis.close();
        fos.close();
    }
    
}
