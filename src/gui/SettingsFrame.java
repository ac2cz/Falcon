package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JButton;

import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import common.Config;
import common.LayoutLoadException;
import common.Log;
import common.SpacecraftSettings;
import fileStore.Directory;
import pacSat.SerialTncDecoder;
import pacSat.TncDecoder;
import com.g0kla.telem.server.Location;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

/**
 * 
 * Amsat PacSat Ground
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
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
 */
@SuppressWarnings("serial")
public class SettingsFrame extends JDialog implements ActionListener, ItemListener, FocusListener, WindowListener, DocumentListener {

	public static final int MAX_CALLSIGN_LEN = 6;
	public static final int MAX_STATION_LEN = 50;
	public static final String NONE = "NONE";
	
	public static final String SETTINGS_WINDOW_X = "settings_window_x";
	public static final String SETTINGS_WINDOW_Y = "settings_window_y";
	public static final String SETTINGS_WINDOW_WIDTH = "settings_window_width";
	public static final String SETTINGS_WINDOW_HEIGHT = "settings_window_height";
	
	private JPanel contentPane,customByteButtons, panelBytes, panelBytes2;
	private JTextField txtLogFileDirectory,txtArchiveDirectory;
	private JTextField txtLatitude;
	private JTextField txtLongitude;
	private JTextField txtMaidenhead;
	private JTextField txtStation;
	private JTextField txtAltitude;


	private JTextField txtCallsign;
	private JTextField txtTxDelay, txtHostname, txtTcpPort, txtFontSize;
	private JTextArea txtTextAtStart, txtBytesAtStart, txtTextAtEnd, txtBytesAtEnd;
	private JLabel lblTextAtStart, lblTextAtEnd;
	JRadioButton rbTcpTncInterface, rbSerialTncInterface, rbTextEdit, rbBytesEdit;
	private JCheckBox cbDebugLayer2, cbDebugLayer3, cbLogKiss, cbLogging, cbDebugTx, cbDebugDownlink, cbTxInhibit, 
					  cbUploadToServer, cbToggleKiss, cbSendCustomBytes, cbShowSystemFilesInDir,cbKeepCaretAtEndOfLog;
	private JComboBox cbTncComPort, cbTncBaudRate, cbTncDataBits, cbTncStopBits, cbTncParity;
	boolean useUDP;
	boolean tcp; // true if we show the tcp interface settings for the TNC
	
	private JPanel serverPanel;
	JPanel leftcolumnpanelSerial;
	JPanel leftcolumnpanelTCP;
	
	JButton btnSave;
	JButton btnCancel;
	JButton btnBrowse,btnBrowseArchive;
		
