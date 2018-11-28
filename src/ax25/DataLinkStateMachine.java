package ax25;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTextArea;

import common.Config;
import common.Log;
import common.Spacecraft;
import pacSat.TncDecoder;
import pacSat.frames.FTL0Frame;
import pacSat.frames.FrameException;
import pacSat.frames.KissFrame;
import pacSat.frames.ULCmdFrame;

public class DataLinkStateMachine implements Runnable {

	public static final int DISCONNECTED = 0;
	public static final int AWAITING_CONNECTION = 1;
	public static final int AWAITING_RELEASE = 2;
	public static final int CONNECTED = 3;
	public static final int TIMER_RECOVERY = 4;
	 
	/*
	 * Error Codes
	 */ 
	public static final String ERROR_A = "F=1 received but P=1 not outstanding.";
	public static final String ERROR_B = "Unexpected DM with F=1 in states 3, 4 or 5.";
	public static final String ERROR_C = "Unexpected UA in states 3, 4 or 5.";
	public static final String ERROR_D = "UA received without F=1 when SABM or DISC was sent P=1.";
	public static final String ERROR_E = "DM received in states 3, 4 or 5.";
	public static final String ERROR_F = "Data link reset; i.e., SABM received in state 3, 4 or 5.";
	public static final String ERROR_I = "N2 timeouts: unacknowledged data.";
	public static final String ERROR_J = "N(r) sequence ERROR_.";
	public static final String ERROR_L = "Control field invalid or not implemented.";
	public static final String ERROR_M = "Information field was received in a U or S-type frame.";
	public static final String ERROR_N = "Length of frame incorrect for frame type.";
	public static final String ERROR_O = "I frame exceeded maximum allowed length.";
	public static final String ERROR_P = "N(s) out of the window.";
	public static final String ERROR_Q = "UI response received, or UI command with P=1 received.";
	public static final String ERROR_R = "UI frame exceeded maximum allowed length.";
	public static final String ERROR_S = "I response received.";
	public static final String ERROR_T = "N2 timeouts: no response to enquiry.";
	public static final String ERROR_U = "N2 timeouts: extended peer busy condition.";
	public static final String ERROR_V = "No DL machines available to establish connection.";
	
	public static final int LOOP_TIME = 1; // length of time in ms to process respones
	
	int SRT = 3000; // smoothed round trip time
	int T1V = SRT; // Next value for T1
	
	public static final int TIMER_T1 = 3000; // milli seconds for T1 - Outstanding I frame or P bit
	int t1_timer = 0; // 0 is inactive, any other value is incrementing
	public static final int TIMER_T3 = 10000; // milli seconds for T3 - Idle supervision, keep alive
	int t3_timer = 0; // 0 is inactive, any other value is incrementing
	public static final int RETRIES_N2 = 10; // max retries
	
	// State variables
	int VS = 0; // The next sequential number to be assigned to the next I Frame
	int VA = 0; //
	int VR = 0; // The sequence number of the next expected received I frame.  Updated when an I frame received whoes send sequence number equals VR
	int RC = 0; // retry count
	boolean peerReceiverBusy = false;
	boolean rejectException = false;
	int sRejException = 0;
	boolean ackPending = false;
	boolean sRejEnabled = false;
	
	Ax25Frame lastCommand = null;
	
	public static final String[] states = {
			"Disc",
			"Wait Conn",
			"Wait Rel",
			"Connected",
			"Timer Rec"
	};
	protected int state;
	protected TncDecoder tncDecoder;
	protected ConcurrentLinkedQueue<Ax25Primitive> frameEventQueue;
	protected ConcurrentLinkedDeque<Iframe> iFrameQueue;
	protected boolean running = true;
	JTextArea ta;
	Spacecraft spacecraft;


