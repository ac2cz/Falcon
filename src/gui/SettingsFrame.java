package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JButton;

import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import common.Config;
import common.LayoutLoadException;
import common.Log;
import pacSat.TncDecoder;

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
public class SettingsFrame extends JDialog implements ActionListener, ItemListener, FocusListener, WindowListener {

	public static final int MAX_CALLSIGN_LEN = 6;
	public static final int MAX_STATION_LEN = 50;
	
	public static final String SETTINGS_WINDOW_X = "settings_window_x";
	public static final String SETTINGS_WINDOW_Y = "settings_window_y";
	public static final String SETTINGS_WINDOW_WIDTH = "settings_window_width";
	public static final String SETTINGS_WINDOW_HEIGHT = "settings_window_height";
	
	private JPanel contentPane;
	private JTextField txtLogFileDirectory;
	private JTextField txtCallsign;
	private JTextField txtAltitude;
//	private JTextField txtTncComPort;
	private JComboBox cbTncComPort;
	boolean useUDP;
	
	private JPanel serverPanel;
	
	JButton btnSave;
	JButton btnCancel;
	JButton btnBrowse;
		
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
		JPanel northpanel2 = new JPanel();
		JPanel northpanelA = new JPanel();
		JPanel northpanelB = new JPanel();
		northpanel.add(northpanel1, BorderLayout.NORTH);
		northpanel1.setLayout(new BorderLayout());
		northpanel1.add(northpanelA, BorderLayout.NORTH);
		northpanel1.add(northpanelB, BorderLayout.SOUTH);
		northpanel.add(northpanel2, BorderLayout.SOUTH);
		
		northpanel2.setLayout(new BorderLayout());
		northpanelA.setLayout(new BorderLayout());
		northpanelB.setLayout(new BorderLayout());
		
		JLabel lblHomeDir = new JLabel("Home directory     ");
		lblHomeDir.setToolTipText("This is the directory that contains the settings file");
		lblHomeDir.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanelA.add(lblHomeDir, BorderLayout.WEST);

		JLabel lblHomeDir2 = new JLabel(Config.get(Config.HOME_DIR));
		northpanelA.add(lblHomeDir2, BorderLayout.CENTER);

		
		JLabel lblLogFilesDir = new JLabel("Log files directory");
		lblLogFilesDir.setToolTipText("This sets the directory that the downloaded telemetry data is stored in");
		lblLogFilesDir.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanel2.add(lblLogFilesDir, BorderLayout.WEST);
		
		txtLogFileDirectory = new JTextField(Config.get(Config.LOGFILE_DIR));
		northpanel2.add(txtLogFileDirectory, BorderLayout.CENTER);
		txtLogFileDirectory.setColumns(30);
		
		txtLogFileDirectory.addActionListener(this);

		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(this);
		northpanel2.add(btnBrowse, BorderLayout.EAST);
		
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
				"Ground station name is the unique identifier that you will use to store data on the AMSAT telemetry server", Config.get(Config.CALLSIGN));
		