	/**
	 * Create the Dialog
	 */
	public SettingsFrame(JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Settings");
		addWindowListener(this);
//		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		//this.setResizable(false);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		// South panel for the buttons
		JPanel southpanel = new JPanel();
		contentPane.add(southpanel, BorderLayout.SOUTH);
		southpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		btnSave = new JButton("Save");
		btnSave.addActionListener(this);
		southpanel.add(btnSave);
		getRootPane().setDefaultButton(btnSave);
		
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		southpanel.add(btnCancel);
		
		// North panel for settings that span across the window
		JPanel northpanel = new JPanel();
		contentPane.add(northpanel, BorderLayout.NORTH);
		//JPanel northcolumnpanel = addColumn(northpanel);
		//txtLogFileDirectory = addSettingsRow(northpanel, 20, "Log files directory", Config.logFileDirectory);
		//JPanel panel = new JPanel();
		//northpanel.add(panel);
		northpanel.setLayout(new BorderLayout());

		JPanel northpanel1 = new JPanel();
		northpanel.add(northpanel1, BorderLayout.NORTH);
		JPanel northpanelA = new JPanel();
		JPanel northpanelB = new JPanel();
		northpanel1.setLayout(new BorderLayout());
		northpanel1.add(northpanelA, BorderLayout.NORTH);
		northpanel1.add(northpanelB, BorderLayout.SOUTH);
		northpanelA.setLayout(new BorderLayout());
		northpanelB.setLayout(new BorderLayout());
		
		JLabel lblHomeDir = new JLabel("Home directory     ");
		lblHomeDir.setToolTipText("This is the directory that contains the settings file");
		lblHomeDir.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanelA.add(lblHomeDir, BorderLayout.WEST);

		JLabel lblHomeDir2 = new JLabel(Config.homeDir);
		northpanelA.add(lblHomeDir2, BorderLayout.CENTER);



		JPanel northpanel2 = new JPanel();
		northpanel.add(northpanel2, BorderLayout.SOUTH);
		JPanel northpanel2A = new JPanel();
		JPanel northpanel2B = new JPanel();
		northpanel2.setLayout(new BorderLayout());
		northpanel2.add(northpanel2A, BorderLayout.NORTH);
		northpanel2.add(northpanel2B, BorderLayout.SOUTH);
		northpanel2A.setLayout(new BorderLayout());
		northpanel2B.setLayout(new BorderLayout());

		
		JLabel lblLogFilesDir = new JLabel("Log files directory");
		lblLogFilesDir.setToolTipText("This sets the directory that the downloaded telemetry data is stored in");
		lblLogFilesDir.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanel2A.add(lblLogFilesDir, BorderLayout.WEST);
		
		txtLogFileDirectory = new JTextField(Config.get(Config.LOGFILE_DIR));
		northpanel2A.add(txtLogFileDirectory, BorderLayout.CENTER);
		txtLogFileDirectory.setColumns(30);
		
		txtLogFileDirectory.addActionListener(this);
		
		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(this);
		northpanel2A.add(btnBrowse, BorderLayout.EAST);
		
		if (Config.logDirFromPassedParam) {
			txtLogFileDirectory.setEnabled(false);
			btnBrowse.setVisible(false);
			JLabel lblPassedParam = new JLabel("  (Fixed at Startup)");
			northpanel2A.add(lblPassedParam, BorderLayout.EAST);
		}

		JLabel lblArchiveDir = new JLabel("Archive directory");
		lblArchiveDir.setToolTipText("This sets the folder where the directory will be arvhived to once it is too large");
		lblArchiveDir.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanel2B.add(lblArchiveDir, BorderLayout.WEST);
		
		txtArchiveDirectory = new JTextField(Config.get(Config.ARCHIVE_DIR));
		northpanel2B.add(txtArchiveDirectory, BorderLayout.CENTER);
		txtArchiveDirectory.setColumns(30);
		
		txtArchiveDirectory.addActionListener(this);
		
		btnBrowseArchive = new JButton("Browse");
		btnBrowseArchive.addActionListener(this);
		northpanel2B.add(btnBrowseArchive, BorderLayout.EAST);
		
		
		TitledBorder eastTitle1 = title("Files and Directories");
		northpanel.setBorder(eastTitle1);
		
		// Center panel for 2 columns of settings
		JPanel centerpanel = new JPanel();
		contentPane.add(centerpanel, BorderLayout.CENTER);
		//centerpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		centerpanel.setLayout(new BoxLayout(centerpanel, BoxLayout.X_AXIS));
		
		// Add left column with 2 panels in it
		JPanel leftcolumnpanel = new JPanel();
		centerpanel.add(leftcolumnpanel);
		//leftcolumnpanel.setLayout(new GridLayout(2,1,10,10));
		leftcolumnpanel.setLayout(new BoxLayout(leftcolumnpanel, BoxLayout.Y_AXIS));
		
		serverPanel = addColumn(leftcolumnpanel,6);
		TitledBorder eastTitle2 = title("Ground Station Params");
		serverPanel.setBorder(eastTitle2);

		txtCallsign = addSettingsRow(serverPanel, 15, "Groundstation Name", 
				"Ground station name is the unique identifier that you will use to store data on the telemetry server", Config.get(Config.CALLSIGN));
	//	txtPrimaryServer = addSettingsRow(serverPanel, 15, "Telem Server", "The address of the Telemetry server. "
	//			+ "Should not need to be changed", Config.get(Config.TELEM_SERVER));
		txtLatitude = addSettingsRow(serverPanel, 10, "Lat (S is -ve)", "Latitude / Longitude or Locator need to be specified if you supply decoded data to the server", Config.get(Config.LATITUDE)); // South is negative
		txtLongitude = addSettingsRow(serverPanel, 10, "Long (W is -ve)", "Latitude / Longitude or Locator need to be specified if you supply decoded data to the server", Config.get(Config.LONGITUDE)); // West is negative
		JPanel locatorPanel = new JPanel();
		JLabel lblLoc = new JLabel("Lat Long gives Locator: ");
		txtMaidenhead = new JTextField(Config.get(Config.MAIDENHEAD_LOC));
		txtMaidenhead.addActionListener(this);
		txtMaidenhead.addFocusListener(this);

		txtMaidenhead.setColumns(10);
		serverPanel.add(locatorPanel);
		locatorPanel.add(lblLoc);
		locatorPanel.add(txtMaidenhead);

		txtAltitude = addSettingsRow(serverPanel, 15, "Altitude (m)", "Altitude will be supplied to the server along with your data if you specify it", Config.get(Config.ALTITUDE));
		txtStation = addSettingsRow(serverPanel, 15, "RF-Receiver Description", "RF-Receiver can be specified to give us an idea of the types of stations that are in operation", Config.get(Config.STATION_DETAILS));


		serverPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));
		
		JPanel leftcolumnpanel3 = addColumn(leftcolumnpanel,6);
		TitledBorder tncTitle = title("TNC");
		leftcolumnpanel3.setBorder(tncTitle);
		JPanel buttons = new JPanel();
		leftcolumnpanel3.add(buttons);
		JLabel lblInterface = new JLabel("Interface: ");
		buttons.add(lblInterface);
		rbTcpTncInterface = new JRadioButton("TCP");
		rbSerialTncInterface = new JRadioButton("Serial");
		rbTcpTncInterface.addActionListener(this);
		rbSerialTncInterface.addActionListener(this);
		buttons.add(rbSerialTncInterface);
		buttons.add(rbTcpTncInterface);
		ButtonGroup groupInterface = new ButtonGroup();
		groupInterface.add(rbSerialTncInterface);
		groupInterface.add(rbTcpTncInterface);

		leftcolumnpanelTCP = new JPanel();
		leftcolumnpanelTCP.setLayout(new BoxLayout(leftcolumnpanelTCP, BoxLayout.Y_AXIS) );
		leftcolumnpanelSerial = new JPanel();
		leftcolumnpanelSerial.setLayout(new BoxLayout(leftcolumnpanelSerial, BoxLayout.Y_AXIS) );
		leftcolumnpanel3.add(leftcolumnpanelTCP);
		leftcolumnpanel3.add(leftcolumnpanelSerial);

		txtHostname = addSettingsRow(leftcolumnpanelTCP, 15, "TCP Hostname", 
				"Hostname where the TNC program is running", Config.get(Config.TNC_TCP_HOSTNAME));
		txtTcpPort = addSettingsRow(leftcolumnpanelTCP, 15, "TCP Port", 
				"TCP Port that the TNC program is listening on for KISS data", Config.get(Config.TNC_TCP_PORT));

		
		String[] ports = SerialTncDecoder.getSerialPorts();
		if (ports == null) {
			ports = new String[1];
			ports[0] = "NONE";
		}
		
		cbTncComPort = addComboBoxRow(leftcolumnpanelSerial, "Com Port", 
				"The Serial Port (virtual or otherwise) that your TNC is on", ports);
		setSelection(cbTncComPort, ports, Config.get(Config.TNC_COM_PORT));
		
		cbTncBaudRate = addComboBoxRow(leftcolumnpanelSerial, "Baud Rate", 
				"The baud rate for the serial port. This is not the baud rate for communication to the spacecraft.  Just the rate for connection to the TNC.", 
				SerialTncDecoder.getAvailableBaudRates());
		setSelection(cbTncBaudRate, SerialTncDecoder.getAvailableBaudRates(), Config.get(Config.TNC_BAUD_RATE));

		cbToggleKiss = addCheckBoxRow(leftcolumnpanelSerial, "Toggle TNC in/out of KISS Mode", "Toggle TNC in/out of KISS mode at start/top.  Uncheck to leave in Kiss mode.",
				Config.getBoolean(Config.TOGGLE_KISS));
		
		cbSendCustomBytes = addCheckBoxRow(leftcolumnpanelSerial, "Send TNC custom bytes at startup", "Send user defined bytes to toggle TNC in/out of KISS mode at start/top.",
				Config.getBoolean(Config.SEND_USER_DEFINED_TNC_BYTES));
		
		customByteButtons = new JPanel();
		leftcolumnpanelSerial.add(customByteButtons);
		JLabel lblEdit = new JLabel("Edit: ");
		customByteButtons.add(lblEdit);
		rbTextEdit = new JRadioButton("Text");
		rbBytesEdit = new JRadioButton("Bytes");
		rbTextEdit.addActionListener(this);
		rbBytesEdit.addActionListener(this);
		customByteButtons.add(rbTextEdit);
		customByteButtons.add(rbBytesEdit);
		ButtonGroup groupInterface2 = new ButtonGroup();
		groupInterface2.add(rbTextEdit);
		groupInterface2.add(rbBytesEdit);
		
		if (Config.getBoolean(Config.EDIT_KISS_BYTES)) { 
			rbBytesEdit.setSelected(true);
			rbTextEdit.setSelected(false);
		} else {
			rbBytesEdit.setSelected(false);
			rbTextEdit.setSelected(true);
		}
		
		panelBytes = new JPanel();
		leftcolumnpanelSerial.add(panelBytes);
		panelBytes.setLayout(new BorderLayout(5,5));
		
		panelBytes.add(new JLabel("Send these bytes to TNC at startup"), BorderLayout.NORTH);
		txtTextAtStart = new JTextArea(byteToString(Config.get(Config.KISS_BYTES_AT_START)));
		panelBytes.add(txtTextAtStart, BorderLayout.WEST);
		txtTextAtStart.setToolTipText("Send this text when TNC toggled into KISS Mode");
		txtTextAtStart.setLineWrap(true);
		txtTextAtStart.setWrapStyleWord(true);
		txtTextAtStart.addFocusListener(this);
		
		txtBytesAtStart = new JTextArea(Config.get(Config.KISS_BYTES_AT_START));
		panelBytes.add(txtBytesAtStart, BorderLayout.CENTER);
		txtBytesAtStart.setLineWrap(true);
		txtBytesAtStart.setWrapStyleWord(true);
		txtBytesAtStart.setToolTipText("Send these bytes when TNC toggled into KISS Mode");
		txtBytesAtStart.addFocusListener(this);
		
		panelBytes2 = new JPanel();
		leftcolumnpanelSerial.add(panelBytes2);
		panelBytes2.setLayout(new BorderLayout(5,5));
		
		panelBytes2.add(new JLabel("Send these bytes to TNC at exit"), BorderLayout.NORTH);
		txtTextAtEnd = new JTextArea(byteToString(Config.get(Config.KISS_BYTES_AT_END)));
		panelBytes2.add(txtTextAtEnd, BorderLayout.WEST);
		txtTextAtEnd.setToolTipText("Send this text to TNC when program ends");
		txtTextAtEnd.setLineWrap(true);
		txtTextAtEnd.setWrapStyleWord(true);
		txtTextAtEnd.addFocusListener(this);
		
		txtBytesAtEnd = new JTextArea(Config.get(Config.KISS_BYTES_AT_END));
		panelBytes2.add(txtBytesAtEnd, BorderLayout.CENTER);
		txtBytesAtEnd.setLineWrap(true);
		txtBytesAtEnd.setWrapStyleWord(true);
		txtBytesAtEnd.setToolTipText("Send these bytes to TNC when program ends");
		txtBytesAtEnd.addFocusListener(this);
		
		setBytesEditable(Config.getBoolean(Config.EDIT_KISS_BYTES));
	//	setKissBytesEnabled(Config.getBoolean(Config.TOGGLE_KISS));

		txtTxDelay = addSettingsRow(leftcolumnpanel3, 5, "TX Delay", 
				"Delay between keying the radio and sending data. Implemented by the TNC.", ""+Config.getInt(Config.TNC_TX_DELAY));

		tcp = Config.getBoolean(Config.KISS_TCP_INTERFACE);
		showTncSettings();
		
		leftcolumnpanel3.add(new Box.Filler(new Dimension(200,10), new Dimension(150,400), new Dimension(500,500)));
		
		// Add a right column with two panels in it
		JPanel rightcolumnpanel = new JPanel();
		centerpanel.add(rightcolumnpanel);
		//rightcolumnpanel.setLayout(new GridLayout(2,1,10,10));
		rightcolumnpanel.setLayout(new BoxLayout(rightcolumnpanel, BoxLayout.Y_AXIS));
		
		JPanel rightcolumnpanel0 = addColumn(rightcolumnpanel,3);
		rightcolumnpanel0.setLayout(new BoxLayout(rightcolumnpanel0, BoxLayout.Y_AXIS));
		TitledBorder eastTitle4 = title("Options");
		rightcolumnpanel0.setBorder(eastTitle4);
		
		cbUploadToServer = addCheckBoxRow(rightcolumnpanel0, "Send Server Telemetry", "Select this if you want to send your collected data to the Server telemetry server",
				Config.getBoolean(Config.SEND_TO_SERVER));
		cbLogging = addCheckBoxRow(rightcolumnpanel0, "Enable Logging", "Log events to a log file for debugging",
				Config.getBoolean(Config.LOGGING) );
		cbLogKiss = addCheckBoxRow(rightcolumnpanel0, "Log KISS", "Log KISS Bytes to a log file",
				Config.getBoolean(Config.KISS_LOGGING) );
		cbTxInhibit = addCheckBoxRow(rightcolumnpanel0, "Inhibit Transmitter", "Prevent the transmission of byte to the TNC",
				Config.getBoolean(Config.TX_INHIBIT) );
		cbDebugLayer2 = addCheckBoxRow(rightcolumnpanel0, "Debug Layer 2", "Select to print out debug for AX25 Layer 2",
				Config.getBoolean(Config.DEBUG_LAYER2) );
		cbDebugLayer3 = addCheckBoxRow(rightcolumnpanel0, "Debug Uplink", "Select to print out debug for Uplink State Machine",
				Config.getBoolean(Config.DEBUG_LAYER3) );
		cbDebugDownlink = addCheckBoxRow(rightcolumnpanel0, "Debug Downlink", "Select to print out debug for Downlink State Machine",
				Config.getBoolean(Config.DEBUG_DOWNLINK) );
		cbDebugTx = addCheckBoxRow(rightcolumnpanel0, "Debug Tx", "Select to print out debug for TNC transmissions",
				Config.getBoolean(Config.DEBUG_TX) );
