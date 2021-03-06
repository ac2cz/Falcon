package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JLabel;

import common.Log;
import common.SpacecraftSettings;
import fileStore.DirSelectionEquation;
import common.Config;

/**
* 
* AMSAT Pacsat Ground
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
* The SpacecraftFrame is a seperate window that opens and allows the spacecraft paramaters to
* be viewed and edited.
*
*/
@SuppressWarnings("serial")
public class SpacecraftFrame extends JDialog implements ItemListener, ActionListener, FocusListener, WindowListener, MouseListener {

	private final JPanel contentPanel = new JPanel();
	JTextField name;
	JTextField telemetryDownlinkFreqkHz;
	JTextField minFreqBoundkHz;
	JTextField maxFreqBoundkHz;
	JTextField dirAge, maxHeaders;
	
	JCheckBox reqDir;
	JCheckBox reqDirHoles;
	JCheckBox reqFiles;
	JCheckBox reqFileHoles;
	JCheckBox uploadFiles,cbPsfHeaderCheckSums;

	JButton btnCancel;
	JButton btnSave, butDirSelection, butDelEquations, butDelEquation, butEditEquation;
	JTable tableEquations;
	DirEquationTableModel dirEquationTableModel;
	private JTextField txtPrimaryServer, txtNoradId,txtServerUrl;
	
	SpacecraftSettings spacecraftSettings;

	int headerSize = 12;
	
	public static final String SPACECRAFT_WINDOW_X = "spacecraft_window_x";
	public static final String SPACECRAFT_WINDOW_Y = "spacecraft_window_y";
	public static final String SPACECRAFT_WINDOW_WIDTH = "spacecraft_window_width";
	public static final String SPACECRAFT_WINDOW_HEIGHT = "spacecraft_window_height";
	
	/**
	 * Create the dialog.
	 */
	public SpacecraftFrame(SpacecraftSettings spacecraftSettings, JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Spacecraft paramaters");
		addWindowListener(this);
		this.spacecraftSettings = spacecraftSettings;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		contentPanel.setLayout(new BorderLayout(0, 0));
		addFields();
		
		addButtons();
		
	}
	
	private void addFields() {
		JPanel titlePanel = new JPanel();
		contentPanel.add(titlePanel, BorderLayout.NORTH);
		titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		TitledBorder heading0 = title("Identification");
		titlePanel.setBorder(heading0);

		//JLabel lName = new JLabel("Name: " + sat.name);
		name = addSettingsRow(titlePanel, 15, "Name", 
				"The name must be the same as the name in your TLE/Keps file if you want to calculate positions or sync with SatPC32", ""+spacecraftSettings.name);
		titlePanel.add(name);
		
		// Left Column - Fixed Params that can not be changed
		JPanel leftPanel = new JPanel();
		contentPanel.add(leftPanel, BorderLayout.WEST);
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		
		JPanel leftFixedPanel = new JPanel();
		JPanel leftFixedPanelf = new JPanel();
		leftFixedPanelf.setLayout(new BorderLayout());
		leftFixedPanel.setLayout(new BoxLayout(leftFixedPanel, BoxLayout.Y_AXIS));
		leftPanel.add(leftFixedPanelf);
		leftFixedPanelf.add(leftFixedPanel, BorderLayout.NORTH);
		
		TitledBorder heading = title("Fixed Paramaters");
		leftFixedPanel.setBorder(heading);
		
		JLabel bbsCall = new JLabel("BBS Callsign: " + spacecraftSettings.get(SpacecraftSettings.BBS_CALLSIGN));
		leftFixedPanel.add(bbsCall);
		JLabel broadcastCall = new JLabel("Broadcast Callsign: " + spacecraftSettings.get(SpacecraftSettings.BROADCAST_CALLSIGN));
		leftFixedPanel.add(broadcastCall);
		JLabel digiCall = new JLabel("Digi-peter Callsign: " + spacecraftSettings.get(SpacecraftSettings.DIGI_CALLSIGN));
		leftFixedPanel.add(digiCall);
		leftFixedPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(250,200), new Dimension(500,500)));
		
		txtServerUrl = addSettingsRow(leftFixedPanel, 20, "Website", "This sets the URL we use to fetch and download server data. "
				+ "Should not need to be changed", spacecraftSettings.get(SpacecraftSettings.TELEM_SERVER));
		
		txtPrimaryServer = addSettingsRow(leftFixedPanel, 20, "Telem Server", "The address of the Telemetry server. "
					+ "Should not need to be changed", spacecraftSettings.get(SpacecraftSettings.TELEM_SERVER));
		txtNoradId = addSettingsRow(leftFixedPanel, 15, "Norad Id", "The id issued by Norad for this spacecraft or a temporary number from SatNogs"
				+ "", spacecraftSettings.get(SpacecraftSettings.NORAD_ID));
		//leftPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(250,500), new Dimension(500,500)));
		

		// Right Column - Things the user can change - e.g. Layout Files, Freq, Tracking etc
		JPanel rightPanel = new JPanel();
		contentPanel.add(rightPanel, BorderLayout.CENTER);
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		
		JPanel rightPanel1 = new JPanel();
		rightPanel.add(rightPanel1);
		rightPanel1.setLayout(new BoxLayout(rightPanel1, BoxLayout.Y_AXIS));
		
		TitledBorder heading2 = title("Pass Actions");
		rightPanel1.setBorder(heading2);
