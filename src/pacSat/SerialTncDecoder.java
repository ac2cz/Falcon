package pacSat;

import java.io.IOException;

import javax.swing.JTextArea;

import common.Config;
import common.Log;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class SerialTncDecoder extends TncDecoder {
	SerialPort serialPort;
	String comPort = "COM1";
    int baudRate = SerialPort.BAUDRATE_9600;
    int dataBits = SerialPort.DATABITS_8;
    int stopBits = SerialPort.STOPBITS_1;
    int parity = SerialPort.PARITY_NONE;
    
	public SerialTncDecoder(FrameDecoder frameDecoder, JTextArea ta, String fileName) {
		super(frameDecoder, ta, fileName);
		comPort = "FILE";
	}
	
	public SerialTncDecoder(String comPort, int baudRate, int dataBits, int stopBits, int parity,
			FrameDecoder frameDecoder, JTextArea ta) {
		super(frameDecoder, ta);
		this.comPort = comPort;
		this.baudRate = baudRate;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		this.parity = parity;
	}
	
	protected void process() {
		if (comPort.equals(Config.NO_COM_PORT)) {
			return;
		}
		serialPort = new SerialPort(comPort);
		try {
			serialPort.openPort();
			serialPort.setParams(baudRate,dataBits,stopBits,parity);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
//			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
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
	}
	
	protected void kissOn() throws SerialPortException {
			serialPort.writeString("KISS ON");
			serialPort.writeByte((byte) 0x0d);
			serialPort.writeString("RESTART");
			serialPort.writeByte((byte) 0x0d);
			log.append("KISS ON\n");
	}
	
	protected void kissOff() throws SerialPortException {
		int[] bytes = { 0xc0,0xff,0xc0 };
		sendFrame(bytes, NOT_EXPEDITED);
		log.append("KISS OFF\n");
	}
	
	protected void txFrame(int[] bytes) throws SerialPortException {
		if (serialPort == null) throw new SerialPortException(comPort, "Write", "Serial Port not initialized");
		serialPort.writeIntArray(bytes);
		//serialPort.writeByte((byte) 0x0d); // Need just CR to terminate or not recognized
		//try {Thread.sleep(100);} catch (InterruptedException e) {}
		if (Config.getBoolean(Config.DEBUG_TX))
			log.append("Tx "+bytes.length+" bytes\n");
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
					Log.println("Error in receiving string from COM-port: " + ex);
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

	public static String[] getAvailableStopBits() {
		String[] rates = new String[3];
		rates[0] = ""+SerialPort.STOPBITS_1;
		rates[1] = ""+SerialPort.STOPBITS_2;
		rates[2] = ""+SerialPort.STOPBITS_1_5;
		return rates;
	}

	public static String[] getAvailableDataBits() {
		String[] rates = new String[4];
		rates[0] = ""+SerialPort.DATABITS_5;
		rates[1] = ""+SerialPort.DATABITS_6;
		rates[2] = ""+SerialPort.DATABITS_7;
		rates[3] = ""+SerialPort.DATABITS_8;
		return rates;
	}

	public static String[] getAvailableParities() {
		String[] rates = new String[4];
		rates[0] = "NONE";//+SerialPort.PARITY_NONE;
		rates[1] = "ODD";//+SerialPort.PARITY_ODD;
		rates[2] = "EVEN";//+SerialPort.PARITY_EVEN;
		rates[3] = "MARK";//+SerialPort.PARITY_MARK;
		return rates;
	}
}