/*		txtPrimaryServer = addSettingsRow(serverPanel, 15, "Primary Server", "The address of the Amsat Telemetry server. "
				+ "Should not need to be changed", Config.primaryServer);
		txtSecondaryServer = addSettingsRow(serverPanel, 15, "Secondary Server", "The backup address of the Amsat Telemetry server. "
				+ "Should not need to be changed",Config.secondaryServer);
		txtLatitude = addSettingsRow(serverPanel, 10, "Lat (S is -ve)", "Latitude / Longitude or Locator need to be specified if you supply decoded data to AMSAT", Config.latitude); // South is negative
		txtLongitude = addSettingsRow(serverPanel, 10, "Long (W is -ve)", "Latitude / Longitude or Locator need to be specified if you supply decoded data to AMSAT", Config.longitude); // West is negative
		JPanel locatorPanel = new JPanel();
		JLabel lblLoc = new JLabel("Lat Long gives Locator: ");
		txtMaidenhead = new JTextField(Config.maidenhead);
		txtMaidenhead.addActionListener(this);
		txtMaidenhead.addFocusListener(this);

		txtMaidenhead.setColumns(10);
		serverPanel.add(locatorPanel);
		locatorPanel.add(lblLoc);
		locatorPanel.add(txtMaidenhead);

		txtAltitude = addSettingsRow(serverPanel, 15, "Altitude (m)", "Altitude will be supplied to AMSAT along with your data if you specify it", Config.altitude);
		txtStation = addSettingsRow(serverPanel, 15, "RF-Receiver Description", "RF-Receiver can be specified to give us an idea of the types of stations that are in operation", Config.stationDetails);
	*/	
		serverPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));
		
		JPanel leftcolumnpanel2 = addColumn(leftcolumnpanel,6);
		TitledBorder eastTitle3 = title("Formatting");
		leftcolumnpanel2.setBorder(eastTitle3);

		/*
		txtDisplayModuleFontSize = addSettingsRow(leftcolumnpanel2, 5, "Health Module Font Size", 
				"Change the size of the font on the Satellite tabs so that it is more readable or so that it fits in the space available", Integer.toString(Config.displayModuleFontSize));
		txtGraphAxisFontSize = addSettingsRow(leftcolumnpanel2, 5, "Graph Font Size", "Change the size of the font on the graph axis", Integer.toString(Config.graphAxisFontSize));
*/
		
		JPanel leftcolumnpanel3 = addColumn(leftcolumnpanel,6);
		TitledBorder measureTitle = title("TNC");
		leftcolumnpanel3.setBorder(measureTitle);
		
		
		cbTncComPort = addComboBoxRow(leftcolumnpanel3, "Com Port", 
				"The Serial Port (virtual or otherwise) that your TNC is on", TncDecoder.getSerialPorts());
		cbTncComPort.setSelectedIndex(Config.getInt(Config.TNC_COM_PORT));
		
		leftcolumnpanel3.add(new Box.Filler(new Dimension(200,10), new Dimension(150,400), new Dimension(500,500)));
		
		// Add a right column with two panels in it
		JPanel rightcolumnpanel = new JPanel();
		centerpanel.add(rightcolumnpanel);
		//rightcolumnpanel.setLayout(new GridLayout(2,1,10,10));
		rightcolumnpanel.setLayout(new BoxLayout(rightcolumnpanel, BoxLayout.Y_AXIS));
		
		JPanel rightcolumnpanel0 = addColumn(rightcolumnpanel,3);
		rightcolumnpanel0.setLayout(new BoxLayout(rightcolumnpanel0, BoxLayout.Y_AXIS));
		TitledBorder eastTitle4 = title("Decoder Options");
		rightcolumnpanel0.setBorder(eastTitle4);
		
