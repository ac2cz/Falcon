package pacSat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JTextArea;

import common.Config;
import common.Log;
import jssc.SerialPortException;

public class TcpTncDecoder extends TncDecoder {
	String hostName;
	int portNumber;
	Socket socket = null;
	OutputStream out = null;

	public TcpTncDecoder(String hostname, int port, FrameDecoder frameDecoder, JTextArea ta) {
		super(frameDecoder, ta);
		this.hostName = hostname;
		this.portNumber = port;
	}

	@Override
	protected void process() {

		try {
			socket = new Socket(hostName, portNumber);
			out = socket.getOutputStream();

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
//		out.write("KISS ON");
//		out.write((byte) 0x0d);
//		out.writeString("RESTART");
//		out.write((byte) 0x0d);
		log.append("KISS ON\n");
		
	}

	@Override
	protected void kissOff() throws IOException {
	//	int[] bytes = { 0xc0,0xff,0xc0 };
	//	sendFrame(bytes, NOT_EXPEDITED);
		log.append("KISS OFF\n");
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

}
