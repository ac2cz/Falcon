package fileStore;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import ax25.KissFrame;
import common.Log;

public class XcamImg {
	public static final int TYPE_THUMB = 0x04;
	public static final int TYPE_FULL = 0x00;
	
	public static final int PACKET_HEADER_LEN = 9;
	public static final int PACKET_DATA_LEN = 249;
	public static final int PACKET_CRC_LEN = 2;
	public static final int PACKET_LEN = PACKET_HEADER_LEN+PACKET_DATA_LEN+PACKET_CRC_LEN;
	
	byte[] bytes;
	byte[] processedBytes;
	int type = TYPE_THUMB;
	BufferedImage image;
	int imageType = BufferedImage.TYPE_BYTE_GRAY;
	int width = 120; // default Thumbnail size
	int height = 100; 
	
	
	public XcamImg(byte[] bytes) {
		this.bytes = bytes;
		processFile();
		try {
		if (type == TYPE_THUMB)
			makeGrayScaleImage();
		else
			make2x2BinnedImage();
		} catch (ArrayIndexOutOfBoundsException e) {
			// partial file can cause strange issues that we ignore
		}
	}
	
	public XcamImg(String filename) throws IOException {
		load(filename);
		processFile();
		try {
		if (type == TYPE_THUMB)
			makeGrayScaleImage();
		else
			make2x2BinnedImage();
		} catch (ArrayIndexOutOfBoundsException e) {
			// partial file can cause strange issues that we ignore			
		}
	}
	
	/**
	 * Loop through the bytes in the file.  The format is as follows:
	 * 	Each frame of image data is 1.3 Mbytes consisting of 1.3 million 8-bit words. This translates to 6418 data packets for un-compressed data.
	 *  Each frame will consist of 4-byte frame identifier followed by 252 bytes of data .
	 *  The first two identifier bytes contain file information relating to the platform file system indicating the source of the data and what compression has been carried out (details TBD in conjunction with platform).
	 *  The second two identifier bytes will define the frame number in the sequence.
	 *  The following 252 bytes will contain data.
	 *  
	 *  Note that a thumbnail is shorter
	 * @param filename
	 */
	private void processFile() {
		if (bytes.length % PACKET_LEN != 0) {
			// seems like that would be an error
			Log.println("Error: Hmm, wrong length for image packet?");
			//throw new IllegalArgumentException("Data does not contain 256 byte packets");
		}
		int processedLength = bytes.length / PACKET_LEN;
		
		processedBytes = new byte[processedLength*PACKET_DATA_LEN];
		//System.err.println("Processing: " + processedLength + " packets");
		int i = 0;  // position we are reading in the bytes
		int frameNum = 0; // the frame that we expect next.  We assume they come in order as this is processed from a pacsat file
		while (i < bytes.length) {
			byte[] line = new byte[PACKET_HEADER_LEN+PACKET_DATA_LEN];
			int h=0;
			for (h=0; h <9; h++)
				line[h] = bytes[h];
			h = 9;
			int[] by = {bytes[i],bytes[i+1],bytes[i+2],bytes[i+3]};
			long fileInfo = KissFrame.getLongFromBytes(by); ////////////////// TODO - No indication that this is little endian
			i += 4;
			int[] by2 = {bytes[i],bytes[i+1],bytes[i+2],bytes[i+3]};
			int num = (int)KissFrame.getLongFromBytes(by2); /////////// TODO - this looks to be Big endian and only 2 bytes
			i += 4;
			
			type = bytes[i];  ///////////// TO DO - SPEC says 2 bytes, looks to be one byte
			// If thumb and processLength != 49 then we have an error!!
			if (type == TYPE_THUMB && processedLength != 49) throw new IllegalArgumentException("Wrong length for thumbnail type file.  Packet number: " + processedLength);
			
			i++;
//			if (frameNum != num) throw new IllegalArgumentException("File frame num "+num+" is wrong, expected: " + frameNum);
			
			
			for (int j=0; j < PACKET_DATA_LEN; j++) {
				line[h+j] = bytes[i];
				processedBytes[frameNum*PACKET_DATA_LEN + j] = bytes[i++];

			}
			int[] by3 = {bytes[i],bytes[i+1]};
			short crc = (short)KissFrame.getIntFromBytes(by3);
			short actCrc = PacSatFileHeader.checksum(line); // the checksums do not pass!!
			//System.out.println("CRC:" + Integer.toHexString(crc) + " ACT:" + Integer.toHexString(actCrc));
			i+=2;
			frameNum++;
		}
	}
	
	/**
	 * Default Java color model stores blue value in bits 0-7, green in 8-15, red in 16-23, and the alpha value in 24-31. When alpha is zero, the pixel 
	 * is transparent and the background color is seen through. When alpha is 255, the pixel is opaque and the 
	 * actual pixel color is displayed.
	 */
	public BufferedImage getImage() {
		return image;
	}
	
