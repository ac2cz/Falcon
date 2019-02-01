package pacSat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JTextArea;

import com.g0kla.telem.server.LeaderboardTable;
import com.g0kla.telem.server.STP;

import common.Config;
import common.Log;
import jssc.SerialPortException;

public class TcpTncServer extends TncDecoder {
	int portNumber;
	int poolSize = 16;
	ServerSocket socket = null;
	LeaderboardTable leaderboard = new LeaderboardTable();

	public TcpTncServer(int port, FrameDecoder frameDecoder, JTextArea ta) {
		super(frameDecoder, ta);
		this.portNumber = port;
	}

	@Override
	protected void process() {
		ServerSocket serverSocket = null;
		boolean listening = true;
		ExecutorService pool = null;

		try {
			serverSocket = new ServerSocket(portNumber);
			pool = Executors.newFixedThreadPool(poolSize);
		} catch (IOException e) {
			Log.println("Could not listen on port: " + portNumber);
			Log.alert("FATAL: Could not listen on port: " + portNumber);
		}

		while (listening) {
			try {
				Log.println("Waiting for connection ...");
				pool.execute(new ServerProcess(serverSocket.accept()));
			}  catch (SocketTimeoutException s) {
				Log.println("Socket timed out! - trying to continue	");
				try { Thread.sleep(1000); } catch (InterruptedException e1) {	}
			} catch (IOException e) {
				e.printStackTrace(Log.getWriter());
				Log.println("Socket Error: waiting to see if we recover: " + e.getMessage());
				try { Thread.sleep(1000); } catch (InterruptedException e1) {	}
			}

		}

		try {
			serverSocket.close();
			pool.shutdown();
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}


	}

	@Override
	public void close() {
		running = false;
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
		
	}

	@Override
	protected void kissOff() throws IOException {

	}

	@Override
	protected void txFrame(int[] bytes) throws UnknownHostException, IOException {
		// no data written back
	}
	
	class ServerProcess implements Runnable {
		private Socket socket = null;
		
		ServerProcess(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			Log.println("Started Thread to handle connection from: " + socket.getInetAddress());
			InputStream in = null;
			byte[] receivedData = new byte[4096];
			
				try {
					in = socket.getInputStream();
					int len = in.read(receivedData);
					if (receivedData != null && len >0) {
//						System.err.println("Got Data: " + new String(receivedData));
						
						int[] stpData = new int[len];
						for (int j=0; j < len; j++) {
							int i = receivedData[j] & 0xff;
							decoder.decodeByte(i);
							stpData[j] = i;
						}
						STP stp = new STP(stpData);
						//Log.println(""+stp);
						leaderboard.add(stp);
						Log.println(""+leaderboard);
						if (Config.getBoolean(Config.KISS_LOGGING))
							try {
								byteFile.write(receivedData);
							} catch (IOException e) {
								Log.errorDialog("ERROR", "Could not write the KISS logfile:\n" + e.getMessage());
							}

					}
				
				} catch (IOException e1) {
					Log.println("Error in receiving bytes from TCP-port: " + e1.getMessage());
				}finally {
					try { in.close(); } catch (IOException e) {		}
					try { socket.close(); } catch (Exception e) {		}
				}
			
			
			Log.println("Stopping Server Process thread");
		}
		
	}
}
