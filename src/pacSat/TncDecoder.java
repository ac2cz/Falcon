package pacSat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class TncDecoder implements Runnable {
    SerialPort serialPort;
    FileOutputStream byteFile;
    FrameDecoder decoder;
    JTextArea log;
    boolean running = true;
    String comPort = "COM1";
    int baudRate = SerialPort.BAUDRATE_9600;
    int dataBits = SerialPort.DATABITS_8;
    int stopBits = SerialPort.STOPBITS_1;
    int parity = SerialPort.PARITY_NONE;
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
		comPort = "FILE";
	}
    
	public TncDecoder (String comPort, int baudRate, int dataBits, int stopBits, int parity, FrameDecoder frameDecoder, JTextArea ta)  {
		this.comPort = comPort;
		this.baudRate = baudRate;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		this.parity = parity;
		log = ta;
		decoder = frameDecoder;
	}
	
	public void sendFrame(int[] bytes, boolean expedited) {
		if (expedited)
			frameQueue.push(bytes); // add to the head
		else
			frameQueue.add(bytes); // add to the tail
	}
	
	public void DEPRECIATED_sendCommand(String command) {
		try {
			serialPort.writeString(command);
			serialPort.writeByte((byte) 0x0d); // Need just CR to terminate or not recognized
		} catch (SerialPortException e) {
			System.out.println("ERROR: writing string to port: " + e);
		}
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
		else if (comPort.equals(Config.NO_COM_PORT)) {
			Log.infoDialog("PORT NOT SETUP", "Configure the connection to the TNC on the Settings Tab.\n"
					+ "No communication is possible without this.");
		} else
		try {
			
			if (Config.getBoolean(Config.KISS_LOGGING))
				byteFile = new FileOutputStream(getKissLogName());
			
			serialPort = new SerialPort(comPort);
			try {
				serialPort.openPort();
				serialPort.setParams(baudRate,dataBits,stopBits,parity);
				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
//				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
				serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
				
				kissOn();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				fullDuplex();
				try {Thread.sleep(500);	} catch (InterruptedException e1) {}
				txDelay(Config.getInt(Config.TNC_TX_DELAY));
				try {Thread.sleep(500);	} catch (InterruptedException e1) {}
				//txTail();
				log.append("Decoder Ready\n");
				while (running) {
					// wait for close
					// send any commands
					if (frameQueue.size() > 0)
						txFrame(frameQueue.poll());
					try {Thread.sleep(100);} catch (InterruptedException e) {}
				}
			}
			catch (SerialPortException ex) {
				Log.errorDialog("ERROR", "writing to serial port: " + ex);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.errorDialog("ERROR", "with raw byte file " + e);
		} finally {
			if (byteFile != null) try { byteFile.close(); } catch (Exception e) {};
		}
		Log.println("EXIT TNC Decoder Thread");
	}
	
	public void close() {
		try {
			if (serialPort != null) {
				kissOff();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				serialPort.closePort();
			}
		} catch (SerialPortException e1) {
			// ignore, we did our best to close this
		}
		running = false;
	}
	
	private void kissOn() throws SerialPortException {
		serialPort.writeString("KISS ON");
		serialPort.writeByte((byte) 0x0d);
		serialPort.writeString("RESTART");
		serialPort.writeByte((byte) 0x0d);
		log.append("KISS ON\n");
	}
	private void fullDuplex() throws SerialPortException {
		int[] bytes = { 0xc0, 0x05, 0x01, 0xc0 };
		sendFrame(bytes, NOT_EXPEDITED);
		log.append("TNC IN FULL DUPLEX\n");
	}
	
	private void txDelay(int ms) throws SerialPortException {
		int[] bytes = { 0xc0, 0x01, 0x0f, 0xc0 };
		bytes[2] = (ms/10) & 0xff;
		sendFrame(bytes, NOT_EXPEDITED);
		log.append("TX DELAY: " + bytes[2] * 10 + "ms\n");
	}

	private void txTail() throws SerialPortException {
		int[] bytes = { 0xc0, 0x04, 0x03, 0xc0 };;
		sendFrame(bytes, NOT_EXPEDITED);
		//log.append("TX TAIL: " + ms * 10 + "\n");
	}

	private void kissOff() throws SerialPortException {
		int[] bytes = { 0xc0,0xff,0xc0 };
		sendFrame(bytes, NOT_EXPEDITED);
		log.append("KISS OFF\n");
	}
	
	private void txFrame(int[] bytes) throws SerialPortException {
		if (serialPort == null) throw new SerialPortException(comPort, "Write", "Serial Port not initialized");
		serialPort.writeIntArray(bytes);
		//serialPort.writeByte((byte) 0x0d); // Need just CR to terminate or not recognized
		//try {Thread.sleep(100);} catch (InterruptedException e) {}
		if (Config.getBoolean(Config.DEBUG_TX))
			log.append("Tx "+bytes.length+" bytes\n");
	}

	private class PortReader implements SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent event) {
			if(event.isRXCHAR() && event.getEventValue() > 0) {
				try {
					byte[] receivedData = serialPort.readBytes(event.getEventValue());
					for (byte b : receivedData) {
						int i = b & 0xff;
						decoder.decodeByte(i);
					}
					if (Config.getBoolean(Config.KISS_LOGGING))
						try {
							byteFile.write(receivedData);
						} catch (IOException e) {
							Log.errorDialog("ERROR", "Could not write the KISS logfile:\n" + e.getMessage());
						}

				}
				catch (SerialPortException ex) {
					System.out.println("Error in receiving string from COM-port: " + ex);
				}
			}
		}

	}
	
	public static String[] portNames;
	public static String[] getSerialPorts() {
		// getting serial ports list into the array
		portNames = SerialPortList.getPortNames();	

		if (portNames.length == 0) {
			Log.errorDialog("FATAL", "There are no serial-ports.  You need to configure one, even if its virtual.");
			return null;
		}
		return portNames;
//		for (int i = 0; i < portNames.length; i++){
//			System.out.println(portNames[i]);
//		}
	}
	
	public static String[] getAvailableBaudRates() {
		String[] rates = new String[6];
		rates[0] = ""+SerialPort.BAUDRATE_1200;
		rates[1] = ""+SerialPort.BAUDRATE_4800;
		rates[2] = ""+SerialPort.BAUDRATE_9600;
		rates[3] = ""+SerialPort.BAUDRATE_19200;
		rates[4] = ""+SerialPort.BAUDRATE_38400;
		rates[5] = ""+SerialPort.BAUDRATE_57600;
		return rates;
	}

}
