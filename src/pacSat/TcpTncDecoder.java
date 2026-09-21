package pacSat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import common.Config;
import common.Log;
import gui.MainWindow;

public class TcpTncDecoder extends TncDecoder {
	String hostName;
	int portNumber;
	Socket socket = null;
	OutputStream out = null;
	InputStream in = null;

	/** Set true while a socket is up; cleared by the reader on EOF/error so the main loop reconnects */
	private volatile boolean connected = false;
	/** Delay between reconnect attempts. Short — direwolf's own restart/backoff does the heavy lifting */
	private static final long RECONNECT_DELAY_MS = 2000;

	public TcpTncDecoder(String hostname, int port, FrameDecoder frameDecoder, MainWindow ta) {
		super(frameDecoder, ta);
		this.hostName = hostname;
		this.portNumber = port;
	}

	@Override
	protected void process() {
		boolean everConnected = false;
		boolean initialFailureReported = false;
		boolean reconnectNoticeShown = false;

		while (running) {
			Thread rxThread = null;
			try {
				socket = new Socket(hostName, portNumber);
				out = socket.getOutputStream();
				in = socket.getInputStream();
				connected = true;
				everConnected = true;
				reconnectNoticeShown = false;   // reset so a future drop is announced again

				// One reader per connection, bound to THIS input stream. When the connection dies the
				// reader exits; the next iteration starts a fresh one for the new stream.
				PortReader portReader = new PortReader(in);
				rxThread = new Thread(portReader);
				rxThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				rxThread.setName("tcpPortReader");
				rxThread.start();

				// Re-initialise the TNC session every time we (re)connect.
				kissOn();
				sleepQuietly(2000);
				fullDuplex();
				sleepQuietly(500);
				txDelay(Config.getInt(Config.TNC_TX_DELAY));
				sleepQuietly(500);

				log.append("Decoder Ready\n");
				MainWindow.setTncConnection(true, null);

				// Run until the socket drops (connected cleared by the reader) or we're asked to stop.
				while (running && connected) {
					if (frameQueue.size() > 0)
						txFrame(frameQueue.poll());
					sleepQuietly(100);
				}
			} catch (UnknownHostException e2) {
				// A bad hostname won't fix itself by retrying — surface it once and stop the decoder.
				Log.errorDialog("ERROR", "Could not connect to the TNC over TCP with host: "
						+ hostName + " and port " + portNumber + "\n" + e2.getMessage());
				e2.printStackTrace(Log.getWriter());
				return;   // the finally below still runs
			} catch (IOException e2) {
				// Connect refused, or the socket dropped. If direwolf is (re)starting this is expected,
				// so retry quietly. Only surface a dialog for a first-attempt failure against an
				// external TNC we don't manage.
				if (!everConnected && !launchingDirewolf() && !initialFailureReported) {
					initialFailureReported = true;
					Log.errorDialog("ERROR", "IO Error connecting to the TNC over TCP with host: "
							+ hostName + " and port " + portNumber
							+ "\nUse File > Settings to configure the TNC.\n" + e2.getMessage());
					e2.printStackTrace(Log.getWriter());
				} else {
					Log.println("TNC connection down (" + e2.getMessage() + ") — will retry");
				}
			} finally {
				connected = false;
				MainWindow.setTncConnection(false, null);
				closeConnection();
				if (rxThread != null) {
					try {
						rxThread.join(500);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}

			if (!running)
				break;

			if (!reconnectNoticeShown) {
				log.append("Reconnecting to TNC...\n");   // announce once, then retry silently
				reconnectNoticeShown = true;
			}
			sleepQuietly(RECONNECT_DELAY_MS);
		}
	}

	private boolean launchingDirewolf() {
		return Config.isWindowsOs() && Config.getBoolean(Config.LAUNCH_DIREWOLF_AT_START);
	}

	private void sleepQuietly(long ms) {
		if (ms <= 0)
			return;
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void closeConnection() {
		connected = false;
		if (in != null)
			try { in.close(); } catch (Exception e) { /* nothing to do */ }
		in = null;
		if (out != null)
			try { out.close(); } catch (Exception e) { /* nothing to do */ }
		out = null;
		if (socket != null)
			try { socket.close(); } catch (Exception e) { /* nothing to do */ }
		socket = null;
	}

	@Override
	public void close() {
		running = false;
		closeConnection();   // unblocks the reader and stops the reconnect loop
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
	protected void txFrame(int[] bytes) throws IOException {
		OutputStream o = out;
		if (o == null)
			return;   // torn down under us — the process() loop will reconnect
		for (int b : bytes)
			o.write(b);
	}

	class PortReader implements Runnable {
		private final InputStream in;

		PortReader(InputStream in) {
			this.in = in;
		}

		@Override
		public void run() {
			Log.println("Starting TCP RX thread");
			byte[] receivedData = new byte[4096];
			while (running && connected) {
				try {
					int len = in.read(receivedData);
					if (len < 0) {                 // EOF — peer closed the socket
						connected = false;         // tell the main loop to reconnect
						break;
					}
					if (len > 0) {
						byte[] kissData = new byte[len];
						for (int j = 0; j < len; j++) {
							int i = receivedData[j] & 0xff;
							decoder.decodeByte(i);
							kissData[j] = receivedData[j];
						}
						if (Config.getBoolean(Config.KISS_LOGGING))
							try {
								if (byteFile == null) // kiss might have been toggled on while running
									byteFile = new FileOutputStream(getKissLogName());
								byteFile.write(kissData);
							} catch (IOException e) {
								Log.errorDialog("ERROR", "Could not write the KISS logfile:\n" + e.getMessage());
							}
					} else {
						sleepQuietly(10);
					}
				} catch (IOException e1) {
					// socket dropped or was closed under us — signal the main loop and exit
					connected = false;
					break;
				}
			}
			Log.println("Stopping TCP RX thread");
		}
	}
}