//				
//		telemetryDownlinkFreqkHz = addSettingsRow(rightPanel1, 15, "Downlink Freq (kHz)", 
//				"The nominal downlink frequency of the spacecraft", ""); //+sat.telemetryDownlinkFreqkHz);
//		minFreqBoundkHz = addSettingsRow(rightPanel1, 15, "Lower Freq Bound (kHz)", 
//				"The lower frequency boundry when we are searching for the spacecraft signal", ""+sat.minFreqBoundkHz);
//		maxFreqBoundkHz = addSettingsRow(rightPanel1, 15, "Upper Freq Bound (kHz)", 
//				"The upper frequency boundry when we are searching for the spacecraft signal", ""+sat.maxFreqBoundkHz);
		reqDir = addCheckBoxRow("Request Directory", "Request the directoty at the start of each pass", spacecraftSettings.getBoolean(SpacecraftSettings.REQ_DIRECTORY), rightPanel1 );
		reqDirHoles = addCheckBoxRow("Fill Directory Holes", "Request holes in the directory", spacecraftSettings.getBoolean(SpacecraftSettings.FILL_DIRECTORY_HOLES), rightPanel1 );
		reqFiles = addCheckBoxRow("Request Files", "Request files that are marked for download", spacecraftSettings.getBoolean(SpacecraftSettings.REQ_FILES), rightPanel1 );
		uploadFiles = addCheckBoxRow("Upload Files", "Upload files to the server", spacecraftSettings.getBoolean(SpacecraftSettings.UPLOAD_FILES), rightPanel1 );
		if (!spacecraftSettings.getBoolean(SpacecraftSettings.SUPPORTS_FILE_UPLOAD)) {
			uploadFiles.setSelected(false);
			uploadFiles.setEnabled(false);
		}
		cbPsfHeaderCheckSums = addCheckBoxRow("Check PSF Header Checksums", "Warn about files where the header or body checksums do not match",
				spacecraftSettings.getBoolean(SpacecraftSettings.PSF_HEADER_CHECK_SUMS), rightPanel1 );

		cbPsfHeaderCheckSums.setEnabled(false);
		JPanel rightPanel3 = new JPanel();
		rightPanel1.add(rightPanel3);
		rightPanel3.add(new Box.Filler(new Dimension(10,10), new Dimension(10,10), new Dimension(4000,500)));

		JPanel rightPanel2 = new JPanel();
		rightPanel.add(rightPanel2);
		rightPanel2.setLayout(new BoxLayout(rightPanel2, BoxLayout.Y_AXIS));
		
		TitledBorder heading3 = title("Directory Settings");
		rightPanel2.setBorder(heading3);

		dirAge = addSettingsRow(rightPanel2, 25, "Oldest Files (days)", 
				"The number of days back in time to request file headers when building the directory or filling holes", ""+spacecraftSettings.get(SpacecraftSettings.DIR_AGE));

		maxHeaders = addSettingsRow(rightPanel2, 25, "Archive limit (headers)", 
				"The maximum number of headers to keep when the archive is run", ""+spacecraftSettings.getInt(SpacecraftSettings.NUMBER_DIR_TABLE_ENTRIES));

		
		dirEquationTableModel = new DirEquationTableModel();
		tableEquations = new JTable(dirEquationTableModel);
		JScrollPane scrollPane = new JScrollPane(tableEquations);
		rightPanel2.add(scrollPane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tableEquations.setFillsViewportHeight(true);
		tableEquations.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN );

		tableEquations.removeColumn(tableEquations.getColumnModel().getColumn(0)); // hide the key
		
		JPanel eqButtonsPanel = new JPanel();
		rightPanel2.add(eqButtonsPanel);
	
		butDirSelection = new JButton("Add");
		butEditEquation = new JButton("Edit");
		butDelEquation = new JButton("Delete");
		butDelEquations = new JButton("Delete All");
		
		eqButtonsPanel.add(butDirSelection);
		butDirSelection.addActionListener(this);
		eqButtonsPanel.add(butEditEquation);
		butEditEquation.addActionListener(this);
		eqButtonsPanel.add(butDelEquation);
		butDelEquation.addActionListener(this);
		eqButtonsPanel.add(butDelEquations);
		butDelEquations.addActionListener(this);
		
		updateDirEquations();
		
		rightPanel2.add(new Box.Filler(new Dimension(10,10), new Dimension(10,10), new Dimension(400,500)));
		
		// Bottom panel for description
		JPanel footerPanel = new JPanel();
		TitledBorder heading9 = title("Description");
		footerPanel.setBorder(heading9);
		footerPanel.setLayout(new BorderLayout());

		JTextArea taDesc = new JTextArea(2, 45);
		taDesc.setText(spacecraftSettings.get(SpacecraftSettings.DESCRIPTION));
		taDesc.setLineWrap(true);
		taDesc.setWrapStyleWord(true);
		taDesc.setEditable(false);
		taDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
		footerPanel.add(taDesc, BorderLayout.CENTER);
		contentPanel.add(footerPanel, BorderLayout.SOUTH);
	}
	
	public void updateDirEquations() {
		String[][] data = spacecraftSettings.directory.getEquationsData();
		dirEquationTableModel.setData(data);
	}
	
	private TitledBorder title(String s) {
		TitledBorder title = new TitledBorder(null, s, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		return title;
	}
	
	private void addButtons() {
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		btnSave = new JButton("Save");
		btnSave.setActionCommand("Save");
		buttonPane.add(btnSave);
		btnSave.addActionListener(this);
		getRootPane().setDefaultButton(btnSave);


		btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("Cancel");
		buttonPane.add(btnCancel);
		btnCancel.addActionListener(this);

	}

	private JCheckBox addCheckBoxRow(String name, String tip, boolean value, JPanel parent) {
		JCheckBox checkBox = new JCheckBox(name);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		parent.add(checkBox);
		if (value) checkBox.setSelected(true); else checkBox.setSelected(false);
		return checkBox;
	}


	private JTextField addSettingsRow(JPanel column, int length, String name, String tip, String value) {
		JPanel panel = new JPanel();
		column.add(panel);
		panel.setLayout(new GridLayout(1,2,0,5));
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

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == btnCancel) {
			this.dispose();
		}
		if (e.getSource() == butDirSelection) {
			DirEquationFrame f = new DirEquationFrame(spacecraftSettings, (JFrame) this.getParent(), true, this);
			f.setVisible(true);
		}
		if (e.getSource() == butEditEquation) {
			int row = tableEquations.getSelectedRow();
			if (row >= 0 && row < tableEquations.getRowCount()) {
				String id = (String) tableEquations.getModel().getValueAt(tableEquations.getSelectedRow(),0);
				DirSelectionEquation equation = spacecraftSettings.directory.getEquation(id);
				DirEquationFrame f = new DirEquationFrame(spacecraftSettings, (JFrame) this.getParent(), true, this, equation);
				f.setVisible(true);
			}
		}
		if (e.getSource() == butDelEquation) {
			int row = tableEquations.getSelectedRow();
			if (row >= 0 && row < tableEquations.getRowCount())
				deleteRow(tableEquations, row);
		}
		if (e.getSource() == butDelEquations) {
			try {
				spacecraftSettings.directory.deleteEquations();
				updateDirEquations();
//				taEquations.setText(Config.spacecraft.directory.getEquationsString());

			} catch (IOException e1) {
				Log.errorDialog("ERROR", "Could not remove the equation file: " + e1.getMessage());
			}
		}
		if (e.getSource() == btnSave) {
			boolean dispose = true;
			try {
				try {
					int age = Integer.parseInt(dirAge.getText());
					if (age < 0) {
						dirAge.setText(""+1);
						throw new NumberFormatException("The Directory Age must contain a valid number from 1-"+SpacecraftSettings.MAX_DIR_AGE);
					}
					if (age > SpacecraftSettings.MAX_DIR_AGE) {
						dirAge.setText(""+SpacecraftSettings.MAX_DIR_AGE);
						throw new NumberFormatException("The Directory Age must contain a valid number from 1-"+SpacecraftSettings.MAX_DIR_AGE);
					}
					spacecraftSettings.set(SpacecraftSettings.DIR_AGE, age);
				} catch (NumberFormatException ex) {
					throw new NumberFormatException("The Directory Age must contain a valid number from 1-"+SpacecraftSettings.MAX_DIR_AGE);
				}
				
				try {
					int num = Integer.parseInt(maxHeaders.getText());
					if (num < 0) {
						maxHeaders.setText(""+1);
						throw new NumberFormatException("The Number of headers must contain a valid number from 1-"+SpacecraftSettings.MAX_DIR_TABLE_ENTRIES);
					}
					if (num > SpacecraftSettings.MAX_DIR_TABLE_ENTRIES) {
						maxHeaders.setText(""+SpacecraftSettings.MAX_DIR_TABLE_ENTRIES);
						throw new NumberFormatException("The Number of headers must contain a valid number from 1-"+SpacecraftSettings.MAX_DIR_TABLE_ENTRIES);
					}
					spacecraftSettings.set(SpacecraftSettings.NUMBER_DIR_TABLE_ENTRIES, num);
				} catch (NumberFormatException ex) {
					throw new NumberFormatException("The Number of headers must contain a valid number from 1-"+SpacecraftSettings.MAX_DIR_TABLE_ENTRIES);
				}

				if (dispose) {
					spacecraftSettings.set(SpacecraftSettings.REQ_DIRECTORY, reqDir.isSelected());
					spacecraftSettings.set(SpacecraftSettings.FILL_DIRECTORY_HOLES, reqDirHoles.isSelected());
					spacecraftSettings.set(SpacecraftSettings.REQ_FILES, reqFiles.isSelected());
					spacecraftSettings.set(SpacecraftSettings.UPLOAD_FILES, uploadFiles.isSelected());
					
					spacecraftSettings.set(SpacecraftSettings.TELEM_SERVER,txtPrimaryServer.getText());
					spacecraftSettings.set(SpacecraftSettings.NORAD_ID,txtNoradId.getText());
					spacecraftSettings.set(SpacecraftSettings.WEB_SITE_URL,txtServerUrl.getText());

					spacecraftSettings.save();
					this.dispose();
					// run the equations by refreshing the dir
					if (spacecraftSettings.directory.getTableData().length > 0)
						if (Config.mainWindow != null)
							Config.mainWindow.setDirectoryData(spacecraftSettings.name, spacecraftSettings.directory.getTableData());
				}
			} catch (NumberFormatException Ex) {
				Log.errorDialog("Invalid Paramaters", Ex.getMessage());
			}
		}

	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void saveProperties() {
		Config.set(SPACECRAFT_WINDOW_HEIGHT, this.getHeight());
		Config.set(SPACECRAFT_WINDOW_WIDTH, this.getWidth());
		Config.set(SPACECRAFT_WINDOW_X, this.getX());
		Config.set(SPACECRAFT_WINDOW_Y, this.getY());
		
		Config.save();
	}
	
	public void loadProperties() {
		if (Config.getInt(SPACECRAFT_WINDOW_X) == 0) {
			Config.set(SPACECRAFT_WINDOW_X, 100);
			Config.set(SPACECRAFT_WINDOW_Y, 100);
			Config.set(SPACECRAFT_WINDOW_HEIGHT, 700);
			Config.set(SPACECRAFT_WINDOW_WIDTH, 625);
		}
		setBounds(Config.getInt(SPACECRAFT_WINDOW_X), Config.getInt(SPACECRAFT_WINDOW_Y), 
				Config.getInt(SPACECRAFT_WINDOW_WIDTH), Config.getInt(SPACECRAFT_WINDOW_HEIGHT));
	}
	
	@Override
	public void windowClosed(WindowEvent arg0) {
		saveProperties();
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
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

	protected void deleteRow(JTable table, int row) {
		String id = (String) table.getValueAt(row, 0);
		Log.println("Delete for: " +id);
		id = (String) table.getModel().getValueAt(table.getSelectedRow(),0);
		Log.println("Delete Key: " +id);
		spacecraftSettings.directory.deleteEquation(id);
		updateDirEquations();
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
