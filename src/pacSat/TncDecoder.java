package pacSat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JTextArea;

import common.Config;
import common.Log;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class TncDecoder implements Runnable{
    static SerialPort serialPort;
    static FileOutputStream byteFile;
    static FrameDecoder decoder;
    JTextArea log;
    boolean running = true;
    String comPort = "COM1";
    String fileName = null;
	
    public TncDecoder (JTextArea ta, String fileName)  {
		log = ta;
		ta.append("Loading file..\n");
		this.fileName = fileName;
		decoder = new FrameDecoder();
		comPort = "FILE";
	}
    
	public TncDecoder (String comPort, JTextArea ta)  {
		this.comPort = comPort;
		log = ta;
		decoder = new FrameDecoder();
	}
	
	public void sendCommand(String command) {
		try {
			serialPort.writeString(command);
			serialPort.writeByte((byte) 0x0d); // Need just CR to terminate or not recognized
		} catch (SerialPortException e) {
			System.out.println("ERROR: writing string to port: " + e);
		}
	}
	
	
	public void run() {
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
			byteFile = new FileOutputStream("bytes.raw");
			serialPort = new SerialPort(comPort);
			try {
				serialPort.openPort();

				serialPort.setParams(SerialPort.BAUDRATE_9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
				serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
				
				kissOn();
				fullDuplex();
				txDelay(15);
				txTail();
				log.append("Decoder Ready\n");
				while (running) {
					// wait for close
					// send any commands
					try {Thread.sleep(100);} catch (InterruptedException e) {}
				}
			}
			catch (SerialPortException ex) {
				Log.errorDialog("ERROR", "writing string to port: " + ex);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.errorDialog("ERROR", "with raw byte file " + e);
		} finally {
			if (byteFile != null) try { byteFile.close(); } catch (Exception e) {};
		}
		
	}
	
	public void close() {
		try {
			if (serialPort != null) {
				kissOff();
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
		sendFrame(bytes);
		log.append("TNC IN FULL DUPLEX\n");
	}
	
	private void txDelay(int ms) throws SerialPortException {
		int[] bytes = { 0xc0, 0x01, 0x0f, 0xc0 };
		bytes[2] = ms;
		sendFrame(bytes);
		log.append("TX DELAY: " + ms * 10 + "\n");
	}

	private void txTail() throws SerialPortException {
		int[] bytes = { 0xc0, 0x04, 0x03, 0xc0 };;
		sendFrame(bytes);
		//log.append("TX TAIL: " + ms * 10 + "\n");
	}

	private void kissOff() throws SerialPortException {
		int[] bytes = { 0xc0,0xff,0xc0 };
		sendFrame(bytes);
		log.append("KISS OFF\n");
	}
	
	public void sendFrame(int[] bytes) throws SerialPortException {
		if (serialPort == null) throw new SerialPortException(comPort, "Write", "Serial Port not initialized");
		serialPort.writeIntArray(bytes);
		//serialPort.writeByte((byte) 0x0d); // Need just CR to terminate or not recognized
		//try {Thread.sleep(100);} catch (InterruptedException e) {}
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
						String response = decoder.decodeByte(i);
						if (response != "")
							log.append(response + "\n");
						char ch = (char)b;
						//System.out.print(ch);
					}
					try {
						byteFile.write(receivedData);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				catch (SerialPortException ex) {
					System.out.println("Error in receiving string from COM-port: " + ex);
				}
			}
		}

	}
	
	private void getSerialPorts() {
		// getting serial ports list into the array
		String[] portNames = SerialPortList.getPortNames();

		if (portNames.length == 0) {
			System.out.println("There are no serial-ports :( You can use an emulator, such ad VSPE, to create a virtual serial port.");
			System.out.println("Press Enter to exit...");
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		for (int i = 0; i < portNames.length; i++){
			System.out.println(portNames[i]);
		}
	}

}