//		cbShowSystemFilesInDir = addCheckBoxRow(rightcolumnpanel0, "Show System Files on Dir Tab", "Show the whole directory on the same tab (useful for debugging dir holes)",
//				Config.getBoolean(SpacecraftSettings.SHOW_SYSTEM_ON_DIR_TAB) );
		cbKeepCaretAtEndOfLog = addCheckBoxRow(rightcolumnpanel0, "Force Log window to scroll to end", "Each time text is added to the log window, scroll to the end and show it",
				Config.getBoolean(Config.KEEP_CARET_AT_END_OF_LOG) );

		int size = Config.getInt(Config.FONT_SIZE);
		
		txtFontSize = addSettingsRow(rightcolumnpanel0, 5, "Font Size", 
				"Font size for the window", ""+size);
		if (size == 0) {
			Font f = cbKeepCaretAtEndOfLog.getFont();
			size = f.getSize();
		}
		
		rightcolumnpanel0.add(new Box.Filler(new Dimension(10,10), new Dimension(150,400), new Dimension(500,500)));
		
		rightcolumnpanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		enableDependentParams();
		setBytesPanelEnabled(Config.getBoolean(Config.SEND_USER_DEFINED_TNC_BYTES));
	}
	
	public void setBytesEditable(boolean b) {
		
//		txtBytesAtStart.setEditable(b);
//		txtBytesAtEnd.setEditable(b);
		txtBytesAtStart.setEnabled(b);
		txtBytesAtEnd.setEnabled(b);

//		txtTextAtStart.setEditable(!b);
//		txtTextAtEnd.setEditable(!b);
		txtTextAtStart.setEnabled(!b);
		txtTextAtEnd.setEnabled(!b);
		
		if (b) {
			txtBytesAtStart.getDocument().addDocumentListener(this);
			txtBytesAtEnd.getDocument().addDocumentListener(this);
			txtTextAtStart.getDocument().removeDocumentListener(this);
			txtTextAtEnd.getDocument().removeDocumentListener(this);
		} else  {
			txtBytesAtStart.getDocument().removeDocumentListener(this);
			txtBytesAtEnd.getDocument().removeDocumentListener(this);
			txtTextAtStart.getDocument().addDocumentListener(this);
			txtTextAtEnd.getDocument().addDocumentListener(this);
		}
	}
	
	private void setBytesPanelEnabled(boolean b) {
		customByteButtons.setVisible(b);
		panelBytes.setVisible(b);
		panelBytes2.setVisible(b);
		txtBytesAtStart.setVisible(b);
		txtBytesAtEnd.setVisible(b);

		txtTextAtStart.setVisible(b);
		txtTextAtEnd.setVisible(b);
	}
	
	private void setKissBytesEnabled(boolean b) {
		txtBytesAtStart.setEnabled(b);
		txtBytesAtEnd.setEnabled(b);

		txtTextAtStart.setEnabled(b);
		txtTextAtEnd.setEnabled(b);
		
	}
	
	/**
	 * Given a set of bytes stored in a string, work out the actual ASCI characters
	 * and make a string for display
	 * 
	 * @param bytes
	 * @return
	 */
	public static String byteToString(String bytes) {
		String s = "";
		String by[] = bytes.split("\\s+");
		for (String c : by) {
			char ch = 999;
			try {
			ch = (char) Integer.parseUnsignedInt(c, 16);
			} catch (NumberFormatException e) { };
			if (ch != 999)
				s = s + ch;
		}
		return s;
	}
	
	/**
	 * Given a set of bytes stored in a string, work out the ascii values and
	 * return as an array of ints
	 * 
	 * @param str
	 * @return
	 */
	public static int[] stringToBytes(String str) {
		
		String byString[] = str.split("\\s+");
		int[] by = new int[byString.length];
		int i = 0;
		for (String c : byString) {
			char ch = 999;
			try {
			ch = (char) Integer.parseUnsignedInt(c, 16);
			} catch (NumberFormatException e) { };
			if (ch != 999)
				by[i] = ch;
			i++;
		}
		return by;
	}
	
	/**
	 * Given a string, work out the ascii values and store them in hex in a string
	 * @param str
	 * @return
	 */
	public static String stringToByteString(String str) {
		String s = "";
		for (int i=0; i< str.length(); i++) {
			char c = str.charAt(i);
			String ch = "";
			try {
				ch = Integer.toHexString(c);
			} catch (NumberFormatException e) { };
			if (!ch.equalsIgnoreCase("")) {
				ch = ch.toUpperCase();
				if (ch.length() == 1)
					ch = "0" + ch;
				if (i == str.length()-1)
					s = s + ch;
				else
					s = s + ch + " ";
			}
		}
		return s;
	}
	
	private void setSelection(JComboBox comboBox, String[] values, String value ) {
		int i=0;
		for (String rate : values) {
			if (rate.equalsIgnoreCase(value))
					break;
			i++;
		}
		if (i >= values.length)
			i = 0;
		comboBox.setSelectedIndex(i);
		
	}

	public void saveProperties() {
		Config.set(SETTINGS_WINDOW_HEIGHT, this.getHeight());
		Config.set(SETTINGS_WINDOW_WIDTH, this.getWidth());
		Config.set(SETTINGS_WINDOW_X, this.getX());
		Config.set(SETTINGS_WINDOW_Y, this.getY());
		
		Config.save();
	}
	
	public void loadProperties() {
		if (Config.getInt(SETTINGS_WINDOW_X) == 0) {
			Config.set(SETTINGS_WINDOW_X, 100);
			Config.set(SETTINGS_WINDOW_Y, 100);
			Config.set(SETTINGS_WINDOW_WIDTH, 600);
			Config.set(SETTINGS_WINDOW_HEIGHT, 650);
		}
		setBounds(Config.getInt(SETTINGS_WINDOW_X), Config.getInt(SETTINGS_WINDOW_Y), 
				Config.getInt(SETTINGS_WINDOW_WIDTH), Config.getInt(SETTINGS_WINDOW_HEIGHT));
	}
	
	private TitledBorder title(String s) {
		TitledBorder title = new TitledBorder(null, s, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		return title;
	}
	
	private boolean validLatLong() {
		float lat = 0, lon = 0;
		try {
			lat = Float.parseFloat(txtLatitude.getText());
			lon = Float.parseFloat(txtLongitude.getText());
			if (lat == Float.parseFloat(Config.DEFAULT_LATITUDE) || 
					txtLatitude.getText().equals("")) return false;
			if (lon == Float.parseFloat(Config.DEFAULT_LONGITUDE) || 
					txtLongitude.getText().equals("")) return false;
		} catch (NumberFormatException n) {
			JOptionPane.showMessageDialog(this,
					"Only numerical values are valid for the latitude and longitude.",
					"Format Error\n",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if ((Float.isNaN(lon)) ||
		(Math.abs(lon) > 180) ||
		(Float.isNaN(lat)) ||
		(Math.abs(lat) == 90.0) ||
		(Math.abs(lat) > 90)) {
			JOptionPane.showMessageDialog(this,
					"Invalid latitude or longitude.",
					"Error\n",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}
	
	private boolean validLocator() {
		if (txtMaidenhead.getText().equalsIgnoreCase(Config.DEFAULT_LOCATOR) || 
				txtMaidenhead.getText().equals("")) {
			JOptionPane.showMessageDialog(this,
					"Enter a latitude/longitude or set the locator to a valid value",
					"Format Error\n",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	private boolean validAltitude() {
		int alt = 0;
			try {
				alt = Integer.parseInt(txtAltitude.getText());
			} catch (NumberFormatException n) {
				JOptionPane.showMessageDialog(this,
						"Only integer values are valid for the altitude. Specify it to the nearest meter, but with no units.",
						"Format Error\n",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		if ((alt < 0) ||(alt > 8484)) {
			JOptionPane.showMessageDialog(this,
					"Invalid altitude.  Must be between 0 and 8484m.",
					"Format Error\n",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
		
	private void updateLocator() throws Exception {
		if (validLatLong()) {
			Location l = new Location(txtLatitude.getText(), txtLongitude.getText());
			txtMaidenhead.setText(l.maidenhead);
		}
	}
	
	private void updateLatLong() throws Exception {
		if (validLocator()) {
			Location l = new Location(txtMaidenhead.getText());
			txtLatitude.setText(Float.toString(l.latitude)); 
			txtLongitude.setText(Float.toString(l.longitude));
		}
	}

	private void enableDependentParams() {
		if (validLatLong() && validAltitude()) {
			if (validCallsign())
				cbUploadToServer.setEnabled(true);
			else
				cbUploadToServer.setEnabled(false);
			//cbFoxTelemCalcsPosition.setEnabled(true);
			//cbWhenAboveHorizon.setEnabled(true);
			
		} else {
			cbUploadToServer.setEnabled(false);
			//cbFoxTelemCalcsPosition.setEnabled(false);
			//cbWhenAboveHorizon.setEnabled(false);
		}
	}
	
	private boolean validCallsign() {
		if (txtCallsign.getText().equalsIgnoreCase(Config.DEFAULT_CALLSIGN) || 
				txtCallsign.getText().equals("")) return false;
		return true;
	}
	
	private boolean validServerParams() {
		if (!validCallsign()) return false;
		if (!validLocator()) return false;
		if (!validLatLong()) return false;
		if (!validAltitude()) return false;
		return true;
	}

	private JPanel addColumn(JPanel parent, int rows) {
		JPanel columnpanel = new JPanel();
		parent.add(columnpanel);
		//columnpanel.setLayout(new GridLayout(rows,1,10,10));
		columnpanel.setLayout(new BoxLayout(columnpanel, BoxLayout.Y_AXIS));

		return columnpanel;
	}


	private JCheckBox addCheckBoxRow(JPanel parent, String name, String tip, boolean value) {
		JCheckBox checkBox = new JCheckBox(name);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		parent.add(checkBox);
		if (value) checkBox.setSelected(true); else checkBox.setSelected(false);
		return checkBox;
	}
	
	private JComboBox addComboBoxRow(JPanel parent, String name, String tip, String[] values) {
		JPanel row = new JPanel();
		row.setLayout(new GridLayout(1,2,5,5));

		//row.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel lbl = new JLabel(name);
		JComboBox checkBox = new JComboBox(values);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		lbl.setToolTipText(tip);
		row.add(lbl);
		row.add(checkBox);
		parent.add(row);
		parent.add(new Box.Filler(new Dimension(10,5), new Dimension(10,5), new Dimension(10,5)));
		return checkBox;
	}


	
	private JTextArea addSettingsBytesText(JPanel column, JLabel lblTranslation, int length, String name, String tip, String value) {
		JPanel panel = new JPanel();
		column.add(panel);
		panel.setLayout(new BorderLayout(5,5));
		JLabel lblDisplayModuleFont = new JLabel(name);
		lblDisplayModuleFont.setToolTipText(tip);
		panel.add(lblDisplayModuleFont, BorderLayout.NORTH);
		JTextArea textField = new JTextArea(value);
		panel.add(lblTranslation, BorderLayout.WEST);
		panel.add(textField, BorderLayout.CENTER);
		textField.setColumns(length);
		textField.addFocusListener(this);
		textField.getDocument().addDocumentListener(this);

		column.add(new Box.Filler(new Dimension(10,5), new Dimension(10,5), new Dimension(10,5)));

		return textField;
	}


	private JTextField addSettingsRow(JPanel column, int length, String name, String tip, String value) {
		JPanel panel = new JPanel();
		column.add(panel);
		panel.setLayout(new GridLayout(1,2,5,5));
		JLabel lblDisplayModuleFont = new JLabel(name);
		lblDisplayModuleFont.setToolTipText(tip);
		panel.add(lblDisplayModuleFont);
		JTextField textField = new JTextField(value);
		panel.add(textField);
		textField.setColumns(length);
		textField.addActionListener(this);
		textField.addFocusListener(this);

		column.add(new Box.Filler(new Dimension(10,5), new Dimension(10,5), new Dimension(10,5)));

		return textField;
	
	}

	
	void showTncSettings() {
		
		rbTcpTncInterface.setSelected(tcp);
		leftcolumnpanelTCP.setVisible(tcp);
		leftcolumnpanelSerial.setVisible(!tcp);
		
//		txtHostname.setVisible(tcp);
//		txtTcpPort.setVisible(tcp);
//		
//		cbTncComPort.setVisible(!tcp);
//		cbTncBaudRate.setVisible(!tcp);
////		cbTncDataBits.setEnabled(!tcp);
////		cbTncStopBits.setEnabled(!tcp);
////		cbTncParity.setEnabled(!tcp);

		rbSerialTncInterface.setSelected(!tcp);

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			this.dispose();
		}
		if (e.getSource() == txtLatitude || e.getSource() == txtLongitude ) {
			try {
				updateLocator();
			} catch (Exception e1) {
				Log.errorDialog("ERROR", e1.getMessage());
			}
			enableDependentParams();
		}
		if (e.getSource() == txtMaidenhead) {
			try {
				updateLatLong();
			} catch (Exception e1) {
				Log.errorDialog("ERROR", e1.getMessage());
			}
			enableDependentParams();
			
		}
		if (e.getSource() == txtAltitude) {
			validAltitude();
			enableDependentParams();
		}

		if (e.getSource() == btnSave) {
			boolean dispose = true;
			// grab all the latest settings
				Config.set(Config.CALLSIGN, txtCallsign.getText().toUpperCase());
				Log.println("Setting callsign: " + Config.get(Config.CALLSIGN));
//				if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
//					MainWindow.butNew.setEnabled(false);			
//				} else {
//					MainWindow.butNew.setEnabled(true);
//				}
				
				if (validLatLong()) {
					Config.set(Config.LATITUDE,txtLatitude.getText());
					Config.set(Config.LONGITUDE,txtLongitude.getText());
				} else {
					if (txtLatitude.getText().equalsIgnoreCase(Config.DEFAULT_LATITUDE) && txtLongitude.getText().equalsIgnoreCase(Config.DEFAULT_LONGITUDE))
						dispose = true;
					else 
						dispose = false;
				}
				if (validLocator()) {
					Config.set(Config.MAIDENHEAD_LOC,txtMaidenhead.getText());
				} else dispose = false;
				if (validAltitude()) {
					Config.set(Config.ALTITUDE,txtAltitude.getText());
				} else dispose = false;
				Config.set(Config.STATION_DETAILS,txtStation.getText());
				//Config.set(Config.TELEM_SERVER,txtPrimaryServer.getText());
								
				int rate = Integer.parseInt(SerialTncDecoder.getAvailableBaudRates()[cbTncBaudRate.getSelectedIndex()]);
				int port_idx = cbTncComPort.getSelectedIndex();
				String port_name = (String) cbTncComPort.getSelectedItem();
				String port = NONE;
				if (!port_name.equalsIgnoreCase(NONE))
					port = SerialTncDecoder.getSerialPorts()[port_idx];
				int delay = Integer.parseInt(txtTxDelay.getText());
				if (!Config.get(Config.TNC_COM_PORT).equalsIgnoreCase(port) ||
						Config.getInt(Config.TNC_BAUD_RATE) != rate ||
						Config.getInt(Config.TNC_TX_DELAY) != delay) {
					Log.infoDialog("RESTART REQUIRED", "New COM port params.  Restart the Ground Station to configure and to correctly initialize the TNC");
				}
				int p = Config.getInt(Config.TNC_TCP_PORT);
				try {
					p = Integer.parseInt(txtTcpPort.getText());
				} catch (NumberFormatException n) {
					Log.errorDialog("ERROR", "TCP Port needs to be numeric.  Setting it to: " + p);
				}
				
				if (tcp != Config.getBoolean(Config.KISS_TCP_INTERFACE))
					Log.infoDialog("TNC Interface Changed", "You will need to restart the program for the TNC interface to be changed");
				Config.set(Config.KISS_TCP_INTERFACE, tcp);
				if (tcp) {
					Config.set(Config.TNC_TCP_HOSTNAME, txtHostname.getText());
					Config.set(Config.TNC_TCP_PORT, p);
				} else {
					Config.set(Config.TNC_COM_PORT, port);
					Config.set(Config.TNC_BAUD_RATE, rate);

//					int data = Integer.parseInt(SerialTncDecoder.getAvailableDataBits()[cbTncDataBits.getSelectedIndex()]);
//					Config.set(Config.TNC_DATA_BITS, data);
//					Config.set(Config.TNC_STOP_BITS, cbTncStopBits.getSelectedIndex()+1);
//					Config.set(Config.TNC_PARITY, cbTncParity.getSelectedIndex());
				}
				Config.set(Config.TNC_TX_DELAY, delay);
				
				Config.set(Config.KISS_BYTES_AT_START, txtBytesAtStart.getText());
				Config.set(Config.KISS_BYTES_AT_END, txtBytesAtEnd.getText());
				
				Config.set(Config.SEND_TO_SERVER, cbUploadToServer.isSelected());
				Config.set(Config.LOGGING, cbLogging.isSelected());
				if (Config.getBoolean(Config.KISS_LOGGING) != cbLogKiss.isSelected()) {
					Log.infoDialog("RESTART REQUIRED", "KISS Logging Enabled.  Restart the Ground Station to configure and to correctly initialize the decoder for KISS logging");
				}
				Config.set(Config.KISS_LOGGING, cbLogKiss.isSelected());
				Config.set(Config.TX_INHIBIT, cbTxInhibit.isSelected());
				Config.set(Config.DEBUG_LAYER2, cbDebugLayer2.isSelected());
				Config.set(Config.DEBUG_LAYER3, cbDebugLayer3.isSelected());
				Config.set(Config.DEBUG_DOWNLINK, cbDebugDownlink.isSelected());
				Config.set(Config.DEBUG_TX, cbDebugTx.isSelected());
				Config.set(Config.TOGGLE_KISS, cbToggleKiss.isSelected());
				Config.set(Config.SEND_USER_DEFINED_TNC_BYTES, cbSendCustomBytes.isSelected());
		//		Config.set(Config.SHOW_SYSTEM_ON_DIR_TAB, cbShowSystemFilesInDir.isSelected());
				Config.set(Config.KEEP_CARET_AT_END_OF_LOG, cbKeepCaretAtEndOfLog.isSelected());
					
				int fontSize = Integer.parseInt(txtFontSize.getText());
				if (fontSize != Config.getInt(Config.FONT_SIZE)) {
					Log.infoDialog("RESTART REQUIRED", "Font Size changed.  Restart the Ground Station to see the changes.");
				}
				Config.set(Config.FONT_SIZE, fontSize);
				
				if (!Config.get(Config.ARCHIVE_DIR).equalsIgnoreCase(txtArchiveDirectory.getText())) {
					String archiveDirFolder = txtArchiveDirectory.getText();
					File dir = new File(archiveDirFolder);
					if (!dir.exists()) {
						// new to try to make the dir
						if (!Directory.makeDir(archiveDirFolder)) {
							dispose=false;
							return;
						}
						
					}
					if (txtArchiveDirectory.getText().equalsIgnoreCase("."))
						txtArchiveDirectory.setText("");
					File file = new File(txtArchiveDirectory.getText());
					if (txtArchiveDirectory.getText().equalsIgnoreCase("")) {
						Log.errorDialog("Invalid archive directory", "Can not archive into the same folder as the current data: " + txtArchiveDirectory.getText());
						dispose = false;
					} else if (!file.isDirectory() || file == null || !file.exists()){
						Log.errorDialog("Invalid archive directory", "Can not find the specified directory: " + txtArchiveDirectory.getText());
						dispose = false;
					} else {
						Config.set(Config.ARCHIVE_DIR,txtArchiveDirectory.getText());
						Log.println("Setting archive to: " + Config.get(Config.ARCHIVE_DIR));
					}
				}
				if (!Config.get(Config.LOGFILE_DIR).equalsIgnoreCase(txtLogFileDirectory.getText())) {
					boolean currentDir = false;
					if (txtLogFileDirectory.getText().equalsIgnoreCase("."))
						txtLogFileDirectory.setText("");
					if (txtLogFileDirectory.getText().equalsIgnoreCase(""))
						currentDir = true;
						
					File file = new File(txtLogFileDirectory.getText());
					//if (!file.isDirectory())
					//	file = file.getParentFile();
					if (!currentDir && (!file.isDirectory() || file == null || !file.exists())){
						Log.errorDialog("Invalid directory", "Can not find the specified directory: " + txtLogFileDirectory.getText());
						dispose = false;
					} else {

						Object[] options = {"Yes",
						"No"};
						int n = JOptionPane.showOptionDialog(
								MainWindow.frame,
								"Do you want to switch log file directories?",
								"Do you want to continue?",
								JOptionPane.YES_NO_OPTION, 
								JOptionPane.QUESTION_MESSAGE,
								null,
								options,
								options[1]);

						if (n == JOptionPane.YES_OPTION) {
							// Save the current directory, in case we have changes in memory
							try {
								for (SpacecraftSettings sat : Config.spacecraftSettings) {
									sat.directory.save();
								}
								String prev = Config.get(Config.LOGFILE_DIR);
								Config.set(Config.LOGFILE_DIR,txtLogFileDirectory.getText());
								Log.println("Setting log file directory to: " + Config.get(Config.LOGFILE_DIR));
								// Now restart the threads
								Config.close();
								try {
									Config.start();
								} catch (com.g0kla.telem.data.LayoutLoadException e1) {
									Log.errorDialog("ERROR", "Can re-load the layouts: " + e1.getMessage());
								} catch (IOException e1) {
									Log.errorDialog("ERROR", "Re-initializing the config: " + e1.getMessage());
								}
								Config.mainWindow.updateLogfileDir();
							} catch (IOException e2) {
								Log.errorDialog("ERROR", "Could not save the existing directory, abandoning switch\n" + e2.getMessage());
							}
							
						}
					}		
			}
			
			if (dispose) {
				Config.save();
				for (SpacecraftSettings sat : Config.spacecraftSettings) {
					Config.mainWindow.setDirectoryData(sat.name, sat.directory.getTableData());
				}
				this.dispose();
			}
		}
		
		if (e.getSource() == rbTcpTncInterface) {
			tcp = true;
			showTncSettings();
		}
		if (e.getSource() == rbSerialTncInterface) {
			tcp = false;
			showTncSettings();
		}
		
		if (e.getSource() == rbBytesEdit) {
			Config.set(Config.EDIT_KISS_BYTES, true);	
			setBytesEditable(Config.getBoolean(Config.EDIT_KISS_BYTES));
		}
		if (e.getSource() == rbTextEdit) {
			Config.set(Config.EDIT_KISS_BYTES, false);
			setBytesEditable(Config.getBoolean(Config.EDIT_KISS_BYTES));
		}
		


		if (e.getSource() == btnBrowse) {
			File dir = null;
			if (!Config.get(Config.LOGFILE_DIR).equalsIgnoreCase("")) {
				dir = new File(Config.get(Config.LOGFILE_DIR));
			}

				JFileChooser fc = new JFileChooser();
				fc.setApproveButtonText("Choose");
				if (dir != null) {
					fc.setCurrentDirectory(dir);	
				}			
				fc.setDialogTitle("Choose Directory for Log Files");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (Config.getInt(MainWindow.WINDOW_FC_WIDTH) == 0) {
					Config.set(MainWindow.WINDOW_FC_WIDTH, 600);
					Config.set(MainWindow.WINDOW_FC_HEIGHT, 600);
				}
				fc.setPreferredSize(new Dimension(Config.getInt(MainWindow.WINDOW_FC_WIDTH), Config.getInt(MainWindow.WINDOW_FC_HEIGHT)));
				int returnVal = fc.showOpenDialog(this);
				Config.set(MainWindow.WINDOW_FC_HEIGHT, fc.getHeight());
				Config.set(MainWindow.WINDOW_FC_WIDTH,fc.getWidth());		

				if (returnVal == JFileChooser.APPROVE_OPTION) { 
					String path = fc.getSelectedFile().getAbsolutePath();
					if (!path.equals(""))
						path = path + File.separator;
					txtLogFileDirectory.setText(path);
				} else {
					Log.println("No Selection ");
				}
		}
		
		if (e.getSource() == btnBrowseArchive) {
			File dir = null;
			if (!Config.get(Config.ARCHIVE_DIR).equalsIgnoreCase("")) {
				dir = new File(Config.get(Config.ARCHIVE_DIR));
			}

				JFileChooser fc = new JFileChooser();
				fc.setApproveButtonText("Choose");
				if (dir != null) {
					fc.setCurrentDirectory(dir);	
				}			
				fc.setDialogTitle("Choose Directory for Archive");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (Config.getInt(MainWindow.WINDOW_FC_WIDTH) == 0) {
					Config.set(MainWindow.WINDOW_FC_WIDTH, 600);
					Config.set(MainWindow.WINDOW_FC_HEIGHT, 600);
				}
				fc.setPreferredSize(new Dimension(Config.getInt(MainWindow.WINDOW_FC_WIDTH), Config.getInt(MainWindow.WINDOW_FC_HEIGHT)));
				int returnVal = fc.showOpenDialog(this);
				Config.set(MainWindow.WINDOW_FC_HEIGHT, fc.getHeight());
				Config.set(MainWindow.WINDOW_FC_WIDTH,fc.getWidth());		

				if (returnVal == JFileChooser.APPROVE_OPTION) { 
					String path = fc.getSelectedFile().getAbsolutePath();
					if (!path.equals(""))
						path = path + File.separator;
					txtArchiveDirectory.setText(path);
				} else {
					Log.println("No Selection ");
				}
		}

	}
	
	
	private int parseDelayTextField(JTextField text) {
		int value = 0;
	
		try {
			value = Integer.parseInt(text.getText());
			if (value > 2550) {
				value = 2550;
				text.setText(Integer.toString(2550));
			}
			if (value < 0) {
				value = 0;
				text.setText(Integer.toString(0));
			}
		} catch (NumberFormatException ex) {
			text.setText(Integer.toString(0));
		}
		return value;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();

		if (source == cbUploadToServer) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				//setServerPanelEnabled(false);
			} else {
				//setServerPanelEnabled(true);
			}
		}
		
		if (source == cbSendCustomBytes) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				setBytesPanelEnabled(false);
			} else {
				setBytesPanelEnabled(true);
			}
		}
		
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override


	public void focusLost(FocusEvent e) {
		if (e.getSource() == txtCallsign) {
			if (txtCallsign.getText().length() > MAX_CALLSIGN_LEN) 
				txtCallsign.setText(txtCallsign.getText().substring(0, MAX_CALLSIGN_LEN));
			enableDependentParams();
		}
		if (e.getSource() == txtTxDelay) {
			this.parseDelayTextField(txtTxDelay);
		}
		if (e.getSource() == txtStation) {
			if (txtStation.getText().length() > MAX_STATION_LEN) 
				txtStation.setText(txtStation.getText().substring(0, MAX_STATION_LEN));
		}
		if (e.getSource() == txtLatitude || e.getSource() == txtLongitude ) {
			try {
				updateLocator();
			} catch (Exception e1) {
				Log.errorDialog("ERROR", e1.getMessage());
			}
			enableDependentParams();
		}
		if (e.getSource() == txtMaidenhead) {
			try {
				updateLatLong();
			} catch (Exception e1) {
				Log.errorDialog("ERROR", e1.getMessage());
			}
			enableDependentParams();
		}
		if (e.getSource() == txtAltitude) {
			validAltitude();
			enableDependentParams();
		}

	}

@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		saveProperties();
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changedUpdate(DocumentEvent e) {

	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		Runnable update = new Runnable() {
	        @Override
	        public void run() {
	        	textAreasUpdated();
	        }
	    }; 
		SwingUtilities.invokeLater(update);
		
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		Runnable update = new Runnable() {
	        @Override
	        public void run() {
	        	textAreasUpdated();
	        }
	    }; 
		SwingUtilities.invokeLater(update);
	}		
	
	private void textAreasUpdated() {
		if (Config.getBoolean(Config.EDIT_KISS_BYTES)) {
			txtTextAtStart.setText( byteToString(txtBytesAtStart.getText()) );
			txtTextAtEnd.setText( byteToString(txtBytesAtEnd.getText()) );
		} else {
			txtBytesAtStart.setText( stringToByteString(txtTextAtStart.getText()) );
			txtBytesAtEnd.setText( stringToByteString(txtTextAtEnd.getText()) );
		}
	}
		
}
