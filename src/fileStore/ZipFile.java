package fileStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
    
    // Zip bytes in memory and provide the name of the file that they encode
    public static byte[] zipBytes(String filename, byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry(filename);
        entry.setSize(input.length);
        zos.putNextEntry(entry);
        zos.write(input);
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
    }
    
    public static byte[] zipBytes(byte[] inBytes) throws IOException {
    	File sourceFile = new File(Config.get(Config.LOGFILE_DIR) + File.separator + "compressed.tmp");
    	FileOutputStream fos = new FileOutputStream(sourceFile);
    	fos.write(inBytes);
    	File destZipFile = new File(sourceFile.getPath() + ".zip");
    	compressFile(sourceFile.getPath(), destZipFile.getPath());
    	sourceFile.delete(); // not fatal if this fails, overwritten next time
    	byte[] outBytes = Files.readAllBytes(destZipFile.toPath());
    	destZipFile.delete(); // not fatal if this fails, overwritten next time
    	fos.close();
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
