package pacSat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JTextArea;

import common.Config;
import common.Log;
import gui.MainWindow;
import jssc.SerialPortException;

public class TcpTncDecoder extends TncDecoder {
	String hostName;
	int portNumber;
	Socket socket = null;
	OutputStream out = null;
	InputStream in = null;

	public TcpTncDecoder(String hostname, int port, FrameDecoder frameDecoder, MainWindow ta) {
		super(frameDecoder, ta);
		this.hostName = hostname;
		this.portNumber = port;
	}

	@Override
	protected void process() {

		try {
			socket = new Socket(hostName, portNumber);
			out = socket.getOutputStream();
			in = socket.getInputStream();
			PortReader portReader = new PortReader();
			Thread rxThread = new Thread(portReader);
			rxThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
			rxThread.setName("tcpPortReader");
			rxThread.start();

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
		} catch (UnknownHostException e2) {
			Log.errorDialog("ERROR", "Could not connect to the TNC over TCP with host: " + hostName + " and port " + portNumber + "\n" + e2.getMessage());
			e2.printStackTrace(Log.getWriter());
		} catch (IOException e2) {
			Log.errorDialog("ERROR", "IO Error connecting to the TNC over TCP with host: " + hostName + " and port " + portNumber + "\n" + e2.getMessage());
			e2.printStackTrace(Log.getWriter());
		}
	}

	@Override
	public void close() {
		running = false;
		if (out != null)
			try {
				out.close();
			} catch (Exception e) {
				// Nothing to do
			}
		out = null;
		if (socket != null)
			try {
				socket.close();
			} catch (Exception e) {
				// Nothing to do
			}
		socket = null;
	}

	@Override
	protected void kissOn() throws IOException {
		log.append("KISS is assumed ON\n");
		
	}

	@Override
	protected void kissOff() throws IOException {
		// Nothing to do in TCP mode
	}

	@Override
	protected void txFrame(int[] bytes) throws UnknownHostException, IOException {
		if (socket == null) {
			socket = new Socket(hostName, portNumber);
			out = socket.getOutputStream();
		}

		for (int b : bytes)
			out.write(b);
	}
	
	class PortReader implements Runnable {

		@Override
		public void run() {
			Log.println("Starting TCP RX thread");
			byte[] receivedData = new byte[4096];
			while (running) {
				try {
					int len = in.read(receivedData);
					if (receivedData != null && len > 0) {
//						System.err.println("Got Data: " + new String(receivedData));
						byte[] kissData = new byte[len];
						for (int j=0; j < len; j++) {
							int i = receivedData[j] & 0xff;
							decoder.decodeByte(i);
							kissData[j] = receivedData[j];
						}
						if (Config.getBoolean(Config.KISS_LOGGING))
							try {
								if (byteFile == null) // kiss might have been toggled on while we are already running
									byteFile = new FileOutputStream(getKissLogName());
								byteFile.write(kissData);
							} catch (IOException e) {
								Log.errorDialog("ERROR", "Could not write the KISS logfile:\n" + e.getMessage());
							}

					} else {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (IOException e1) {
					Log.println("Error in receiving bytes from TCP-port: " + e1.getMessage());
				}
			}
			
			Log.println("Stopping TCP RX thread");
		}
	}
}
