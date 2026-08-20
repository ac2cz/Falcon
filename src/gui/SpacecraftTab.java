package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.DefaultCaret;

import com.g0kla.telem.data.ByteArrayLayout;


import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.DirHole;
import fileStore.FileHole;
import fileStore.PacSatFile;
import fileStore.SortedArrayList;
import pacSat.frames.CmdFrame;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.RequestFileFrame;

public class SpacecraftTab extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;
	public static final String SHOW_ALL_LBL = "All Files";
	public static final String SHOW_USER_LBL = "User Files";

	public static final boolean SHOW_ALL = false;
	public static final boolean SHOW_USER = true;
	
	public DirectoryPanel dirPanel;
	public OutboxPanel outbox;
	JTabbedPane jtabbedPane;
	JPanel dirAndStatusPanel;
	SpacecraftSettings spacecraftSettings;
	
	JButton butDirReq, butFileReq, butStop, butCmd; //, butCmdSend, butCmdStop; 
	JTextField txtFileId; //, txtArg[];
	JButton butFilter;
	JButton butNew;
	JButton butLogin;
	JCheckBox cbUplink, cbDownlink;

	JLabel lblDirHoles;
	JLabel lblFileName;
	JLabel lblPBStatus;
	JLabel lblPGStatus, lblPGLogin;
	JLabel lblFileUploading;
	JLabel lblUplinkStatus;
	JLabel lblLayer2Status;
	JLabel lblDownlinkStatus;
	JPanel filePanel;
	JLabel lblEfficiency;
	
	public CommandFrame commanding;
	
	SpacecraftTab(SpacecraftSettings spacecraftSettings) {
		this.spacecraftSettings = spacecraftSettings;
	
		spacecraftSettings.directory.setShowFiles(spacecraftSettings.getBoolean(SpacecraftSettings.SHOW_USER_FILES));
		
		setLayout(new BorderLayout());
		
		makeTopPanel();
		makeSpacecraftPanel(spacecraftSettings);
		makeBottomPanel();
		
	}
	
	private void makeTopPanel() {
		JPanel topPanel = new JPanel();
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new FlowLayout (FlowLayout.LEFT));
		
		butDirReq = new JButton("DIR");
		butDirReq.setMargin(new Insets(0,0,0,0));
		butDirReq.addActionListener(this);
		butDirReq.setToolTipText("Request the lastest directory entries");

		butDirReq.setFont(MainWindow.sysFont);
		JLabel bar = new JLabel("|");
		JLabel bar2 = new JLabel("|");
		
		JLabel lblReq = new JLabel("Request: ");
		lblReq.setFont(MainWindow.sysFont);
		
		butFileReq = new JButton("FILE");
		butFileReq.setMargin(new Insets(0,0,0,0));
		butFileReq.addActionListener(this);
		butFileReq.setToolTipText("Request the file with this ID");
		butFileReq.setFont(MainWindow.sysFont);
		JLabel dash = new JLabel("-");
		txtFileId = new JTextField();
		txtFileId.setColumns(4);

		butStop = new JButton("Stop");
		butStop.setMargin(new Insets(0,0,0,0));
		butStop.addActionListener(this);
		butStop.setToolTipText("Stop sending a Request or Command");
		butStop.setFont(MainWindow.sysFont);

		butFilter = new JButton();
		if (spacecraftSettings.getBoolean(SpacecraftSettings.SHOW_USER_FILES))
			butFilter.setText(SHOW_USER_LBL);
		else
			butFilter.setText(SHOW_ALL_LBL);

		butFilter.setMargin(new Insets(0,0,0,0));
		butFilter.addActionListener(this);
		butFilter.setToolTipText("Toggle ALL or User Files");
		butFilter.setFont(MainWindow.sysFont);
		
		if (spacecraftSettings.getBoolean(SpacecraftSettings.SUPPORTS_FILE_UPLOAD)) {
			butNew = new JButton("New Msg");
			butNew.setMargin(new Insets(0,0,0,0));
			butNew.addActionListener(this);
			butNew.setToolTipText("Create a new Message");
			if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
				butNew.setEnabled(false);			
			} else {
				butNew.setEnabled(true);
			}
			butNew.setFont(MainWindow.sysFont);
			
			topPanel.add(butNew);
			topPanel.add(bar2);
		}
		
		JLabel bar3 = new JLabel("  |    ");
		
		if (spacecraftSettings.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) {
			
			butCmd = new JButton("Command");
			butCmd.setMargin(new Insets(0,0,0,0));
			butCmd.addActionListener(this);
			butCmd.setToolTipText("Open Command Window");
			butCmd.setFont(MainWindow.sysFont);
			
		}
		
		topPanel.add(lblReq);
		topPanel.add(butDirReq);
		topPanel.add(butFileReq);
		topPanel.add(dash);
		topPanel.add(txtFileId);
		topPanel.add(butStop);
		topPanel.add(bar);
		topPanel.add(butFilter);
		
		if (spacecraftSettings.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) {
			topPanel.add(bar3);
			topPanel.add(butCmd);
//			topPanel.add(cbCommands);
//			for (int a=0; a<4; a++) {
//				topPanel.add(txtArg[a]);
//			}
//			topPanel.add(butCmdSend);
//			topPanel.add(butCmdStop);
//
		}
			
	}
	
	private void makeSpacecraftPanel(SpacecraftSettings spacecraftSettings) {
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		jtabbedPane = new JTabbedPane();
		add(jtabbedPane,BorderLayout.CENTER);
		// Scroll panel holds the directory
		
		dirPanel = new DirectoryPanel(spacecraftSettings, this);
		outbox = new OutboxPanel(spacecraftSettings, this);
		
		//JTabbedPane tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
		jtabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setFont(MainWindow.sysFont);
		
		addDirTabs();
		addTelemTabs(spacecraftSettings);

	}
	
	private void makeBottomPanel( ) {
		JPanel bottomPanel = new JPanel();
		dirAndStatusPanel.add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BorderLayout());
		JPanel satStatusPanel = makeSatStatusPanel();
		bottomPanel.add(satStatusPanel, BorderLayout.CENTER);
	}
	
	
	void addDirTabs() {
		JPanel logPanel = makeLogPanel();
		
		dirAndStatusPanel = new JPanel();
		dirAndStatusPanel.setLayout(new BorderLayout());
		dirAndStatusPanel.add(dirPanel, BorderLayout.CENTER);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				dirAndStatusPanel, logPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
		
		MainWindow.splitPaneHeight = Config.getInt(MainWindow.WINDOW_SPLIT_PANE_HEIGHT);
		
		if (MainWindow.splitPaneHeight != 0) 
			splitPane.setDividerLocation(MainWindow.splitPaneHeight);
		else
			splitPane.setDividerLocation(MainWindow.DEFAULT_DIVIDER_LOCATION);

		SplitPaneUI spui = splitPane.getUI();
		if (spui instanceof BasicSplitPaneUI) {
			// Setting a mouse listener directly on split pane does not work, because no events are being received.
			((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					MainWindow.splitPaneHeight = splitPane.getDividerLocation();
					//Log.println("SplitPane: " + splitPaneHeight);
					Config.set(MainWindow.WINDOW_SPLIT_PANE_HEIGHT, MainWindow.splitPaneHeight);
				}
			});
		}
		jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Directory</body></html>", splitPane );
