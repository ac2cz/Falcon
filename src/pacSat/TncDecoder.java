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
				//fullDuplex();
				log.append("Decoder Ready\n");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Test frame - turns TNX TO TX
//				int[] by = {0xc0, 0x00,0xA2,0xA6,0xA8,0x40,0x40,0x40,0x02,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x17,0x03,0xBD,0x20,
//						55,0x08,0x00,0x00,0x00,0x00,0x00,0x00,0x88,0x6E,0x1C,0x5B,0x17,0x8F,0x1C,0x5B,0xAA,0x55,0x01,0x00,0x04,
//						55,0x08,0x00,0x00,0x02,0x00,0x08,0x77,0x65,0x30,0x36,0x30,0x39,0x30,0x39,0x03,0x00,0x03,0x20,0x20,0x20,
//						04,0x00,0x04,0x3F,0x13,0x00,0x00,0x05,0x00,0x04,0x25,0x4E,0x1C,0x5B,0x06,0x00,0x04,0x88,0x6E,0x1C,0x5B,
//						12,0x00,0x04,0x88,0x6E,0x1C,0x5B,0x07,0x00,0x01,0x00,0x08,0x00,0x01,0x03,0x09,0x00,0x02,0x2B,0xEE,0x0A,
//						00,0x02,0xCD,0x09,0x0B,0x00,0x02,0x50,0x00,0x00,0x00,0x00,0x51,0x61,0xc0};
//				sendFrame(by);
				while (running) {
					// wait for close
					// send any commands
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
		int[] bytes = { 0x05, 0xff };
		sendFrame(bytes);
		log.append("TNC IN FULL DUPLEX\n");
	}
	private void kissOff() throws SerialPortException {
		int[] bytes = { 0xff };
		sendFrame(bytes);
		log.append("KISS OFF\n");
	}
	
	public void sendFrame(int[] bytes) throws SerialPortException {
		if (serialPort == null) throw new SerialPortException(comPort, "Write", "Serial Port not initialized");
		serialPort.writeIntArray(bytes);
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