	public DataLinkStateMachine(Spacecraft sat) {
		this.spacecraft = sat;
		iFrameQueue = new ConcurrentLinkedDeque<Iframe>();
		frameEventQueue = new ConcurrentLinkedQueue<Ax25Primitive>();
		state = DISCONNECTED;
	}
	
	public boolean isConnected() {
		if (state == CONNECTED || state == TIMER_RECOVERY || state == AWAITING_RELEASE) 
			return true;
		return false;
	}
	
	public void processEvent(Ax25Primitive frame) {
		Log.println("Adding LAYER2 DATA Event: " + frame.toString());
		frameEventQueue.add(frame);
	}
	
	protected void nextState(Ax25Primitive prim) {

		switch (state) {
		case DISCONNECTED:
			stateDisc(prim);
			break;
		case AWAITING_CONNECTION:
			stateWaitConn(prim);
			break;
		case AWAITING_RELEASE:
			stateWaitRelease(prim);
			break;
		case CONNECTED:
			stateConnected(prim);
			break;
		case TIMER_RECOVERY:
			stateTimerRec(prim);
			break;
		default:
			break;
		}

	}
	
	private void sendFrame(Ax25Frame frame) {
		KissFrame kss = new KissFrame(0, KissFrame.DATA_FRAME, frame.getBytes());
		if (ta != null)
			if (Config.getBoolean(Config.DEBUG_LAYER2))
				ta.append("LAYER2 SENDING: " + frame.toString() + " ... \n");
		if (tncDecoder != null) {
			lastCommand = frame;
			tncDecoder.sendFrame(kss.getDataBytes());
		} else {
			Log.infoDialog("NO TNC", "Nothing was transmitted as no TNC is connected\n ");
		}
	}
	
	/**
	 * We are disconnected, so ignore all received Layer 2 data.  We don't accept incoming requests
	 * @param frame
	 */
	private void stateDisc(Ax25Primitive prim) {
		if (prim instanceof Ax25Request) {
			Ax25Request req = (Ax25Request) prim;
			switch (req.type) {
			case Ax25Request.DL_CONNECT:
				// SAT initial Default
				// TIV = 2* SAT
				establishDataLink(req.fromCall, req.toCall);
				state = AWAITING_CONNECTION;
				break;
			default:
				break;
			}
		} else {
			Ax25Frame frame = (Ax25Frame) prim;
			switch (frame.type) {
			case Ax25Frame.TYPE_UA:
				// DL ERROR INDICATION D C
				state = DISCONNECTED;
				break;
			default:
				break;
			}
		}

	}

	/**
	 * We have sent a connection request, so we are waiting for a UA, DM or a Timeout
	 * @param frame
	 */
	private void stateWaitConn(Ax25Primitive prim) {
		if (prim instanceof Ax25Request) {
			Ax25Request req = (Ax25Request) prim;
			switch (req.type) {
			case Ax25Request.DL_CONNECT:
				iFrameQueue = new ConcurrentLinkedDeque<Iframe>(); // discard the queue
				state = AWAITING_CONNECTION;
				break;
			case Ax25Request.DL_DISCONNECT:
				// requeue
				state = AWAITING_CONNECTION;
				break;
			case Ax25Request.DL_DATA:
				iFrameQueue.add(((Ax25Request)prim).iFrame);
				break;
			case Ax25Request.TIMER_T1_EXPIRY:
				if (RC == RETRIES_N2) {
					// Max retries reached, disconnect
					iFrameQueue = new ConcurrentLinkedDeque<Iframe>(); // discard the queue
					// DL ERROR INDICATION G
					ta.append("LAYER 2 ERROR: G");
					state = DISCONNECTED;
					RC=0;
					t1_timer = 0;
				} else {
					RC++;
					t1_timer = 1; // start the timer
					sendFrame(lastCommand);
					state = AWAITING_CONNECTION;
				}
				break;
			default:
				break;
			}
		} else {
			Ax25Frame frame = (Ax25Frame) prim;
			switch (frame.type) {
			case Ax25Frame.TYPE_U_DISCONNECT: // DISC

				// Disconnect and stop any timer
				// TODO We should really send a DM with the POLL bit set to confirm, but not sure we get this from FS-3
				state = DISCONNECTED;
				t1_timer = 0;
				RC = 0;

				break;
			case Ax25Frame.TYPE_U_DISCONNECT_MODE: // DM
				if (frame.PF == 1) {
					// Disconnect and stop any timer
					state = DISCONNECTED;
					t1_timer = 0;
					RC = 0;
				}
				break;
			case Ax25Frame.TYPE_UA:
				if (frame.PF == 1) {
					VS = 0;
					VA = 0;
					VR = 0;
					t1_timer = 0; // stop the timer
					t3_timer = 0; // stop the timer
					RC = 0;
					state = CONNECTED;					
				} else {
					// DL ERROR INDICATION D
					ta.append("LAYER 2 ERROR: D");
					state = AWAITING_CONNECTION;
				}
				break;

			default:
				break;
			}
		}
		
	}

