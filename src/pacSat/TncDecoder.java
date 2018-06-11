package pacSat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.JTextArea;

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
				log.append("Decoder Ready\n");
				while (running) {
					// wait for close
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		running = false;
	}
	
	private void kissOn() throws SerialPortException {
		// KISS ON
		serialPort.writeString("KISS ON");
		serialPort.writeByte((byte) 0x0d);
		serialPort.writeString("RESTART");
		serialPort.writeByte((byte) 0x0d);
		log.append("KISS ON\n");
	}
	private void kissOff() throws SerialPortException {
		// KISS OFF
		serialPort.writeByte((byte) 0xc0);
		serialPort.writeByte((byte) 0xff);
		serialPort.writeByte((byte) 0xc0);
		log.append("KISS OFF\n");
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