	public BufferedImage getFlippedImage() {
		return createFlipped(image, imageType);
	}
	
	public BufferedImage getRotatedImage() {
		return createRotated(image, imageType);
	}
	
	public void makeGrayScaleImage() {
		width = 120;
		height = 100;
		imageType = BufferedImage.TYPE_BYTE_GRAY;
		if (type == TYPE_FULL) {
			width = 1280;
			height = 1024;
		}
		image = new BufferedImage(width, height, imageType); // vs BufferedImage.TYPE_3BYTE_BGR

		byte[] array = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		for (int i=0; i< array.length; i++) {
			array[i] = processedBytes[i];
		}

	}
	
	public void makeImage() {
		width = 1280;
		height = 1024;
		imageType = BufferedImage.TYPE_INT_RGB;

		image = new BufferedImage(width, height, imageType); // vs BufferedImage.TYPE_3BYTE_BGR
	
		int[] oriImageAsIntArray = new int[image.getWidth()*image.getHeight()];
		image.getRGB(0, 0, image.getWidth(),image.getHeight(), oriImageAsIntArray, 0, image.getWidth());

		for (int i=0; i< oriImageAsIntArray.length; i++) {
			oriImageAsIntArray[i] = processedBytes[i]&0xff; // this all goes into blue!!
		}
		image.setRGB(0,0,image.getWidth(),image.getHeight(),oriImageAsIntArray, 0, image.getWidth());

	}
	
	/**
	 * This makes a half size image with 2x2 binning.  This is a reference image to test demosaic algorithms against.
	 * The pattern of 2x2 pixels is:
	 * B G
	 * G R
	 * 
	 * Each line is half the length in the final image.
	 * 
	 */
	public void make2x2BinnedImage() {
		int processedWidth = 1280;
		width = 1280/2;
		height = 1024/2;
		imageType = BufferedImage.TYPE_INT_RGB;

		image = new BufferedImage(width, height, imageType); // vs BufferedImage.TYPE_3BYTE_BGR
	
		int[] imageAsIntArray = new int[image.getWidth()*image.getHeight()];
		image.getRGB(0, 0, image.getWidth(),image.getHeight(), imageAsIntArray, 0, image.getWidth());

		// Check size
		//if (width*height != imageAsIntArray.length)
		//System.err.println("Expected: " + width*height + " got: " + imageAsIntArray.length);
		
		for (int h=0; h < height; h++) { // line we are on
			for (int w=0; w < width; w++) { // pixel position on the line

				// Line 1 goes B G B G etc
				int b = processedBytes[2*h*processedWidth+2*w] & 0xff;
				int gb = (processedBytes[2*h*processedWidth+2*w+1] & 0xff );
				int gr = (processedBytes[(2*h+1)*processedWidth+2*w] & 0xff);				
				int g = ((gb + gr) / 2);
				int r = (processedBytes[(2*h+1)*processedWidth+2*w+1] & 0xff) ;
				
				
				imageAsIntArray[h*width+w] =   (r << 16) + (g << 8) + b;

			}
		}
		image.setRGB(0,0,image.getWidth(),image.getHeight(),imageAsIntArray, 0, image.getWidth());
		
	}
	
	private static BufferedImage createRotated(BufferedImage image, int imageType) {
        AffineTransform at = AffineTransform.getRotateInstance(
            Math.PI, image.getWidth()/2.0, image.getHeight()/2.0);
        return createTransformed(image, at, imageType);
    }
	
	private static BufferedImage createFlipped(BufferedImage image, int imageType) {
        AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getScaleInstance(1, -1));
        at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
        return createTransformed(image, at, imageType);
    }
	
	private static BufferedImage createTransformed(BufferedImage image, AffineTransform at, int imageType) {
	        BufferedImage newImage = new BufferedImage(
	            image.getWidth(), image.getHeight(),
	            imageType);
	        Graphics2D g = newImage.createGraphics();
	        g.transform(at);
	        g.drawImage(image, 0, 0, null);
	        g.dispose();
	        return newImage;
	    }
	
	/**
	 * Export the raw bytes.
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void export(String filename) throws IOException {
		FileOutputStream saveFile = null;
		try {
			saveFile = new FileOutputStream(filename);
			saveFile.write(processedBytes);
		} finally {
			try { if (saveFile != null) saveFile.close(); } catch (IOException e) { }
		}
	}
	
	public void saveAsPng(String filename) throws IOException {

		File outputfile = new File(filename);
	    ImageIO.write(image, "png", outputfile);
	}
	
	private void load(String filename) throws IOException {
		RandomAccessFile fileOnDisk = null;
		
		try {
			fileOnDisk = new RandomAccessFile(filename, "r"); // opens file 
			bytes = new byte[(int) fileOnDisk.length()];
			fileOnDisk.read(bytes);
		} finally {
			try { if (fileOnDisk != null) fileOnDisk.close(); } catch (IOException e) { }
		}
	}
}