	/**
	 * This state called if we have sent a disconnect request and we are waiting for confirmation that the other
	 * station has disconnected.
	 * @param prim
	 */
	private void stateWaitRelease(Ax25Primitive prim) {
		if (prim instanceof Ax25Request) {
			Ax25Request req = (Ax25Request) prim;
			switch (req.type) {
			case Ax25Request.DL_DISCONNECT:
				int P = 1;  // it says DM Expidited, does that mean 0 or 1?
				Uframe frame = new Uframe(req.fromCall, req.toCall, 1, Ax25Frame.TYPE_U_DISCONNECT_MODE);
				lastCommand = frame;
				t1_timer = 0;
				RC = 0;
				sendFrame(frame);
				state = AWAITING_RELEASE;
				break;
			case Ax25Request.TIMER_T1_EXPIRY:
				if (RC >= RETRIES_N2) {
					state = DISCONNECTED;
				} else {
					RC++;
					t1_timer = 1; // start the timer
					sendFrame(lastCommand); // our disconnect request
				}
				break;
			default:
				break;
			}
		} else {
			Ax25Frame frame = (Ax25Frame) prim;
			switch (frame.type) {
			case Ax25Frame.TYPE_U_DISCONNECT_MODE: // DM
				if (frame.PF == 1) {
					// Disconnect and stop any timer
					state = DISCONNECTED;
					t1_timer = 0;
					RC = 0;
				} else {
					state = AWAITING_RELEASE;
				}
				break;
			case Ax25Frame.TYPE_UA:
				if (frame.PF == 1) {
					t1_timer = 0;
					RC = 0;
					state = DISCONNECTED;
				} else {
					// DL ERROR INDICATION - D
					ta.append("LAYER 2 ERROR: D\n");
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * We are connected.  If we receive a DL_DATA request then queue the i frame
	 * If we receive an I frame then we transmit it
	 * We handle all the acks and RRs
	 * Or any disconnects
	 * @param prim
	 */
	private void stateConnected(Ax25Primitive prim) {
		if (prim instanceof Ax25Request) {
			Ax25Request req = (Ax25Request) prim;
			switch (req.type) {
			case Ax25Request.DL_DISCONNECT:
				int P = 1;
				Uframe frame = new Uframe(req.fromCall, req.toCall, 1, Ax25Frame.TYPE_U_DISCONNECT);
				lastCommand = frame;
				t1_timer = 1;
				t3_timer = 0;
				RC = 0;
				sendFrame(frame);
				state = AWAITING_RELEASE;
				break;
			case Ax25Request.TIMER_T1_EXPIRY:
				RC = 1;
				transmitInquiry(lastCommand.fromCallsign, lastCommand.toCallsign);
				state = TIMER_RECOVERY;
				break;
			case Ax25Request.TIMER_T3_EXPIRY:
				RC = 0;
				transmitInquiry(lastCommand.fromCallsign, lastCommand.toCallsign);
				state = TIMER_RECOVERY;
				break;
			case Ax25Request.DL_DATA:
				iFrameQueue.add(((Ax25Request)prim).iFrame);
			case Ax25Request.DL_POP_IFRAME:
				// Transmit this I FRAME
				if (!peerReceiverBusy)
					if (!(VS == VA + 7)) {
						Iframe iFrame = iFrameQueue.pop(); // remove the head of the queue
						P = 0;
						iFrame.setControlByte(VR, VS, P);
						VS++;
						if (VS > 7) VS = 0;
						ackPending = false;
						lastCommand = iFrame;
						sendFrame(iFrame);
						if (!(t1_timer > 0)) {
							// Start the T1 Timer and stop T3
							RC=0;
							t1_timer = 1;
							t3_timer = 0;
						}
						state = CONNECTED;
					} else {
						// nothing to do, we leave the iFrame on the queue
					}
				break;
			default:
				break;
			}
		} else {
			Ax25Frame frame = (Ax25Frame) prim;
			switch (frame.type) {
			case Ax25Frame.TYPE_U_DISCONNECT_MODE: // DM
				iFrameQueue = new ConcurrentLinkedDeque<Iframe>(); // discard the queue
				// Disconnect and stop any timer
				state = DISCONNECTED;
				t1_timer = 0;
				t3_timer = 0;
				RC = 0;
				break;
			case Ax25Frame.TYPE_S:
				if (Config.getBoolean(Config.DEBUG_LAYER2))
					ta.append("VA: " + VA + " VR: " + VR + " NR: " + frame.NR + " NS: " + frame.NS);
				switch (frame.SS) {
				// RR;
				case Ax25Frame.TYPE_S_RECEIVE_READY:
					peerReceiverBusy = false; // clear receiver busy
					checkNeedForResponse(frame.toCallsign, frame.fromCallsign, frame.isCommandFrame(), frame.PF);
 					if (VA <= frame.NR && frame.NR <= VS) {
						checkIframeAckd(frame);	
						state = CONNECTED;
					} else {
						NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
						state = AWAITING_CONNECTION;
					}
 					break;
 					
 				// RNR
				case Ax25Frame.TYPE_S_RECEIVE_NOT_READY:
					peerReceiverBusy = true;
					checkNeedForResponse(frame.toCallsign, frame.fromCallsign, frame.isCommandFrame(), frame.PF);
					if (VA <= frame.NR && frame.NR <= VS) {
						checkIframeAckd(frame);	
						state = CONNECTED;
					} else {
						NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
						state = AWAITING_CONNECTION;
					}
					break;
				
				// SREJ - Selective Reject
				case Ax25Frame.TYPE_S_SELECTIVE_REJECT:
					peerReceiverBusy = false;
					checkNeedForResponse(frame.toCallsign, frame.fromCallsign, frame.isCommandFrame(), frame.PF);
					if (VA <= frame.NR && frame.NR <= VS) {
						if (frame.PF == 1) 
							VA = frame.NR;
						t1_timer = 0; // stop T1
						t3_timer = 1; // start T3
						// TODO Select T1 Value
						checkIframeAckd(frame);
						// TODO push old Iframe NR on queue
						//////////////////iFrameQueue.push(oldFrame);

						state = CONNECTED;
					} else {
						// TODO NR error recovery
						state = AWAITING_CONNECTION;
					}

					break;

				// REJ
				case Ax25Frame.TYPE_S_REJECT:
					peerReceiverBusy = false;
					checkNeedForResponse(frame.toCallsign, frame.fromCallsign, frame.isCommandFrame(), frame.PF);
					if (VA <= frame.NR && frame.NR <= VS) {
						VA = frame.NR;
						t1_timer = 0; // stop T1
						t3_timer = 0; // stop T3
						// TODO select T1 value
						invokeRetransmission();

						state = CONNECTED;
					} else {
						NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
						state = AWAITING_CONNECTION;
					}
					break;

				default:
					break;
				}
				
				if (Config.getBoolean(Config.DEBUG_LAYER2))
					ta.append(" => VA: " + VA + " VR: " + VR + "\n");
				
				break; // end of case frame type S
					
			case Ax25Frame.TYPE_I:
				processIFrame(frame, CONNECTED);
				break;

			default:
				break;
			}
		}
	}
	
	private void processIFrame(Ax25Frame frame, int finalState) {
		// Received I FRAME - this is considered FTL0 Layer 3 data
		if (Config.getBoolean(Config.DEBUG_LAYER2))
			ta.append("LAYER2: " + frame + "\n");
		FTL0Frame ftl;
		try {
			ftl = new FTL0Frame(frame);
			// check if command, but if not we throw exception
			// also checks length and integrity or throws exception
			if (VA <= frame.NR && frame.NR <= VS) {
				checkIframeAckd(frame);
				// Full duplex so our receiver can't be busy
				if (frame.NS == VR) {
					VR++;
					rejectException = false;
					if (sRejException > 0)
						sRejException--;
					// DL DATA Indication => pass data to layer 3
					if (Config.uplink != null)
						Config.uplink.processEvent(ftl);

					// Loop if there are multiple I frames and increament VR???
					if (frame.PF == 1) {
						sendRR(frame);
					} else {
						if (!ackPending) {
							//TODO LM SIEZE REQUEST
							ackPending = true;
						} 
					}
				} else {
					if (rejectException) {
						// TODO discard the I frame
						if (frame.PF == 1) {
						    sendRR(frame);
						} 
					} else {
						if (sRejEnabled) {
							// TODO save contents of I Frame
							if (sRejException > 0) {
								int NR = frame.NS;
								int F = 0;
								sendSREJ(frame, NR, F);
							} else {
								if (frame.NS > VR + 1) {
									sendREJ(frame);
								} else {
									int NR = VR;
									int F = 1;
									sendSREJ(frame, NR, F);
								}
							}
						} else {
							sendREJ(frame);
						}
					}
				}
				state = finalState;
			} else {
				NRErrorRecovery(frame.toCallsign, frame.fromCallsign); // reversed as we pull them from the received frame
				state = AWAITING_CONNECTION;
			}
		} catch (FrameException e) {
			// Bad frame - not valid
			ta.append("Layer 2: ERROR Indication S");
			establishDataLink(frame.toCallsign, frame.fromCallsign); // reversed as we pull them from the received frame
			state = AWAITING_CONNECTION;
		}

	}
	
	private void sendRR(Ax25Frame frame) {
		int F = 1;
		int NR = VR;
		// send RR with from/to reversed as we pull them from the frame
		Sframe sframe = new Sframe(frame.toCallsign, frame.fromCallsign, NR, F, Ax25Frame.TYPE_S_RECEIVE_READY);
		sendFrame(sframe);
		ackPending = false;
	}
	
	private void sendREJ(Ax25Frame frame) {
		// TODO discard contents of I Frame
		rejectException = true;
		int F = frame.PF;
		int NR = VR;
		// send REJ with from/to reversed as we pull them from the frame
		Sframe sframe = new Sframe(frame.toCallsign, frame.fromCallsign, NR, F, Ax25Frame.TYPE_S_REJECT);
		sendFrame(sframe);
		ackPending = false;
		

	}
	
	private void sendSREJ(Ax25Frame frame, int NR, int F) {
		sRejException++;
		Sframe sframe = new Sframe(frame.toCallsign, frame.fromCallsign, NR, F, Ax25Frame.TYPE_S_SELECTIVE_REJECT);
		sendFrame(sframe);
		ackPending = false;
	}

	private void stateTimerRec(Ax25Primitive prim) {
		if (prim instanceof Ax25Request) {
			Ax25Request req = (Ax25Request) prim;
			switch (req.type) {
			case Ax25Request.TIMER_T1_EXPIRY:
				if (RC >= RETRIES_N2) {
					// Max retries reached, disconnect
					if (VA == VS) {
						if (peerReceiverBusy) {
							//DL ERROR INDICATION U
							ta.append("DL ERROR - N2 timeouts; extended peer busy condition");
						} else {
							// DL ERROR INDICATION T
							ta.append("DL ERROR - N2 timeouts; no response to enquiry");
						}
					} else {
						// DL ERROR INDICATION I
						ta.append("DL ERROR - N2 timeouts; unacknowldged data");
					}
					// Send DM
					iFrameQueue = new ConcurrentLinkedDeque<Iframe>(); // discard the queue
					int F = 0;
					Uframe frame = new Uframe(lastCommand.fromCallsign, lastCommand.toCallsign, F, Ax25Frame.TYPE_U_DISCONNECT_MODE);
					sendFrame(frame);
					state = DISCONNECTED;
				} else {
					RC++;
					transmitInquiry(lastCommand.fromCallsign, lastCommand.toCallsign);					
					state = TIMER_RECOVERY;
				}
				break;
			case Ax25Request.DL_CONNECT:
				iFrameQueue = new ConcurrentLinkedDeque<Iframe>(); // discard the queue
				establishDataLink(req.fromCall, req.toCall);
				state = AWAITING_CONNECTION;
				break;

			case Ax25Request.DL_DISCONNECT:
				iFrameQueue = new ConcurrentLinkedDeque<Iframe>(); // discard the queue
				RC = 0;
				// Send DISC
				int P = 1;
				Uframe frame = new Uframe(req.fromCall, req.toCall, P, Ax25Frame.TYPE_U_DISCONNECT);
				sendFrame(frame);
				t3_timer = 0; // stop T3
				t1_timer = 1; // start T1
				state = AWAITING_RELEASE;
				break;
			case Ax25Request.DL_DATA:
				iFrameQueue.add(((Ax25Request)prim).iFrame); // add to end of iFrame queue
				state = TIMER_RECOVERY;
				break;
			case Ax25Request.DL_POP_IFRAME:
				// Transmit this I FRAME
				if (!peerReceiverBusy)
					if (!(VS == VA + 7)) {
						Iframe iFrame = iFrameQueue.pop(); // remove the head of the queue
						P = 0;
						// NS = VS and NR = VR
						iFrame.setControlByte(VR, VS, P);
						sendFrame(iFrame);
						lastCommand = iFrame;
						VS++;
						ackPending = false;
						if (!(t1_timer > 0)) {
							t1_timer = 1; // start T1
							t3_timer = 0; // stop T3
						}
						state = TIMER_RECOVERY;
					} else {
						// nothing to do, we leave the iFrame on the queue
					}

				break;

			default:
				break;
			}
		} else {
			Ax25Frame frame = (Ax25Frame) prim;
			switch (frame.type) {
			case Ax25Frame.TYPE_S:
				if (Config.getBoolean(Config.DEBUG_LAYER2))
					ta.append("VA: " + VA + " VR: " + VR + " NR: " + frame.NR + " NS: " + frame.NS);
				switch (frame.SS) {
				// RR;
				case Ax25Frame.TYPE_S_RECEIVE_READY:
					peerReceiverBusy = false; // clear receiver busy
					processTimerRecoveryRR(frame);
 					break;
 					
 	 				// RNR
 					case Ax25Frame.TYPE_S_RECEIVE_NOT_READY:
 						peerReceiverBusy = true;
 						processTimerRecoveryRR(frame);
 						break;
 	 				// REJ - Reject
 	 				case Ax25Frame.TYPE_S_REJECT:
 	 					peerReceiverBusy = false; // clear receiver busy
 	 					if (!frame.isCommandFrame() && frame.PF == 1) {
 	 						t1_timer = 0; // stop T1
 	 						// TODO select T1 value
 	 						if (VA <= frame.NR && frame.NR <= VS) {
 	 							VA = frame.NR;
 	 							if (VS == VA) {
 	 								t3_timer = 1;
 	 								state = CONNECTED;
 	 							} else {
 	 								invokeRetransmission();
 	 								state = TIMER_RECOVERY;
 	 							}
 	 						} else {
 	 							NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
 	 							state = AWAITING_CONNECTION;
 	 						}
 	 					} else {
 	 						if (frame.isCommandFrame() && frame.PF == 1) {
 	 							int F = 1;
 	 							enquiryResponse(frame.toCallsign, frame.fromCallsign, F);  // reverse from / to as we pull this from the received frame
 	 						}
 	 						if (VA <= frame.NR && frame.NR <= VS) {
 	 							VA = frame.NR;
 	 							if (VS == VA) {
 	 								state = TIMER_RECOVERY;
 	 							} else {
 	 								invokeRetransmission();
 	 								state = TIMER_RECOVERY; 	 								
 	 							}
 	 						} else {
	 							NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
 	 							state = AWAITING_CONNECTION;	 							
 	 						}
 	 					}

 	 					break;

 	 				// SREJ - Selective Reject
 					case Ax25Frame.TYPE_S_SELECTIVE_REJECT:
 						// TODO
 						break;
				}
				if (Config.getBoolean(Config.DEBUG_LAYER2))
					ta.append(" => VA: " + VA + " VR: " + VR + "\n");
				
				break; // end of case frame type S
			case Ax25Frame.TYPE_U_DISCONNECT_MODE: // DM
				if (frame.PF == 1) {
					// DL ERROR Indication E
					ta.append("LAYER 2 ERROR: DL ERROR Indication E");
					iFrameQueue = new ConcurrentLinkedDeque<Iframe>(); // discard the queue
					t1_timer = 0;
					t3_timer = 0;
					state = DISCONNECTED;
				}
				break;

			case Ax25Frame.TYPE_I:
				processIFrame(frame, CONNECTED);
				break;

			default:
				break;
			}
		}
	}

	private void processTimerRecoveryRR(Ax25Frame frame) {
		if (!frame.isCommandFrame() && frame.PF == 1) {
			t1_timer = 0; // stop T1
			// TODO select T1 value
				if (VA <= frame.NR && frame.NR <= VS) {
					VA = frame.NR;
					if (VS == VA) {
						t3_timer = 1; // start T3
						state = CONNECTED;
					} else {
						invokeRetransmission();
						state = TIMER_RECOVERY;
					}
				} else {
					NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
					state = AWAITING_CONNECTION;
				}
		} else {
			if (frame.isCommandFrame() && frame.PF == 1) {
				int F = 1;
				enquiryResponse(frame.toCallsign, frame.fromCallsign, F);  // reverse from / to as we pull this from the received frame
			}
			if (VA <= frame.NR && frame.NR <= VS) {
				VA = frame.NR;
				state = TIMER_RECOVERY;
			} else {
				NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
					state = AWAITING_CONNECTION;							
			}
		}
			if (VA <= frame.NR && frame.NR <= VS) {
			checkIframeAckd(frame);	
			state = CONNECTED;
		} else {
			NRErrorRecovery(frame.toCallsign, frame.fromCallsign);  // reverse from / to as we pull this from the received frame
			state = AWAITING_CONNECTION;
		}

	}
	private void invokeRetransmission() {
		// backtrack
		////////////////////////////// TODO
	}
	
	private void checkNeedForResponse(String fromCallsign, String toCallsign, boolean command, int PF) {
		// command and P = 1
		if (command && PF == 1) {
			enquiryResponse(fromCallsign, toCallsign, 1); // lastCommand.fromCallsign, lastCommand.toCallsign
		} else {
			if (!command && PF ==1) {
				// DL ERROR IND - A
				ta.append("LAYER 2: ERROR Indication - A");
			}
		}
		
		// response and F = 1
	}
	
	private void enquiryResponse(String fromCallsign, String toCallsign, int PF) {
		// NR = VR
		// We are full duplex so our receiver can't be busy
		// So send RR
		int NR = VR;
		int P = 0;
		ackPending = false;
		Sframe frame = new Sframe(fromCallsign, toCallsign, NR, P, Ax25Frame.TYPE_S_RECEIVE_READY);
		sendFrame(frame);
	}
	
	private void transmitInquiry(String fromCallsign, String toCallsign) {
		int P = 1;
		int NR = VR;
		// We are full duplex so do not need to transmit RNR as receiver can not be "busy"
		// So send RR with P bit set as an inquiry because we did not get an ACK
		Sframe frame = new Sframe(fromCallsign, toCallsign, NR, P, Ax25Frame.TYPE_S_RECEIVE_READY);
		sendFrame(frame);
		ackPending = false;
		// Start the T1 Timer
		RC=0;
		t1_timer = 1;

	}
	
	private void NRErrorRecovery(String fromCall, String toCall) {
		// DL ERROR Indication J
		ta.append("LAYER 2: DL ERROR Indication - J");
		establishDataLink(fromCall, toCall);
		// clear Layer 3 initialized
		
	}
	
	private void clearExceptionConditions() {
		peerReceiverBusy = false;
		rejectException = false;
		// full duplex so own receiver is not busy and does not need to be cleared
		ackPending = false;
	}
	
	private void establishDataLink(String fromCall, String toCall) {
		clearExceptionConditions();
		RC = 0;
//		PF = 1;
		// Send SABM
		// Create Login Request
		int controlByte = Ax25Frame.TYPE_U_SABM;
		Ax25Frame frame = new Ax25Frame(fromCall, toCall, controlByte);
		lastCommand = frame;
		sendFrame(frame);
		// Start the T1 Timer
		RC=0;
		t3_timer = 0; // stop T3
		t1_timer = 1; // start T1
		
	}
	
	private void checkIframeAckd(Ax25Frame frame) {
		// Check I frame ACK'd
		if (peerReceiverBusy) {
			VA = frame.NR;
			t3_timer = 1; // start T3 timer
			if (t1_timer == 0) {
				t1_timer = 1; // then start T1 timer
				RC = 0;
			}
		} else {
			if (frame.NR == VS) {
				VA = frame.NR;
				t1_timer = 0; // stop t1
				t3_timer = 1; // start t3
				// TODO select T1 value
			} else {
				// then not all frames ACK'd
				if (frame.NR != VA) {
					VA = frame.NR;
					t1_timer = 1; // restart T1
					RC = 0;
				}
			}
		}
	}
	
	public void setTncDecoder(TncDecoder tnc, JTextArea ta) {
		tncDecoder = tnc;
		this.ta = ta;
	}

	@Override
	public void run() {
		Log.println("STARTING Layer 2 Data Link Thread");
		while (running) {
			if (t1_timer > 0) {
				// we are timing something
				t1_timer++;
				if (t1_timer > TIMER_T1) {
					t1_timer = 0;
					System.err.println("T1 expired");
					nextState(new Ax25Request(Ax25Request.TIMER_T1_EXPIRY));
				}
				
			}
			if (t3_timer > 0) {
				// we are timing something
				t3_timer++;
				if (t3_timer > TIMER_T3) {
					t3_timer = 0;
					nextState(new Ax25Request(Ax25Request.TIMER_T3_EXPIRY));
				}
			}
			if (frameEventQueue.size() > 0) {
				nextState(frameEventQueue.poll());
			} 

			try {
				Thread.sleep(LOOP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Config.mainWindow != null)
				Config.mainWindow.setLayer2Status(states[state]);
		}
		Log.println("EXIT Layer 2 Thread");
	}
	
}
