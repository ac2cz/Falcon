package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import common.Config;
import common.Log;

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
 */
@SuppressWarnings("serial")
public class InitalSettings extends JDialog implements ActionListener, WindowListener {

	private JPanel contentPane;
//	private JPanel buttons;
	private JPanel directories;
	
	//JRadioButton typical;
	//JRadioButton custom;

	private JTextField txtLogFileDirectory;
	JLabel title;
	JLabel lab;
	JLabel lab2;
	JLabel lab3;
	JLabel lab4;
	JTextField txtCallsign;
	
	JButton btnContinue;
	JButton btnCancel;
	JButton btnBrowse;
	
	public InitalSettings(JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Welcome to the AMSAT Pacsat Ground Station");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		//setBounds(100, 100, 650, 200);
		//this.setResizable(false);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
	
		JPanel top = new JPanel();
		contentPane.add(top, BorderLayout.NORTH);
		JPanel center = new JPanel();
		contentPane.add(center, BorderLayout.CENTER);
		JPanel bottom = new JPanel();
		contentPane.add(bottom, BorderLayout.SOUTH);
		
		title = new JLabel();
		lab = new JLabel();
		lab2 = new JLabel();
		lab3 = new JLabel();
		lab4 = new JLabel();
		title.setFont(new Font("SansSerif", Font.BOLD, 14));
		
		title.setText("AMSAT PacsatGround");
		lab.setText("It looks like this is the first time you have run the ground station. ");
		lab2.setText("Configuration settings will be saved in:  " + Config.homeDir);
		lab3.setText("Enter your callsign and a folder below, where the directory and downloaded files will be stored.");
		lab4.setText("You will also need to configure the connection to a software or hardware TNC on the File > Settings menu ");
		
		JPanel titlePanel = new JPanel();
		top.setLayout(new BorderLayout(0, 0));
		top.add(titlePanel,BorderLayout.NORTH);
		titlePanel.add(title);
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		top.add(textPanel, BorderLayout.CENTER);
		textPanel.add(lab);
		textPanel.add(lab2);
		textPanel.add(new JLabel(" "));
		textPanel.add(lab3);
		textPanel.add(lab4);
		textPanel.add(new JLabel(" "));
		JPanel callPanel = new JPanel();
		textPanel.add(callPanel);
		callPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JLabel lblCall = new JLabel("Ground station Callsign");
		txtCallsign = new JTextField();
		txtCallsign.setColumns(15);
		callPanel.add(lblCall);
		callPanel.add(txtCallsign);
		directories = FilesPanel();
		directories.setVisible(true);
		center.add(directories);
		
		bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		btnContinue = new JButton("Continue");
		btnContinue.addActionListener(this);
		bottom.add(btnContinue);
		
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		bottom.add(btnCancel);

		pack();
		setLocationRelativeTo(null);
	}
	
	private JPanel FilesPanel() {
		JPanel northpanel = new JPanel();
		//JPanel northcolumnpanel = addColumn(northpanel);
		//txtLogFileDirectory = addSettingsRow(northpanel, 20, "Log files directory", Config.logFileDirectory);
		//JPanel panel = new JPanel();
		//northpanel.add(panel);
		northpanel.setLayout(new BorderLayout());
		
		JLabel lblDisplayModuleFont = new JLabel("Data Folder");
		lblDisplayModuleFont.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanel.add(lblDisplayModuleFont, BorderLayout.WEST);
		txtLogFileDirectory = new JTextField(Config.get(Config.LOGFILE_DIR));
		northpanel.add(txtLogFileDirectory, BorderLayout.CENTER);
		txtLogFileDirectory.setColumns(30);
		
		txtLogFileDirectory.addActionListener(this);

		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(this);
		northpanel.add(btnBrowse, BorderLayout.EAST);
		if (!Config.get(Config.LOGFILE_DIR).equalsIgnoreCase("")) {
			txtLogFileDirectory.setEnabled(false);
			btnBrowse.setEnabled(false);
		}
		//TitledBorder title = new TitledBorder(null, "Files and Directories", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		//northpanel.setBorder(title);
		
		return northpanel;

	}
		
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			System.exit(1);
		}
		if (e.getSource() == btnContinue) {
				// We are done
				saveAndExit();
		}

		if (e.getSource() == btnBrowse) {
			File dir = null;
			if (!Config.get(Config.LOGFILE_DIR).equalsIgnoreCase("")) {
				dir = new File(Config.get(Config.LOGFILE_DIR));
			}
//			if(Config.getBoolean(Config.USE_NATIVE_FILE_CHOOSER) && Config.isWindowsOs()) { // not on windows because native dir chooser does not work on linux/mac
//				// use the native file dialog on the mac
//				System.setProperty("apple.awt.fileDialogForDirectories", "true");
//				FileDialog fd =
//						new FileDialog(this, "Choose Directory for Log Files",FileDialog.LOAD);
//				if (dir != null) {
//					fd.setDirectory(dir.getAbsolutePath());
//				}
//				fd.setVisible(true);
//				System.setProperty("apple.awt.fileDialogForDirectories", "false");
//				
//				String filename = fd.getFile();
//				String dirname = fd.getDirectory();
//				if (filename == null)
//					Log.println("You cancelled the choice");
//				else {
//					Log.println("File: " + filename);
//					Log.println("DIR: " + dirname);
//					File selectedFile = new File(dirname + filename);
//					txtLogFileDirectory.setText(selectedFile.getAbsolutePath());
//				}
//			} else {
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
					txtLogFileDirectory.setText(fc.getSelectedFile().getAbsolutePath());
				} else {
					System.out.println("No Selection ");
				}
//			}

		}

	}

	void saveAndExit() {
		Config.set(Config.FIRST_RUN, false);

		// user changed the logfile directory
		File file = new File(txtLogFileDirectory.getText());
		String logs = txtLogFileDirectory.getText();
		String call = txtCallsign.getText();
		if (logs.equalsIgnoreCase("") || !file.isDirectory() || file == null || !file.exists()){
			Log.errorDialog("Invalid directory", "Can not find the specified directory: " + txtLogFileDirectory.getText());
		} else {
			if (call.equalsIgnoreCase("") || call.length() > 6){
				Log.errorDialog("Invalid Callsign", "You need to set your ground station callsign.  Note that the Pacsat protocol\n"
						+ "restricts it to a maximum of 6 characters.");
			} else {
				Config.set(Config.LOGFILE_DIR, txtLogFileDirectory.getText());
				Config.set(Config.CALLSIGN, txtCallsign.getText().toUpperCase());
				this.dispose();
			}
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		//System.exit(1);
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	

}