//		centerPanel.add(splitPane, BorderLayout.CENTER);
		if (spacecraftSettings.getBoolean(SpacecraftSettings.SUPPORTS_FILE_UPLOAD))
			jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Outbox</body></html>", outbox );
	}
	
//	private void makeCenterPanel() {
//		JPanel centerPanel = new JPanel();
//		centerPanel.setLayout(new BorderLayout ());
//		
//		// Bottom has the log view
//		JPanel centerBottomPanel = makeLogPanel();
//		//centerPanel.add(centerBottomPanel, BorderLayout.SOUTH);
//		getContentPane().add(centerPanel, BorderLayout.CENTER);
//		
//		splitPaneHeight = Config.getInt(WINDOW_SPLIT_PANE_HEIGHT);
//		
//		spacecraftTabbedPanel = new JTabbedPane(JTabbedPane.TOP);
//		spacecraftTabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
//		spacecraftTabbedPanel.setFont(sysFont);
//		
//		for (SpacecraftSettings satSettings : Config.spacecraftSettings) {
//			//SpacecraftTab center = makeSpacecraftPanel(satSettings);
//			SpacecraftTab center = new SpacecraftTab(satSettings);
//			spacecraftTabs.put(satSettings.name, center);
//			spacecraftTabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>"+satSettings.name+"</body></html>", center );
//		}
//		
//		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
//				spacecraftTabbedPanel, centerBottomPanel);
//		splitPane.setOneTouchExpandable(true);
//		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
//		if (splitPaneHeight != 0) 
//			splitPane.setDividerLocation(splitPaneHeight);
//		else
//			splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);
//
//		SplitPaneUI spui = splitPane.getUI();
//		if (spui instanceof BasicSplitPaneUI) {
//			// Setting a mouse listener directly on split pane does not work, because no events are being received.
//			((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
//				public void mouseReleased(MouseEvent e) {
//					splitPaneHeight = splitPane.getDividerLocation();
//					//Log.println("SplitPane: " + splitPaneHeight);
//					Config.set(WINDOW_SPLIT_PANE_HEIGHT, splitPaneHeight);
//				}
//			});
//		}
//		centerPanel.add(splitPane, BorderLayout.CENTER);
//	}
	
	private JPanel makeLogPanel() {
		JPanel centerBottomPanel = new JPanel();
		
		centerBottomPanel.setLayout(new BoxLayout(centerBottomPanel, BoxLayout.Y_AXIS));
		MainWindow.logTextArea = new JTextArea(10,135);
		MainWindow.logTextArea.setLineWrap(true);
		MainWindow.logTextArea.setWrapStyleWord(true);
		MainWindow.logTextArea.setEditable(false);
		// get the current font size
		//int size = Config.getInt(Config.FONT_SIZE);
		Font f = MainWindow.logTextArea.getFont();
		//if (size == 0) {
		//	size = f.getSize();
		//	Config.set(Config.FONT_SIZE, size);
		//}
		// create a new, smaller font from the current font
		Font f2 = new Font(f.getFontName(), f.getStyle(), (int) (MainWindow.fontSize));
		// set the new font in the editing area
		MainWindow.logTextArea.setFont(f2);
		DefaultCaret caret = (DefaultCaret)MainWindow.logTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		MainWindow.logTextArea.getDocument().addDocumentListener(
			    new LimitLinesDocumentListener(100) );
		JScrollPane logScroll = new JScrollPane (MainWindow.logTextArea);
		logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		logScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		centerBottomPanel.add(logScroll);
		
		return centerBottomPanel;
	}
	
	void addTelemTabs(SpacecraftSettings spacecraftSettings) {
		if (spacecraftSettings.spacecraft == null) return;
		
		if (spacecraftSettings.spacecraft.layoutsUseToCallsignAsType) {
			for (ByteArrayLayout layout  : spacecraftSettings.spacecraft.layout) {
				TelemTab wodPanel = new TelemTab(layout, spacecraftSettings.spacecraft, spacecraftSettings.db);
				Thread wodPanelThread = new Thread(wodPanel);
				wodPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				wodPanelThread.setName(layout.name);
				wodPanelThread.start();
				jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>"+ layout.name +"</body></html>", wodPanel );
			}
		} else {
			ByteArrayLayout wodLayout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.WOD_LAYOUT);
			if (wodLayout != null) {
				TelemTab wodPanel = new TelemTab(wodLayout, spacecraftSettings.spacecraft, spacecraftSettings.db);
				Thread wodPanelThread = new Thread(wodPanel);
				wodPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				wodPanelThread.setName("WODTab");
				wodPanelThread.start();
				jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>WOD</body></html>", wodPanel );
			}

			ByteArrayLayout fullWodLayout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.FULL_WOD_LAYOUT);
			if (fullWodLayout != null) {
				TelemTab fullWodPanel = new TelemTab(fullWodLayout, spacecraftSettings.spacecraft, spacecraftSettings.db);
				Thread wodPanelThread = new Thread(fullWodPanel);
				wodPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				wodPanelThread.setName("FullWODTab");
				wodPanelThread.start();
				jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Full WOD</body></html>", fullWodPanel );
			}


			ByteArrayLayout tlmLayout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLMI_LAYOUT);
			if (tlmLayout != null) {
				TelemTab tlmIPanel = new TelemTab(tlmLayout, spacecraftSettings.spacecraft, spacecraftSettings.db);
				Thread telemIPanelThread = new Thread(tlmIPanel);
				telemIPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				telemIPanelThread.setName("TLMItab");
				telemIPanelThread.start();
				jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>TLM</body></html>", tlmIPanel );
			}

			ByteArrayLayout tlm16Layout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLM16_LAYOUT);
			if (tlm16Layout != null) {
				TelemTab tlm16Panel = new TelemTab(tlm16Layout, spacecraftSettings.spacecraft, spacecraftSettings.db);
				Thread telemIPanelThread = new Thread(tlm16Panel);
				telemIPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				telemIPanelThread.setName("FailSafeTab");
				telemIPanelThread.start();
				jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>FailSafe</body></html>", tlm16Panel );
			}

			ByteArrayLayout tlm1Layout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLM1_LAYOUT);
			if (tlm1Layout != null) {
				TelemTab tlm1Panel = new TelemTab(tlm1Layout, spacecraftSettings.spacecraft, spacecraftSettings.db);
				Thread telem2PanelThread = new Thread(tlm1Panel);
				telem2PanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				telem2PanelThread.setName("TLM1tab");
				telem2PanelThread.start();
				jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>TLM1</body></html>", tlm1Panel );
			}

			ByteArrayLayout tlm2Layout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLM2_LAYOUT);
			if (tlm2Layout != null) {
				TelemTab tlm2Panel = new TelemTab(tlm2Layout, spacecraftSettings.spacecraft, spacecraftSettings.db);
				Thread telem2PanelThread = new Thread(tlm2Panel);
				telem2PanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				telem2PanelThread.setName("TLM2tab");
				telem2PanelThread.start();
				jtabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>TLM2</body></html>", tlm2Panel );
			}
		}
	}
	
	void removeTabs() {
		for(int i = 0; i < jtabbedPane.getTabCount(); i++) {
			Component comp = jtabbedPane.getComponentAt(i);
			if (comp instanceof TelemTab) {
				TelemTab tab = (TelemTab) comp;
				if (tab != null) {
					tab.stopProcessing();
					jtabbedPane.remove(tab);
				}
			}
		}
		
				
		jtabbedPane.remove(dirAndStatusPanel);
		//tabbedPanel.remove(systemDirPanel);
		if (spacecraftSettings.getBoolean(SpacecraftSettings.SUPPORTS_FILE_UPLOAD))
			jtabbedPane.remove(outbox);

//		ByteArrayLayout wodLayout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.WOD_LAYOUT);
//		if (wodLayout != null && wodPanel != null) {
//			wodPanel.stopProcessing();
//			jtabbedPane.remove(wodPanel);
//			wodPanel = null;
//		}
//		ByteArrayLayout tlmLayout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLMI_LAYOUT);
//		if (tlmLayout != null && tlmIPanel != null) {
//			tlmIPanel.stopProcessing();
//			jtabbedPane.remove(tlmIPanel);
//			tlmIPanel = null;
//		}
//		ByteArrayLayout tlm1Layout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLM1_LAYOUT);
//		if (tlm1Layout != null && tlm1Panel != null) {
//			tlm1Panel.stopProcessing();
//			jtabbedPane.remove(tlm1Panel);
//			tlm1Panel = null;
//		}
//		ByteArrayLayout tlm2Layout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLM2_LAYOUT);
//		if (tlm2Layout != null && tlm2Panel != null) {
//			tlm2Panel.stopProcessing();
//			jtabbedPane.remove(tlm2Panel);
//			tlm2Panel = null;
//		}
	}
	
	void setDirectoryData(String[][] data) {
		dirPanel.setDirectoryData(data);
		lblDirHoles.setText("DIR: " + dirPanel.holes + " holes. Age: " + dirPanel.age + " days");
	}

	private JPanel makeSatStatusPanel() {
		JPanel satStatusPanel = new JPanel();
		satStatusPanel.setLayout(new BorderLayout ());
		Border loweredbevel = BorderFactory.createLoweredBevelBorder();
		
		JPanel centerSat = new JPanel();
		centerSat.setLayout(new BorderLayout());
		centerSat.setBorder(loweredbevel);
		satStatusPanel.add(centerSat, BorderLayout.CENTER );
		
		lblPBStatus = new JLabel("PB: ?????? ");
		centerSat.add(lblPBStatus, BorderLayout.NORTH);
		lblPBStatus.setFont(MainWindow.sysFont);
		Font f = lblPBStatus.getFont();
		lblPBStatus.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
			
		JPanel pbStatusPanel = new JPanel();
		pbStatusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		centerSat.add(pbStatusPanel, BorderLayout.SOUTH);
		
		lblDirHoles = new JLabel("DIR: ?? Holes");
		lblDirHoles.setFont(MainWindow.sysFont);
		lblDirHoles.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		centerSat.add(lblDirHoles);
		
		JLabel bar = new JLabel("|");
		pbStatusPanel.add(bar);
		
		lblDownlinkStatus = new JLabel("Start");
		lblDownlinkStatus.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblDownlinkStatus.setFont(MainWindow.sysFont);
		pbStatusPanel.add(lblDownlinkStatus);

		JLabel bar2 = new JLabel("|");
		pbStatusPanel.add(bar2);
		
		lblEfficiency = new JLabel("Broadcast: 0 / Rec: 0 / Eff: 0%");
		lblEfficiency.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblEfficiency.setFont(MainWindow.sysFont);
		pbStatusPanel.add(lblEfficiency);
		
		JPanel rightSat = new JPanel();
		rightSat.setLayout(new BorderLayout());
		rightSat.setBorder(loweredbevel);
		satStatusPanel.add(rightSat, BorderLayout.EAST);
		
		lblPGLogin = new JLabel("Disconnected");
		lblPGLogin.setFont(MainWindow.sysFont);
		rightSat.add(lblPGLogin, BorderLayout.CENTER);
		
		lblPGStatus = new JLabel("Open: ??????");
		lblPGStatus.setFont(MainWindow.sysFont);
		// toggle bold
		f = lblPGStatus.getFont();
		lblPGStatus.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
		rightSat.add(lblPGStatus, BorderLayout.NORTH);
		//rightSat.add(new Box.Filler(new Dimension(10,40), new Dimension(400,40), new Dimension(1500,500)), BorderLayout.NORTH);
		
		JPanel pgStatusPanel = new JPanel();
		pgStatusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		rightSat.add(pgStatusPanel, BorderLayout.SOUTH);
		lblFileUploading = new JLabel("File: None");
		lblFileUploading.setFont(MainWindow.sysFont);
		this.setFileUploading(0, 0, 0);
		lblFileUploading.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pgStatusPanel.add(lblFileUploading);
		
		JLabel bar4 = new JLabel("|");
		pgStatusPanel.add(bar4);
		
		lblUplinkStatus = new JLabel("Init");
		lblUplinkStatus.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblUplinkStatus.setFont(MainWindow.sysFont);
		pgStatusPanel.add(lblUplinkStatus);

		JLabel bar3 = new JLabel("|");
		pgStatusPanel.add(bar3);
		
		lblLayer2Status = new JLabel("LAYER2: UNK");
		lblLayer2Status.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblLayer2Status.setFont(MainWindow.sysFont);
		pgStatusPanel.add(lblLayer2Status);

		
		return satStatusPanel;
	}
	
	@SuppressWarnings("unused")
	private JPanel makeFilePanel() {
		JPanel centerTopPanel = new JPanel();
		
		filePanel = new JPanel();
		filePanel.setVisible(false);
		centerTopPanel.add(filePanel);
		lblFileName = new JLabel();
		filePanel.add(lblFileName);
		return centerTopPanel;
	}

	
	public void setFileUploading(long id, long offset, long length) {
		String p = "0";
		if (length != 0) 
			p = String.format("%.1f",(float)(100*offset/(float)length));
		lblFileUploading.setText("File: " + Long.toHexString(id) + " " + p + "%");
		if (id == 0) 
			lblFileUploading.setForeground(Color.BLACK); // Nothing to upload
		else if (offset == length)
			lblFileUploading.setForeground(Color.BLUE); // Uploaded
		else
			lblFileUploading.setForeground(Config.AMSAT_RED); // Uploading
	}
	
	public void setPBStatus(String pb) {
		lblPBStatus.setText("fred "+pb);
	}

	public void setPGStatus(String pg) {
		lblPGStatus.setText(pg);
	}
	
	public void setLoggedIn(String pg) {
		lblPGLogin.setText(pg);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == butNew) {
			if (!spacecraftSettings.getBoolean(SpacecraftSettings.SUPPORTS_FILE_UPLOAD)) return;
			if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
				Log.errorDialog("ERROR", "You need to set the callsign before you can compose a message\nGo to the File > Settings screen\n");
				return;
			}
			
			MainWindow.newMessage(spacecraftSettings);
		}
		if (e.getSource() == butDirReq) {
			if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
				Log.errorDialog("ERROR", "You need to set the callsign transmitting\nGo to the File > Settings screen\n");
				return;
			}
			if (!Config.getBoolean(Config.TX_INHIBIT)) {
				if (spacecraftSettings == null) return;
				// Make a DIR Request Frame and Send it to the TNC for TX
				RequestDirFrame dirFrame = null;
				SortedArrayList<DirHole> holes = spacecraftSettings.directory.getHolesList();
				if (holes != null) {
					//for (DirHole hole : holes)
					//	Log.println("" + hole);
					dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), spacecraftSettings.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, holes);
				} else {
					Log.errorDialog("ERROR", "Something has gone wrong and the directory holes file is missing or corrupt\nCan't request the directory\n");
					//Date fromDate = Config.spacecraft.directory.getLastHeaderDate();
					//dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, fromDate);
				}
				spacecraftSettings.downlink.processEvent(dirFrame);
			} else {
				Log.errorDialog("Transmitted Disabled", "Directory can't be requested when the transmitter is inhibitted\n"
						+ "Disable 'Inhibit Tranmitter' on the settings tab" );

			}
		}
		if (e.getSource() == butFileReq) {
			if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
				Log.errorDialog("ERROR", "You need to set the callsign transmitting\nGo to the File > Settings screen\n");
				return;
			}
			if (!Config.getBoolean(Config.TX_INHIBIT)) {
				// Make a DIR Request Frame and Send it to the TNC for TX
				if (spacecraftSettings == null) return;
				String fileIdstr = txtFileId.getText(); 
				if (fileIdstr == null || fileIdstr.length() == 0 || fileIdstr.length() > 4)
					Log.errorDialog("File Request Error", "File Id should be 1-4 digits in HEX.  Invalid: " + fileIdstr);
				else {
					try {
						long fileId = Long.decode("0x" + fileIdstr);
						PacSatFile psf = new PacSatFile(spacecraftSettings, spacecraftSettings.directory.dirFolder, fileId);
						// The PSF always returns the entire hole list.  If there are too many then the Request Frame limits it
						SortedArrayList<FileHole> holes = psf.getHolesList();
						//for (FileHole fh : holes)
						//	Log.print(fh + " ");
						//Log.println("");
						RequestFileFrame fileFrame = new RequestFileFrame(Config.get(Config.CALLSIGN), spacecraftSettings.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, fileId, holes);
						spacecraftSettings.downlink.processEvent(fileFrame);
					} catch (NumberFormatException ne) {
						Log.errorDialog("File Request Error", "File Id should be 1-4 digits in HEX. Invalid: " + fileIdstr);
					}
			}
			} else {
				Log.errorDialog("Transmitted Disabled", "File Id can't be requested when the transmitter is inhibitted\n"
						+ "Disable 'Inhibit Tranmitter' on the settings tab" );
			}
		}
		if (e.getSource() == butFilter) {
			spacecraftSettings.set(SpacecraftSettings.SHOW_USER_FILES, !spacecraftSettings.getBoolean(SpacecraftSettings.SHOW_USER_FILES));

			if (spacecraftSettings.getBoolean(SpacecraftSettings.SHOW_USER_FILES))
				butFilter.setText(SHOW_USER_LBL);
			else
				butFilter.setText(SHOW_ALL_LBL);
			Log.println("SHOW: " + spacecraftSettings.getBoolean(SpacecraftSettings.SHOW_USER_FILES));
			spacecraftSettings.directory.setShowFiles(spacecraftSettings.getBoolean(SpacecraftSettings.SHOW_USER_FILES));
			setDirectoryData(spacecraftSettings.directory.getTableData());
			spacecraftSettings.save();
		}
		
		if (e.getSource() == butCmd) {
			if (spacecraftSettings.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) {
				if (commanding == null)
					commanding = new CommandFrame(spacecraftSettings);
				else
					commanding.setVisible(true);
			}
		}

		if (e.getSource() == butStop) {
			// Send an empty command frame to stop the transmission
			CmdFrame cmdFrame = new CmdFrame();
			spacecraftSettings.downlink.processEvent(cmdFrame);
		}
		
		if (e.getSource() == cbUplink) {
			if (cbUplink.isSelected())
				Config.set(Config.UPLINK_ENABLED, true);
			else
				Config.set(Config.UPLINK_ENABLED, false);
			
		}
		if (e.getSource() == cbDownlink) {
			if (cbDownlink.isSelected())
				Config.set(Config.DOWNLINK_ENABLED, true);
			else
				Config.set(Config.DOWNLINK_ENABLED, false);
		}

		
	}
	
}
