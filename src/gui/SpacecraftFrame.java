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
import javax.swing.table.TableColumn;
import javax.swing.JLabel;

import common.LayoutLoadException;
import common.Log;
import common.Spacecraft;
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
public class SpacecraftFrame extends JDialog implements ItemListener, ActionListener, FocusListener, WindowListener {

	private final JPanel contentPanel = new JPanel();
	JTextField name;
	JTextField telemetryDownlinkFreqkHz;
	JTextField minFreqBoundkHz;
	JTextField maxFreqBoundkHz;
	JTextField dirAge;
	
	JCheckBox track;

	JButton btnCancel;
	JButton btnSave, butDirSelection;
	
	Spacecraft sat;

	int headerSize = 12;
	
	public static final String SPACECRAFT_WINDOW_X = "spacecraft_window_x";
	public static final String SPACECRAFT_WINDOW_Y = "spacecraft_window_y";
	public static final String SPACECRAFT_WINDOW_WIDTH = "spacecraft_window_width";
	public static final String SPACECRAFT_WINDOW_HEIGHT = "spacecraft_window_height";
	
	/**
	 * Create the dialog.
	 */
	public SpacecraftFrame(Spacecraft sat, JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Spacecraft paramaters");
		addWindowListener(this);
		this.sat = sat;
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
				"The name must be the same as the name in your TLE/Keps file if you want to calculate positions or sync with SatPC32", ""+sat.name);
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
		
		JLabel bbsCall = new JLabel("BBS Callsign: " + sat.get(Spacecraft.BBS_CALLSIGN));
		leftFixedPanel.add(bbsCall);
		JLabel broadcastCall = new JLabel("Broadcast Callsign: " + sat.get(Spacecraft.BROADCAST_CALLSIGN));
		leftFixedPanel.add(broadcastCall);
		JLabel digiCall = new JLabel("Digi-peter Callsign: " + sat.get(Spacecraft.DIGI_CALLSIGN));
		leftFixedPanel.add(digiCall);
		leftFixedPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(250,200), new Dimension(500,500)));
		
		
		//leftPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(250,500), new Dimension(500,500)));
		

		// Right Column - Things the user can change - e.g. Layout Files, Freq, Tracking etc
		JPanel rightPanel = new JPanel();
		contentPanel.add(rightPanel, BorderLayout.CENTER);
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		
		JPanel rightPanel1 = new JPanel();
		rightPanel.add(rightPanel1);
		rightPanel1.setLayout(new BoxLayout(rightPanel1, BoxLayout.Y_AXIS));
		
//		TitledBorder heading2 = title("Frequency and Tracking");
//		rightPanel1.setBorder(heading2);
//				
//		telemetryDownlinkFreqkHz = addSettingsRow(rightPanel1, 15, "Downlink Freq (kHz)", 
//				"The nominal downlink frequency of the spacecraft", ""); //+sat.telemetryDownlinkFreqkHz);
//		minFreqBoundkHz = addSettingsRow(rightPanel1, 15, "Lower Freq Bound (kHz)", 
//				"The lower frequency boundry when we are searching for the spacecraft signal", ""+sat.minFreqBoundkHz);
//		maxFreqBoundkHz = addSettingsRow(rightPanel1, 15, "Upper Freq Bound (kHz)", 
//				"The upper frequency boundry when we are searching for the spacecraft signal", ""+sat.maxFreqBoundkHz);
//		track = addCheckBoxRow("Track when Find Signal Enabled", "When Find Signal is enabled include this satellite in the search", sat.track, rightPanel1 );
//		rightPanel1.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		JPanel rightPanel2 = new JPanel();
		rightPanel.add(rightPanel2);
		rightPanel2.setLayout(new BoxLayout(rightPanel2, BoxLayout.Y_AXIS));
		
		TitledBorder heading3 = title("Directory Settings");
		rightPanel2.setBorder(heading3);

		dirAge = addSettingsRow(rightPanel2, 25, "Oldest Files (days)", 
				"The number of days back in time to request file headers when building the directory or filling holes", ""+sat.get(Spacecraft.DIR_AGE));

		butDirSelection = new JButton("Auto Select");
		rightPanel2.add(butDirSelection);
		butDirSelection.addActionListener(this);
		
		rightPanel2.add(new Box.Filler(new Dimension(10,10), new Dimension(400,400), new Dimension(400,500)));

		
		// Bottom panel for description
		JPanel footerPanel = new JPanel();
		TitledBorder heading9 = title("Description");
		footerPanel.setBorder(heading9);

		JTextArea taDesc = new JTextArea(2, 45);
		taDesc.setText(sat.get(Spacecraft.DESCRIPTION));
		taDesc.setLineWrap(true);
		taDesc.setWrapStyleWord(true);
		taDesc.setEditable(false);
		taDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
		footerPanel.add(taDesc);
		contentPanel.add(footerPanel, BorderLayout.SOUTH);
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
		JPanel box = new JPanel();
		box.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		JCheckBox checkBox = new JCheckBox(name);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		box.add(checkBox);
		parent.add(box);
		if (value) checkBox.setSelected(true); else checkBox.setSelected(false);
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

	@Override
	public void actionPerformed(ActionEvent e) {
		boolean restartTNC = false;
		
		if (e.getSource() == btnCancel) {
			this.dispose();
		}
		if (e.getSource() == butDirSelection) {
			DirSelectionFrame f = new DirSelectionFrame(sat, (JFrame) this.getParent(), true);
			f.setVisible(true);
		}
		
		if (e.getSource() == btnSave) {
			boolean dispose = true;
			int downlinkFreq = 0;
			int minFreq = 0;
			int maxFreq = 0;
			try {
				try {
					int age = Integer.parseInt(dirAge.getText());
					if (age < 0) {
						dirAge.setText(""+1);
						throw new NumberFormatException("The Directory Age must contain a valid number from 1-"+Spacecraft.MAX_DIR_AGE);
					}
					if (age > Spacecraft.MAX_DIR_AGE) {
						dirAge.setText(""+Spacecraft.MAX_DIR_AGE);
						throw new NumberFormatException("The Directory Age must contain a valid number from 1-"+Spacecraft.MAX_DIR_AGE);
					}
					sat.set(Spacecraft.DIR_AGE, age);
//					minFreq = Integer.parseInt(minFreqBoundkHz.getText());
//					maxFreq = Integer.parseInt(maxFreqBoundkHz.getText());
				} catch (NumberFormatException ex) {
					throw new NumberFormatException("The Directory Age must contain a valid number from 1-"+Spacecraft.MAX_DIR_AGE);
				}
//				if (minFreq < maxFreq) {
//					sat.telemetryDownlinkFreqkHz = downlinkFreq;
//					sat.minFreqBoundkHz = minFreq;
//					sat.maxFreqBoundkHz = maxFreq;
//				} else {
//					Log.errorDialog("ERROR", "Lower Frequency Bound must be less than Upper Frequency Bound");
//					dispose = false;
//				}
		
				//sat.track = track.isSelected();

				if (dispose) {
					sat.save();
					Config.init();
					this.dispose();
					if (restartTNC)
						Config.mainWindow.initDecoder();
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
			Config.set(SPACECRAFT_WINDOW_HEIGHT, 400);
			Config.set(SPACECRAFT_WINDOW_WIDTH, 600);
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
}
