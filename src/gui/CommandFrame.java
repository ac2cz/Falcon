package gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.CommandParams;
import common.Config;
import common.Log;
import common.SpacecraftSettings;
import pacSat.frames.CmdFrame;

/**
 * 
 * Amsat PacSat Ground
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2023 amsat.org
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
public class CommandFrame  extends JFrame implements ActionListener, WindowListener { 
	public static final String CMD_WINDOW_X = "cmd_window_x";
	public static final String CMD_WINDOW_Y = "cmd_window_y";
	public static final String CMD_WINDOW_WIDTH = "cmd_window_width";
	public static final String CMD_WINDOW_HEIGHT = "cmd_window_height";
	
	static SpacecraftSettings spacecraftSettings;
	private JComboBox<String> cbNameSpace;
	private JComboBox<String> cbCommands;
	JButton butCmdSend, butCmdStop; 
	JLabel lblArg[], lblStatus;
	JTextField txtFileId, txtArg[];
	JComboBox<String> cbArg[];
	JPanel argPanel[];
	JTextArea taDesc;
	

	public CommandFrame(SpacecraftSettings spacecraftSettings) {
		super("PACSAT COMMAND");
		CommandFrame.spacecraftSettings = spacecraftSettings;
		    
		addWindowListener(this);
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/pacsat.jpg")));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());
		
		JPanel top = new JPanel();
		JPanel left = new JPanel();
		JPanel right = new JPanel();
		JPanel center = new JPanel();
		JPanel bottom = new JPanel();
		pane.add(top, BorderLayout.NORTH);
		pane.add(left, BorderLayout.WEST);
		pane.add(right, BorderLayout.EAST);
		pane.add(center, BorderLayout.CENTER);
		pane.add(bottom, BorderLayout.SOUTH);
		
		/* TOP PANEL */
		top.setLayout(new FlowLayout(FlowLayout.CENTER));
		lblStatus = new JLabel(""); //IORS Mode: ...... PM: ..  CH-A: ... CH-B: ... RPT: ...");
		Font f = lblStatus.getFont();
		lblStatus.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		top.add(lblStatus);
		
		/* LEFT PANEL */
		//left.add(new Box.Filler(new Dimension(10,10), new Dimension(40,10), new Dimension(100,40)));

		/* RIGHT PANEL */
		right.add(new Box.Filler(new Dimension(10,10), new Dimension(40,10), new Dimension(100,40)));

		/* CENTER PANEL */
		center.setLayout(new BorderLayout());
		//center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		//center.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		JPanel centerTop = new JPanel();
		JPanel centerCenter = new JPanel();
		JPanel centerBottom = new JPanel();
		center.add(centerTop,BorderLayout.NORTH);
		center.add(centerCenter, BorderLayout.CENTER);
		center.add(centerBottom, BorderLayout.SOUTH);
		
		
		TitledBorder heading0 = title("Command");
		centerTop.setBorder(heading0);
		
		/* This sets how big tall the pull downs are*/
		centerTop.add(new Box.Filler(new Dimension(10,10), new Dimension(40,30), new Dimension(100,30)));
		
		centerTop.setLayout(new BoxLayout(centerTop, BoxLayout.X_AXIS));
//		centerTop.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		JLabel lblNameSpace = new JLabel("   Type  ");
		cbNameSpace = new JComboBox<String>();
		ArrayList<String> types = spacecraftSettings.getList(CommandParams.NAME_SPACES);
		if (types != null) {
			for (String param : types) {
				cbNameSpace.addItem(param);
			}
		}
		cbNameSpace.setSelectedIndex(1);
		centerTop.add(lblNameSpace);
		centerTop.add(cbNameSpace);
		cbNameSpace.addActionListener(this);
		
		JLabel lblCommand = new JLabel("     Command  ");
		cbCommands = new JComboBox<String>();
		setCommands();
		centerTop.add(lblCommand);
		centerTop.add(cbCommands);
		cbCommands.addActionListener(this);
		
		centerCenter.setLayout(new BoxLayout(centerCenter, BoxLayout.Y_AXIS));
		
		centerCenter.add(new Box.Filler(new Dimension(10,10), new Dimension(40,50), new Dimension(100,50)));
		TitledBorder heading1 = title("Paramaters");
		centerCenter.setBorder(heading1);
		//JLabel lblParams = new JLabel("Command Paramaters:");
		//Font f = lblParams.getFont();
		//lblParams.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		//centerCenter.add(lblParams);
		
		lblArg = new JLabel[4];
		txtArg = new JTextField[4];
		cbArg = new JComboBox[4];
		argPanel = new JPanel[4];
		for (int a=0; a<4; a++ ) {
			argPanel[a] = new JPanel();
			argPanel[a].setLayout(new FlowLayout());
			//argPanel[a].setLayout(new BoxLayout(argPanel[a], BoxLayout.X_AXIS));
			lblArg[a] = new JLabel("Argument " + a + " (s)   ");
			txtArg[a] = new JTextField();
			txtArg[a].setColumns(10);
			cbArg[a] = new JComboBox<String>();
			cbArg[a].addActionListener(this);
			argPanel[a].add(lblArg[a]);
			argPanel[a].add(cbArg[a]);
			argPanel[a].add(txtArg[a]);
			
			centerCenter.add(argPanel[a]);

		}
		
		TitledBorder heading9 = title("Description");
		centerBottom.setBorder(heading9);
		centerBottom.setLayout(new BorderLayout());

		taDesc = new JTextArea(2, 45);
		taDesc.setText(spacecraftSettings.get(SpacecraftSettings.DESCRIPTION));
		taDesc.setLineWrap(true);
		taDesc.setWrapStyleWord(true);
		taDesc.setEditable(false);
		taDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
		centerBottom.add(taDesc, BorderLayout.CENTER);
		
		CommandParams p = spacecraftSettings.getParam(cbNameSpace.getSelectedIndex(), (String)cbCommands.getSelectedItem());

		setArgs(p);
		
		centerCenter.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));
		
		/* BOTTOM PANEL */
		bottom.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		butCmdSend = new JButton("Send");
