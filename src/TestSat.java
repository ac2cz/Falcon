import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import ax25.Ax25Frame;
import ax25.Iframe;
import ax25.KissFrame;
import ax25.Sframe;
import ax25.Uframe;
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
	FileOutputStream byteFile;
	
	public static void main(String[] args) throws FileNotFoundException {	
		String port = null;
		if (args.length > 0)
			port = args[0];
		TestSat testSat = new TestSat(port);
	}
	
	TestSat(String com) throws FileNotFoundException {
		Config.init("TestSat.properties");
		
		Config.initLayer2();
		
		if (com != null)
			comPort = com;
		decoderThread = new Thread(decoder);
		decoderThread.start();
			serialPort = new SerialPort(comPort);
			try {
				byteFile = new FileOutputStream("testsat_bytes.kss");
				serialPort.openPort();

				serialPort.setParams(SerialPort.BAUDRATE_9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
//				serialPort.setParams(SerialPort.BAUDRATE_1200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
//				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
				serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
					
				System.out.println("Listening on Port: " + comPort);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String help = "Send a command or H for this menu:\n"
						+ "DOWNLINK\n"
						+ "OK) OK AC2CZ\n"
						+ "ERR) NO -2 AC2CZ\n"
						+ "OTHER) PB: Other cally\n"
						+ "PBD) PB: AC2CZ\\D\n"
						+ "PB ) PB: AC2CZ\n"
						+ "EMPTY ) PB: Empty\n\n"
						+ "UPLINK\n"
						+ "OPEN) Open: ABCD\n"
						+ "PG) Open: ABC AC2CZ\n"
						+ "UA) Send UA frame\n"
						+ "RR 0-7) RR Session Response\n"
						+ "REJ) REJ NR - rejects that ns and all after\n"
						+ "GO) Ready for File 846\n"
						+ "GOO) Ready for File 34e at offset 0x333\n"
						+ "ACK 0-7) UL ACK RESP\n"
						+ "NAK 0-7) UL NAK RESP 16\n"
						+ "UL_ERR 0-7) UL ERR RESP\n"
						+ "LOG) Login Response\n";
				Scanner scanner = new Scanner(System.in);
				System.out.println(help); 
				while (running) {
					// wait for close
					// send any commands

					String line = scanner.nextLine();
					String[] command = line.split("\\s+");
					if (command.length==1) {
						if (command[0].equalsIgnoreCase("H"))
							System.out.println(help); 
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
						if (command[0].equalsIgnoreCase("EMPTY"))
							sendPBEmpty();
						if (command[0].equalsIgnoreCase("OPEN"))
							sendOpen();
						if (command[0].equalsIgnoreCase("PG"))
							sendOnPG();
						if (command[0].equalsIgnoreCase("UA"))
							sendUA();
						if (command[0].equalsIgnoreCase("UA2"))
							sendUA2();
						if (command[0].equalsIgnoreCase("DM"))
							sendDM();
						if (command[0].equalsIgnoreCase("DISC"))
							sendDISC();
						if (command[0].equalsIgnoreCase("LOG"))
							sendLOGGED_IN();
						if (command[0].equalsIgnoreCase("GO"))
							sendGO();
						if (command[0].equalsIgnoreCase("GOO"))
							sendGOoffset();

						if (command[0].equalsIgnoreCase("EXIT"))
							System.exit(0);
					} else if (command.length==2) {
						if (command[0].equalsIgnoreCase("RR")) sendRR(Integer.parseInt(command[1]));
						if (command[0].equalsIgnoreCase("ACK"))
							sendAck(Integer.parseInt(command[1]));
						if (command[0].equalsIgnoreCase("NAK"))
							sendNak(Integer.parseInt(command[1]));
						if (command[0].equalsIgnoreCase("REJ"))
							sendRej(Integer.parseInt(command[1]));
					} else if (command.length==3) {
						if (command[0].equalsIgnoreCase("UL_ERR"))
							sendUlErr(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
						if (command[0].equalsIgnoreCase("RR")) sendRR(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
					} else if (command.length==4) {
						if (command[0].equalsIgnoreCase("UL_ERR"))
							sendUlErr(Integer.parseInt(command[1]), Integer.parseInt(command[2]),Integer.parseInt(command[3]));
					}
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

			} finally {
				if (byteFile != null) try { byteFile.close(); } catch (Exception e) {};
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
		
		
		private void sendPBEmpty() throws SerialPortException {
			int[] bytes = {0xC0,0x00,0xA0,0x84,0x98,0x92,0xA6,0xA8,0x00,0xA0,0x8C,0xA6,0x66,
					0x40,0x40,0x17,0x03,0xF0,0x50,0x42,0x3A,0x20,0x45,0x6D,0x70,0x74,0x79,0x2E,0x0D,0xC0};
			sendFrame(bytes);
			System.out.println("SENT Empty");
		}

		
		private void sendPBOther() throws SerialPortException {
			int[] bytes ={0xC0, 0xC0, 0x00, 0xA0, 0x84, 0x98, 0x92, 0xA6, 0xA8, 0x00, 0xA0, 0x8C, 0xA6, 0x66, 0x40, 
					0x40, 0x17, 0x03, 0xF0, 0x50, 0x42, 0x3A, 0x20, 0x4B, 0x34, 0x4B, 0x44, 0x52, 0x0D, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT Other");
		}
		
		private void sendPBD() throws SerialPortException {
			int[] bytes ={0xC0, 0x00, 0xA0, 0x84, 0x98, 0x92, 0xA6, 0xA8, 0x00, 0xA0, 0x8C, 0xA6, 0x66, 0x40, 
					0x40, 0x17, 0x03, 0xF0, 0x50, 0x42, 0x3A, 0x20, 0x41, 0x43, 0x32, 0x43, 0x5A, 0x5C, 0x44, 0x0D, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT PB AC2CZ/D");
		}
		
		private void sendPB() throws SerialPortException {
			int[] bytes ={0xC0, 0xC0, 0x00, 0xA0, 0x84, 0x98, 0x92, 0xA6, 0xA8, 0x00, 0xA0, 0x8C, 0xA6, 0x66, 
				0x40, 0x40, 0x17, 0x03, 0xF0, 0x50, 0x42, 0x3A, 0x20, 0x4B, 0x42, 0x32, 0x4D, 0x20, 0x41, 0x43, 0x32, 
				0x43, 0x5A, 0x0D, 0xC0, 0xC0};
			sendFrame(bytes);
			System.out.println("SENT PB AC2CZ");
		}
		
		// UPLINK TESTS
		private void sendOpen() throws SerialPortException {
			int[] bytes = {0xC0,0x00,0x84,0x84,0xA6,0xA8,0x82,0xA8,0x00,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x19,
					0x03,0xF0,0x4F,0x70,0x65,0x6E,0x20,0x41,0x42,0x43,0x44,0x3A,0x20,0xC0};
			sendFrame(bytes);
			System.out.println("SENT Open");
		}

		private void sendOnPG() throws SerialPortException {
			int[] bytes = {0xC0,0x00,0x84,0x84,0xA6,0xA8,0x82,0xA8,0x00,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x19,0x03,
					0xF0,0x4F,0x70,0x65,0x6E,0x20,0x41,0x42,0x20,0x44,0x3A,0x20,0x4B,0x43,0x39,0x45,0x4C,0x55,0x20,0xC0};
			sendFrame(bytes);
			System.out.println("SENT Open");
		}

		public void sendAx25Frame(Ax25Frame u) throws SerialPortException {
			if (serialPort == null) throw new SerialPortException(comPort, "Write", "Serial Port not initialized");
			KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, u.getBytes());
			int[] bytes = kss.getDataBytes();
			serialPort.writeIntArray(bytes);
		}
		
		private void sendUA() throws SerialPortException {
			Uframe u = new Uframe("PFS3-12", "AC2CZ", 1, Ax25Frame.TYPE_UA, Ax25Frame.RESPONSE);
//			int[] bytes = {0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x63,0xC0};
			sendAx25Frame(u);
			System.out.println("SENT UA: " + u.toString());
		}

		private void sendUA2() throws SerialPortException {
			Uframe u = new Uframe("PFS3-12", "N2AAA", 1, Ax25Frame.TYPE_UA, Ax25Frame.RESPONSE);
//			int[] bytes = {0xC0,0x00,0x86,0x86,0x64,0x86,0x86,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x63,0xC0};
			sendAx25Frame(u);
			System.out.println("SENT UA: " + u.toString());
		}

		
		private void sendDISC() throws SerialPortException {
			Uframe u = new Uframe("PFS3-12", "AC2CZ", 1, Ax25Frame.TYPE_U_DISCONNECT, Ax25Frame.COMMAND);
//			int[] bytes = {0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x63,0xC0};
			sendAx25Frame(u);
			System.out.println("SENT DISC: " + u.toString());
		}

		private void sendDM() throws SerialPortException {
			Uframe u = new Uframe("PFS3-12", "AC2CZ", 1, Ax25Frame.TYPE_U_DISCONNECT_MODE, Ax25Frame.RESPONSE);
//			int[] bytes = {0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x63,0xC0};
			sendAx25Frame(u);
			System.out.println("SENT DM: " + u.toString());
		}

		
		private void sendLOGGED_IN() throws SerialPortException {
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x00,0xF0,0x05,0x02,0x34,0xC4,0xB9,0x5A,0x04,0xC0};
			sendFrame(bytes);
			System.out.println("SENT LOGGED IN AC2CZ");
		}

		private void sendGO() throws SerialPortException {
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x22,0xF0,0x08,0x04,0x4E,0x03,0x00,0x00,0x00,0x00,0x00,0x00,0xC0
					};
			sendFrame(bytes);
			System.out.println("SENT GO");
		}
		private void sendGOoffset() throws SerialPortException {
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x22,0xF0,0x08,0x04,0x4E,0x03,0x00,0x00,0x33,0x03,0x00,0x00,0xC0
					};
			sendFrame(bytes);
			System.out.println("SENT GO");
		}
		private void sendRR(int n) throws SerialPortException {
			sendRR(n,0);
		}
		private void sendRR(int n, int P) throws SerialPortException {
			int SS = Ax25Frame.TYPE_S_RECEIVE_READY;
			Sframe u = new Sframe("PFS3-12", "AC2CZ", n, P, SS, Ax25Frame.RESPONSE);
			System.out.println("SENT RR: " + u.toString());
			sendAx25Frame(u);
		}
		private void sendRej(int n) throws SerialPortException {
			int SS = Ax25Frame.TYPE_S_REJECT;;
			int P=0;
			Sframe u = new Sframe("PFS3-12", "AC2CZ", n, P, SS, Ax25Frame.RESPONSE);
			System.out.println("SENT REJ: " + u.toString());
			sendAx25Frame(u);
		}

		// R0 P = 1
//		private void sendRR() throws SerialPortException {
//			
//			int[] bytes = {
//					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x11,0xC0
//					};
//			Sframe u = new Sframe("PFS3-12", "AC2CZ", 0, 1, 0);
//			System.out.println("SENT RR: " + u.toString());
//			sendAx25Frame(u);
//		
////			sendFrame(bytes);
////			System.out.println("SENT RR");
//		}

		
		private void sendCMD(int n) throws SerialPortException {
			
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0x60,0xA0,0x8C,0xA6,0x66,0x40,0x40,0xF9,0x00,0xC0
					};
			bytes[16] = n;
			sendFrame(bytes);
			System.out.println("SENT Ctrl:" + Integer.toHexString(n));
		}
		

		private void sendAck(int nr) throws SerialPortException {
			
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x84,0xF0,0x00,0x06,0xC0
					};
			// IFRAME 
			int p = 0;
			int ns = 2;
			int controlByte = (nr << 5) | (p << 4) | (ns << 1) | 0b00;
			
			bytes[16] = controlByte;
			
			sendFrame(bytes);
			System.out.println("SENT ACK " + nr);
		}

		// Send NAK 16 - ER_BODY_CHECK
		private void sendNak(int nr) throws SerialPortException {
			
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x84,0xF0,0x01,0x07,0x10, 0xC0
					};
			// IFRAME 
			int p = 0;
			int ns = 2;
			int controlByte = (nr << 5) | (p << 4) | (ns << 1) | 0b00;
			
			bytes[16] = controlByte;
			
			sendFrame(bytes);
			System.out.println("SENT NAK " + nr);
		}

		
		private void sendUlErr(int nr, int err) throws SerialPortException {
			
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x84,0xF0,0x01,0x05,0x0f,0xC0
					};
			// IFRAME 
			int p = 0;
			int ns = 1;
			int controlByte = (nr << 5) | (p << 4) | (ns << 1) | 0b00;
			
			bytes[16] = controlByte;
			bytes[20] = err;
			
			sendFrame(bytes);
			System.out.println("SENT UL ERR: " + err);
		}

		private void sendUlErr(int nr, int ns, int err) throws SerialPortException {
			
			int[] bytes = {
					0xC0,0x00,0x82,0x86,0x64,0x86,0xB4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x84,0xF0,0x01,0x05,0x0f,0xC0
					};
			// IFRAME 
			int p = 0;
			int controlByte = (nr << 5) | (p << 4) | (ns << 1) | 0b00;
			
			bytes[16] = controlByte;
			bytes[20] = err;
			
			sendFrame(bytes);
			System.out.println("SENT UL ERR: " + err);
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
}