//		cbUploadToServer = addCheckBoxRow("Upload to Server", "Select this if you want to send your collected data to the AMSAT telemetry server",
//				Config.uploadToServer, rightcolumnpanel0 );
//		rdbtnTrackSignal = addCheckBoxRow("Track Doppler","Leave this on except in a test situation.  It allows FoxTelem to follow the spacecraft downllink signal",
//				Config.trackSignal, rightcolumnpanel0);
//		useUDP = true;
//		if (Config.serverProtocol == TlmServer.TCP)
//			useUDP = false;
//		cbUseUDP = addCheckBoxRow("Use UDP", "Use UDP (vs TCP) to send data to the AMSAT telemetry server",
//				useUDP, rightcolumnpanel0 );
//		storePayloads = addCheckBoxRow("Store Payloads", "Uncheck this if you do not want to store the decoded payloads on disk", Config.storePayloads, rightcolumnpanel0 );
//		saveFcdParams = addCheckBoxRow("Store FCD Params", "Save the FCD settings to disk and restore at start up.  May conflict with other programs or other copies of FoxTelem.", Config.saveFcdParams, rightcolumnpanel0 );
//		useLeftStereoChannel = addCheckBoxRow("Use Left Stereo Channel", "The default is for FoxTelem to read audio from the left stereo channel of your soundcard.  "
//				+ "If you uncheck this it will read from the right",
//				Config.useLeftStereoChannel, rightcolumnpanel0 );
//		swapIQ = addCheckBoxRow("Swap IQ", "Swap the I and Q channels in IQ deocder mode",
//				Config.swapIQ, rightcolumnpanel0 );
//		insertMissingBits = addCheckBoxRow("Fix Dropped Bits", "Fix bits dropped in the audio channel (may fix frames but use more CPU)",
//				Config.insertMissingBits, rightcolumnpanel0 );
		rightcolumnpanel0.add(new Box.Filler(new Dimension(10,10), new Dimension(150,400), new Dimension(500,500)));
		
		rightcolumnpanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));


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
			Config.set(SETTINGS_WINDOW_WIDTH, 560);
			Config.set(SETTINGS_WINDOW_HEIGHT, 350);
		}
		setBounds(Config.getInt(SETTINGS_WINDOW_X), Config.getInt(SETTINGS_WINDOW_Y), 
				Config.getInt(SETTINGS_WINDOW_WIDTH), Config.getInt(SETTINGS_WINDOW_HEIGHT));
	}
	
	private TitledBorder title(String s) {
		TitledBorder title = new TitledBorder(null, s, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		return title;
	}
	
	
	private boolean validCallsign() {
		if (txtCallsign.getText().equalsIgnoreCase(Config.DEFAULT_CALLSIGN) || 
				txtCallsign.getText().equals("")) return false;
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
		JComboBox checkBox = new JComboBox(values);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		parent.add(checkBox);
		return checkBox;
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
		
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			this.dispose();
		}
		if (e.getSource() == btnSave) {
			boolean dispose = true;
			// grab all the latest settings
				Config.set(Config.CALLSIGN, txtCallsign.getText().toUpperCase());
				Log.println("Setting callsign: " + Config.get(Config.CALLSIGN));
				
				Config.set(Config.TNC_COM_PORT, TncDecoder.portNames[cbTncComPort.getSelectedIndex()]);

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
							String prev = Config.get(Config.LOGFILE_DIR);
							Config.set(Config.LOGFILE_DIR,txtLogFileDirectory.getText());
							Log.println("Setting log file directory to: " + Config.get(Config.LOGFILE_DIR));
							Config.close();
							try {
								Config.init();
							} catch (LayoutLoadException e1) {
								Log.errorDialog("ERROR", "Could not switch directories due to an error loading the spacecraft settings.\n"
										+ "Try restarting PacSat Ground\n" + e1.getMessage());
								Config.set(Config.LOGFILE_DIR,prev);
							} catch (IOException e1) {
								Log.errorDialog("ERROR", "Could not switch directories. File read/write error.\n"
										+ "Try restarting PacSat Ground\n" + e1.getMessage());
								Config.set(Config.LOGFILE_DIR,prev);
							}
							
						}
					}		
			}
			
			if (dispose) {
				Config.save();
				Config.mainWindow.setDirectoryData(Config.spacecraft.directory.getTableData());
				this.dispose();
			}
		}

		if (e.getSource() == btnBrowse) {
			File dir = null;
			if (!Config.get(Config.LOGFILE_DIR).equalsIgnoreCase("")) {
				dir = new File(Config.get(Config.LOGFILE_DIR));
			}
			if(Config.getBoolean(Config.USE_NATIVE_FILE_CHOOSER) && !Config.isWindowsOs()) { // not on windows because native dir chooser does not work) {
				// use the native file dialog on the mac
				System.setProperty("apple.awt.fileDialogForDirectories", "true");
				FileDialog fd =
						new FileDialog(this, "Choose Directory for Log Files",FileDialog.LOAD);
				if (dir != null) {
					fd.setDirectory(dir.getAbsolutePath());
				}
				fd.setVisible(true);
				System.setProperty("apple.awt.fileDialogForDirectories", "false");
				String filename = fd.getFile();
				String dirname = fd.getDirectory();
				if (filename == null)
					Log.println("You cancelled the choice");
				else {
					Log.println("File: " + filename);
					Log.println("DIR: " + dirname);
					File selectedFile = new File(dirname + filename);
					String path = selectedFile.getAbsolutePath();
					if (!path.equals(""))
						path = path + File.separator;
					txtLogFileDirectory.setText(path);
				}
				
			} else {

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
					System.out.println("No Selection ");
				}
			}
		}

	}
	
	
	private int parseIntTextField(JTextField text) {
		int value = 0;
	
		try {
			value = Integer.parseInt(text.getText());
			if (value > 30) {
				value = 30;
				text.setText(Integer.toString(30));
			}
		} catch (NumberFormatException ex) {
			
		}
		return value;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		e.getItemSelectable();

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
		
}
