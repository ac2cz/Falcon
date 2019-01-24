package pacSatServer;

import java.io.IOException;
import java.net.UnknownHostException;
import com.g0kla.telem.server.STPQueue;
import com.g0kla.telem.server.TlmServer;

import common.Config;
import common.Log;;
/**
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2019 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Class to hold a queue of frames after they are received by the ground station and before they have been sent to the 
 * amsat server.  This class runs as a back ground thread.  When frames are added to it they are stored.  They are then 
 * sent in the background to the server.
 * 
 * If the passManager is active (findSignal is true) then we are attempting to monitor the satellites and measure TCA.
 * In that case, we check if a pass is active and hold on to the last record until the pass is done.  We then append
 * TCA is measured.
 *
 */
public class KissStpQueue extends STPQueue {
	public static String KISS_STP_FILE = "kissStp.log";
	
	static final int serverTxPeriod = 5; // number of seconds to pause between sending frames
	static final int serverRetryWaitPeriod = 12*5; // multiples of the Tx Period to wait if we failed

	public KissStpQueue(String file, String server, int port) throws IOException {
		super(file, server, port);
	}

	@Override
	public void run() {
		running = true;
		boolean success = true;
		int retryStep = 0;
		while(running) {

			try {
				Thread.sleep(1000 * serverTxPeriod); // refresh data periodically
			} catch (InterruptedException e) {
				e.printStackTrace(Log.getWriter());
			} 	

			//			MainWindow.setTotalQueued(this.getSize());
			if (Config.getBoolean(Config.SEND_TO_SERVER)) {
				if (!success) {
					// We failed the last time we tried to connect, so wait until we retry
					//System.out.print(".");
					if (retryStep++ > serverRetryWaitPeriod) {
						success = true;
						retryStep = 0;
					}
				}
				// try to send these frames to the server
				// We attempt to send the first one, if unsuccessful, we try the backup server.  If still unsuccessful we drop out
				// and try next time, unless sendToBoth is set, in which case we just send to both servers
				while (getSize() > 0 && success) {
					// Make sure we have the latest connection settings in case the user updated in settings
					server.setHostName(Config.get(Config.TELEM_SERVER));
					server.setPort(Config.getInt(Config.TELEM_SERVER_PORT));
					Log.println("Trying Primary Server: " + server.getHostname() + ":" + server.getPort());
					try {
						success = sendFrame();
					} catch (UnknownHostException e) {
						Log.println("Could not connect to primary server: "+ e.getMessage());
						//e.printStackTrace(Log.getWriter());
						success = false;
					} catch (IOException e) {
						Log.println(e.getMessage());
						success = false;
					}
				}
				try {
					Thread.sleep(100); // pause so that the server can keep up
				} catch (InterruptedException e) {
					Log.println("ERROR: server DUV frame queue thread interrupted");
					e.printStackTrace(Log.getWriter());
				} 	
			}
		}
		Log.println("Server Queue thread ended");

	}
}