//		butCmdSend.setMargin(new Insets(0,0,0,0));
		butCmdSend.addActionListener(this);
		butCmdSend.setToolTipText("Send the command");
		butCmdSend.setFont(MainWindow.sysFont);
		bottom.add(butCmdSend);

		butCmdStop = new JButton("Stop");
//		butCmdStop.setMargin(new Insets(0,0,0,0));
		butCmdStop.addActionListener(this);
		butCmdStop.setToolTipText("Stop sending command");
		butCmdStop.setFont(MainWindow.sysFont);
		bottom.add(butCmdStop);

		setVisible(true);
	}
	
	private TitledBorder title(String s) {
		TitledBorder title = new TitledBorder(null, s, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		return title;
	}
	
	void setCommands() {
		cbCommands.removeAllItems();
		ArrayList<String> names = spacecraftSettings.getParamsByNamespace(cbNameSpace.getSelectedIndex());
		for (String param : names) {
			cbCommands.addItem(param);
		}
	}
	
	public void setStatus(String status) {
		lblStatus.setText(status);
	}
	
	void setArgs(CommandParams param) {
		if (param == null) return;
		boolean noParams = true;
		for (int a=0; a<4; a++ ) {
			if (param.argNames[a].equalsIgnoreCase(CommandParams.NONE)) {
				lblArg[a].setVisible(false);
				txtArg[a].setVisible(false);
				cbArg[a].setVisible(false);
				
			} else if (param.argNames[a].equalsIgnoreCase(CommandParams.MSB32BIT)) {
				lblArg[a].setVisible(false);
				txtArg[a].setVisible(false);
				cbArg[a].setVisible(false);				
			} else {
				lblArg[a].setVisible(true);
				lblArg[a].setText(param.argNames[a]);
				txtArg[a].setText(""+param.args[a]);
				ArrayList<String> argStrings = spacecraftSettings.getList(param.argNames[a]);
				if (argStrings == null ) {
					txtArg[a].setVisible(true);
					cbArg[a].setVisible(false);
				} else {
					txtArg[a].setVisible(false);
					cbArg[a].setVisible(true);
					cbArg[a].removeAllItems();
					for (String val : argStrings) {
						cbArg[a].addItem(val);
					}
					cbArg[a].setSelectedIndex(param.args[a]);
				}
				noParams = false;
			}
		}
		if (noParams) {
			lblArg[0].setVisible(true);
			lblArg[0].setText("None");
		}
		if (param.args[0] == CommandParams.TIME_PARAM) {
			txtArg[0].setEnabled(false);
		} else {
			txtArg[0].setEnabled(true);
		}
		if (param.description != null)
			taDesc.setText(param.description);

	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void saveProperties() {
		Config.set(CMD_WINDOW_HEIGHT, this.getHeight());
		Config.set(CMD_WINDOW_WIDTH, this.getWidth());
		Config.set(CMD_WINDOW_X, this.getX());
		Config.set(CMD_WINDOW_Y, this.getY());

		Config.save();
	}

	public void loadProperties() {
		if (Config.getInt(CMD_WINDOW_X) == 0) {
			Config.set(CMD_WINDOW_X, 100);
			Config.set(CMD_WINDOW_Y, 100);
			Config.set(CMD_WINDOW_HEIGHT, 600);
			Config.set(CMD_WINDOW_WIDTH, 650);
		}
		setBounds(Config.getInt(CMD_WINDOW_X), Config.getInt(CMD_WINDOW_Y), 
				Config.getInt(CMD_WINDOW_WIDTH), Config.getInt(CMD_WINDOW_HEIGHT));
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
	public void actionPerformed(ActionEvent e) {
		for (int i=0;i< 4; i++) {
			if (e.getSource() == cbArg[i]) {
				int j = cbArg[i].getSelectedIndex();
				txtArg[i].setText(""+j);
			}
		}
		if (e.getSource() == cbNameSpace) {
			// User changes selected name space
			setCommands();
		}
		if (e.getSource() == cbCommands) {
			// User changes selected command
			CommandParams p = spacecraftSettings.getParam(cbNameSpace.getSelectedIndex(), (String)cbCommands.getSelectedItem());
			setArgs(p);
		}
		if (e.getSource() == butCmdSend) {
			CommandParams cmd = spacecraftSettings.getParam(cbNameSpace.getSelectedIndex(), (String)cbCommands.getSelectedItem());
			if (cmd == null) {
				Log.infoDialog("No command selcted", "Select a Command type and command to transmit.");
				return;
			}
			if (cmd.confirm) {
				Object[] options = {"Yes",
				"No"};
				int n = JOptionPane.showOptionDialog(
						MainWindow.frame,
						"Are you sure you want to send a command that will:\n " + cmd.description,
								"Do you want to continue?",
								JOptionPane.YES_NO_OPTION, 
								JOptionPane.ERROR_MESSAGE,
								null,
								options,
								options[1]);

				if (n == JOptionPane.NO_OPTION) {
					return;
				}
			}
			int pass_args[] = new int[4];
			if (cmd.args[0] == CommandParams.TIME_PARAM) {
				Date now = new Date();
				long unixtime = (now.getTime()/1000);
				
				//System.err.println("Unix: " + unixtime);
				pass_args[0] = (int)unixtime & 0xFFFF;
				pass_args[1] = (int)unixtime >> 16;
			} else {
				for (int i=0; i<4; i++) {
					try {
						pass_args[i] = (int) ((Long.parseLong(txtArg[i].getText())) & 0xffff);
					} catch (NumberFormatException ef) {
						pass_args[i] = 0;
					}
					if (cmd.argNames[i].equalsIgnoreCase(CommandParams.MSB32BIT)) {
						if (i>0)
							pass_args[i] = (int) ((Long.parseLong(txtArg[i-1].getText()) >> 16));
					}
				}
				
			}
			sendCommand(cmd.nameSpace, cmd.cmd, pass_args);	
		}
		if (e.getSource() == butCmdStop) {
			// Send an empty command frame to stop the transmission
			CmdFrame cmdFrame = new CmdFrame();
			spacecraftSettings.downlink.processEvent(cmdFrame);
		}

	}
	
	void sendCommand(int nameSpace, int cmd, int[] args) {
		Log.println("Sending command: " + cmd + " "+ Integer.toHexString(args[0]) 
				+ " " + Integer.toHexString(args[1]) 
						+ " " + Integer.toHexString(args[2]) 
								+ " " + Integer.toHexString(args[3]));
		if (spacecraftSettings == null) return;
		if (!spacecraftSettings.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) return;
		if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
			Log.errorDialog("ERROR", "You need to set the callsign transmitting\nGo to the File > Settings screen\n");
			return;
		}
		if (!Config.getBoolean(Config.TX_INHIBIT)) {

			
			 try {
				 Date now = new Date();
				 long time = now.getTime()/1000;
				 //System.err.println("Time" + time);
				 //System.err.println("Time" + Long.toHexString(time));
				 CmdFrame cmdFrame;
				
				 cmdFrame = new CmdFrame(Config.get(Config.CALLSIGN), spacecraftSettings.get(SpacecraftSettings.BROADCAST_CALLSIGN),
							time, nameSpace, cmd, args, spacecraftSettings.key);
				//System.err.println("Ready Cmd:" + cmdFrame + "/n");
				 // RESET/UPTIME
				// cmdFrame = new CmdFrame(Config.get(Config.CALLSIGN), spacecraftSettings.get(SpacecraftSettings.BROADCAST_CALLSIGN),
				//	0xABCD, time, nameSpace, cmd, args, key);
				 spacecraftSettings.downlink.processEvent(cmdFrame);
			 } catch (IllegalArgumentException e) {
				 Log.errorDialog("ERROR", "Invalid secret command key\n");
			 }
		} else {
			Log.errorDialog("Transmitted Disabled", "Command cant be sent when the transmitter is inhibitted\n"
					+ "Disable 'Inhibit Tranmitter' on the settings tab" );
		}
	}
}
