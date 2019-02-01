package pacSat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedDeque;
 
import javax.swing.JTextArea;

import common.Config;
import common.Log;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import pacSat.frames.PacSatFrame;

public abstract class TncDecoder implements Runnable {
    
    protected FileOutputStream byteFile;
    protected FrameDecoder decoder;
    JTextArea log;
    protected boolean running = true;
    
    String fileName = null;
    
    public static final boolean EXPEDITED = true;
    public static final boolean NOT_EXPEDITED = false;
    public static final DateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
    public static final String kissFileName = "bytes_";
    
	protected ConcurrentLinkedDeque<int[]> frameQueue = new ConcurrentLinkedDeque<int[]>();

    public TncDecoder (FrameDecoder frameDecoder, JTextArea ta, String fileName)  {
		log = ta;
		ta.append("Loading file..\n");
		this.fileName = fileName;
		decoder = frameDecoder;
		
	}
    
	public TncDecoder (FrameDecoder frameDecoder, JTextArea ta)  {
		log = ta;
		decoder = frameDecoder;
	}
	
	public void sendFrame(int[] bytes, boolean expedited) {
		if (!Config.getBoolean(Config.TX_INHIBIT))
			if (expedited)
				frameQueue.push(bytes); // add to the head
			else
				frameQueue.add(bytes); // add to the tail
	}
	
	public String getKissLogName() {
		Date today = Calendar.getInstance().getTime();
		fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = fileDateFormat.format(today);
		String name = this.kissFileName + reportDate + ".kss";
		if (!Config.get(Config.LOGFILE_DIR).equalsIgnoreCase("")) {
			name = Config.get(Config.LOGFILE_DIR) + File.separator + name;
		} 
		return name;
	}
	
	public void run() {
		Log.println("START TNC Decoder Thread");
		if (fileName != null) {
			try {
				log.append("processing file..\n");
				decoder.decode(fileName, log);
			} catch (IOException e) {
				Log.errorDialog("ERROR", "with raw byte file " + e);
			}
		}
		else 
		try {
			
			if (Config.getBoolean(Config.KISS_LOGGING))
				byteFile = new FileOutputStream(getKissLogName());
			
			process();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.errorDialog("ERROR", "with raw byte file " + e);
		} finally {
			if (byteFile != null) try { byteFile.close(); } catch (Exception e) {};
		}
		Log.println("EXIT TNC Decoder Thread");
	}
	
	abstract protected void process();
	public abstract void close();
	abstract protected void kissOn() throws SerialPortException, IOException;
	abstract protected void kissOff() throws SerialPortException, IOException;
	abstract protected void txFrame(int[] bytes) throws SerialPortException, UnknownHostException, IOException;
	
	protected void fullDuplex() {
		int[] bytes = { 0xc0, 0x05, 0x01, 0xc0 };
		sendFrame(bytes, NOT_EXPEDITED);
		log.append("TNC IN FULL DUPLEX\n");
	}
	
	protected void txDelay(int ms) {
		int[] bytes = { 0xc0, 0x01, 0x0f, 0xc0 };
		bytes[2] = (ms/10) & 0xff;
		sendFrame(bytes, NOT_EXPEDITED);
		log.append("TX DELAY: " + bytes[2] * 10 + "ms\n");
	}

	protected void txTail() {
		int[] bytes = { 0xc0, 0x04, 0x03, 0xc0 };;
		sendFrame(bytes, NOT_EXPEDITED);
		//log.append("TX TAIL: " + ms * 10 + "\n");
	}
}
