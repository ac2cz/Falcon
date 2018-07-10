import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import pacSat.FrameDecoder;
import common.Config;
import common.Log;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class TestSat {

	static String comPort = "COM1";
	static SerialPort serialPort;
	static boolean running = true;
	static FrameDecoder decoder = new FrameDecoder(null);
	static Thread decoderThread;
	
	public static void main(String[] args) {	
		TestSat testSat = new TestSat();
	}
	
	TestSat() {
		Config.load();
		decoderThread = new Thread(decoder);
		decoderThread.start();
			serialPort = new SerialPort(comPort);
			try {
				serialPort.openPort();

				serialPort.setParams(SerialPort.BAUDRATE_9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
				serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Scanner scanner = new Scanner(System.in);
				System.out.println("Send a command:\n"
						+ "OK) OK AC2CZ\n"
						+ "ERR) NO -2 AC2CZ\n"
						+ "other) PB: Other cally\n"
						+ "PBD) PB: AC2CZ\\D\n"
						+ "PB ) PB: AC2CZ\n"); 
				while (running) {
					// wait for close
					// send any commands
					
					String line = scanner.next();
					String[] command = line.split(" ");
					if (command[0].equalsIgnoreCase("OK"))
						sendOK();
					if (command[0].equalsIgnoreCase("ERR"))
						sendERR();
					if (command[0].equalsIgnoreCase("other"))
						sendPBOther();
					if (command[0].equalsIgnoreCase("PB"))
						sendPB();
					if (command[0].equalsIgnoreCase("PBD"))
						sendPBD();
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

	}

		public void close() {
			try {
				if (serialPort != null) {
					serialPort.closePort();
				}
			} catch (SerialPortException e1) {
				// ignore, we did our best to close this
			}
			running = false;
		}
		
		private void sendOK() throws SerialPortException {
			int[] bytes = {0xC0, 0xC0, 0x00, 0x82, 0x86, 0x64, 0x86, 0xB4, 0x40, 0x00, 0xA0, 0x8C, 0xA6, 
				0x66, 0x40, 0x40, 0x17, 0x03, 0xBB, 0x4F, 0x4B, 0x20, 0x41, 0x43, 0x32, 0x43, 0x5A, 0x0D, 0xC0, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT OK AC2CZ");
		}
		

		private void sendERR() throws SerialPortException {
			int[] bytes = {0xC0, 0xC0, 0x00, 0x82, 0x86, 0x64, 0x86, 0xB4, 0x40, 0x00, 0xA0, 0x8C, 0xA6, 
				0x66, 0x40, 0x40, 0x17, 0x03, 0xBB, 0x4E, 0x4F, 0x20, 0x2D, 0x32, 0x20, 0x41, 0x43, 0x32, 0x43, 0x5A, 0x0D, 0xC0, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT ERR AC2CZ");
		}

		
		private void sendPBOther() throws SerialPortException {
			int[] bytes ={0xC0, 0xC0, 0x00, 0xA0, 0x84, 0x98, 0x92, 0xA6, 0xA8, 0x00, 0xA0, 0x8C, 0xA6, 0x66, 0x40, 
					0x40, 0x17, 0x03, 0xF0, 0x50, 0x42, 0x3A, 0x20, 0x4B, 0x34, 0x4B, 0x44, 0x52, 0x0D, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT PB AC2CZ");
		}
		
		private void sendPBD() throws SerialPortException {
			int[] bytes ={0xC0, 0x00, 0xA0, 0x84, 0x98, 0x92, 0xA6, 0xA8, 0x00, 0xA0, 0x8C, 0xA6, 0x66, 0x40, 
					0x40, 0x17, 0x03, 0xF0, 0x50, 0x42, 0x3A, 0x20, 0x41, 0x43, 0x32, 0x43, 0x5A, 0x5C, 0x44, 0x0D, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT PB AC2CZ");
		}
		
		private void sendPB() throws SerialPortException {
			int[] bytes ={0xC0, 0xC0, 0x00, 0xA0, 0x84, 0x98, 0x92, 0xA6, 0xA8, 0x00, 0xA0, 0x8C, 0xA6, 0x66, 
				0x40, 0x40, 0x17, 0x03, 0xF0, 0x50, 0x42, 0x3A, 0x20, 0x4B, 0x42, 0x32, 0x4D, 0x20, 0x41, 0x43, 0x32, 
				0x43, 0x5A, 0x0D, 0xC0, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT PB AC2CZ");
		}
		
		public void sendFrame(int[] bytes) throws SerialPortException {
			if (serialPort == null) throw new SerialPortException(comPort, "Write", "Serial Port not initialized");
			serialPort.writeIntArray(bytes);
			//serialPort.writeByte((byte) 0x0d); // Need just CR to terminate or not recognized
		}

		private class PortReader implements SerialPortEventListener {

			@Override
			public void serialEvent(SerialPortEvent event) {
				if(event.isRXCHAR() && event.getEventValue() > 0) {
					try {
						byte[] receivedData = serialPort.readBytes(event.getEventValue());
						for (byte b : receivedData) {
							int i = b & 0xff;
							char ch = (char)b;
							decoder.decodeByte(i);
						}
						

					}
					catch (SerialPortException ex) {
						System.out.println("Error in receiving string from COM-port: " + ex);
					}
				}
			}

		}
}
