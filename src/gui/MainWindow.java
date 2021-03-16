package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.DefaultCaret;

import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import com.apple.eawt.Application;
import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.gui.ProgressPanel;

import pacSat.FrameDecoder;
import pacSat.SerialTncDecoder;
import pacSat.TcpTncDecoder;
import pacSat.TncDecoder;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.RequestFileFrame;
import common.Config;
import common.DesktopApi;
import common.Log;
import common.SpacecraftSettings;
import fileStore.DirHole;
import fileStore.Directory;
import fileStore.FileHole;
import fileStore.PacSatFile;
import fileStore.SortedArrayList;
import macos.MacAboutHandler;
import macos.MacPreferencesHandler;
import macos.MacQuitHandler;

@SuppressWarnings("serial")
public class MainWindow extends JFrame implements ActionListener, WindowListener {

	public static final String MAINWINDOW_X = "mainwindow_x";
	public static final String MAINWINDOW_Y = "mainwindow_y";
	public static final String MAINWINDOW_WIDTH = "mainwindow_width";
	public static final String MAINWINDOW_HEIGHT = "mainwindow_height";
	public static final String WINDOW_FC_WIDTH = "mainwindow_width";
	public static final String WINDOW_FC_HEIGHT = "mainwindow_height";
	public static final String WINDOW_CURRENT_DIR = "mainwindow_current_dir";
	public static final String EDITOR_CURRENT_DIR = "editor_current_dir";
	
	public static final String WINDOW_SPLIT_PANE_HEIGHT = "window_split_pane_height";
	
	public static final int DEFAULT_DIVIDER_LOCATION = 450;
	
	static Application macApplication;
	
	public static JFrame frame;
	// Swing File Chooser
	JFileChooser fc = null;
	//AWT file chooser for the Mac
	FileDialog fd = null;
	JLabel lblVersion;
	JLabel lblTotalTelem;
	JLabel lblTotalFrames;
	JLabel lblEfficiency;
	static JLabel lblLogFileDir;
	private JTextArea logTextArea;
	JLabel lblFileName;
	static JLabel lblPBStatus;
	static JLabel lblPGStatus;
	static JLabel lblFileUploading;
	static JLabel lblUplinkStatus;
	static JLabel lblLayer2Status;
	JPanel filePanel;
	
	Hashtable<String, SpacecraftTab> spacecraftTabs;
	
	TelemTab wodPanel;
	TelemTab tlmIPanel;
	TelemTab tlm2Panel;
	
	JButton butDirReq;
	JButton butFileReq;
	static JTextField txtFileId;
	static JButton butFilter;
	static JButton butNew;
	static JButton butLogin;
	JCheckBox cbUplink, cbDownlink;

	public static final String SHOW_ALL_LBL = "All Files";
	public static final String SHOW_USER_LBL = "User Files";

	public static final boolean SHOW_ALL = false;
	public static final boolean SHOW_USER = true;
	boolean showFilter = SHOW_USER;
	//JTabbedPane tabbedPanel;
	JTabbedPane spacecraftTabbedPanel;
	int splitPaneHeight = DEFAULT_DIVIDER_LOCATION;
	
	public static TncDecoder tncDecoder;
	Thread tncDecoderThread;
	FrameDecoder frameDecoder;
	Thread frameDecoderThread;
	
	// Status indicators
	JLabel lblDownlinkStatus;
	JLabel lblComPort;
	JLabel lblServerQueue;
//	static JLabel lblDCD;
	static JLabel lblDirHoles;
	
	// Menu items
	static JMenuItem mntmNewMsg, mntmGetServerData;
	static JMenuItem mntmExit;
	static JMenuItem mntmLoadKissFile;
	static JMenuItem mntmArchiveDir;
	static JMenuItem mntmSettings;
	static JMenuItem mntmManual, mntmLeaderboard, mntmWebsite;
	static JMenuItem mntmAbout;
	static JMenuItem[] mntmSat;
	static JMenuItem mntmReqDir;
	
	Timer timer;
	
	int fontSize;
	public static Font sysFont;

	public MainWindow() {
		frame = this; // a handle for error dialogues
		showFilter = Config.getBoolean(Config.SHOW_USER_FILES);
		for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings)
			spacecraftSettings.directory.setShowFiles(showFilter);
		
		spacecraftTabs = new Hashtable<String, SpacecraftTab>();
		initialize();
		
		if (Config.isMacOs()) {
			macApplication = com.apple.eawt.Application.getApplication();
			macApplication.setAboutHandler(new MacAboutHandler());
			macApplication.setPreferencesHandler(new MacPreferencesHandler());
			macApplication.setQuitHandler(new MacQuitHandler(this));
		}
		
		
		for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
			String[][] data = spacecraftSettings.directory.getTableData();
			if (data.length > 0)
				setDirectoryData(spacecraftSettings.name, data);
			if (spacecraftSettings.outbox.getTableData() != null) 
				setOutboxData(spacecraftSettings.name, spacecraftSettings.outbox.getTableData());
		}
		initDecoder();
		
		int speed = 1000; // start after 1 second
		int pause = 1000; // repeat every second
		timer = new Timer(speed, this);
		timer.setInitialDelay(pause);
		timer.start(); 
	}
	
	private void initialize() {
		if (Config.getInt(MAINWINDOW_X) == 0) {
			Config.set(MAINWINDOW_X, 100);
			Config.set(MAINWINDOW_Y, 100);
			Config.set(MAINWINDOW_WIDTH, 850);
			Config.set(MAINWINDOW_HEIGHT, 800);
		}
		setBounds(Config.getInt(MAINWINDOW_X), Config.getInt(MAINWINDOW_Y), 
				Config.getInt(MAINWINDOW_WIDTH), Config.getInt(MAINWINDOW_HEIGHT));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle("AMSAT PacSat Ground Station");
		fontSize = Config.getInt(Config.FONT_SIZE);
		
		initMenu();
		makeTopPanel();
		makeBottomPanel();
		makeCenterPanel();
		
		fd = new FileDialog(MainWindow.frame, "Select Kiss file",FileDialog.LOAD);
		fd.setFile("*.kss");
		fc = new JFileChooser();	
		// initialize the file chooser
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "Kiss", "kss", "Raw", "raw");
		fc.setFileFilter(filter);
		Action details = fc.getActionMap().get("viewTypeDetails");
		if (Config.isWindowsOs()) // this crashes Ubuntu
			details.actionPerformed(null);
		
		addWindowListener(this);
		
	}
	
	public void setDownlinkStatus(String txt) {
		lblDownlinkStatus.setText("DL: " + txt);
	}
	public void setUplinkStatus(String txt) {
		lblUplinkStatus.setText("UL: " + txt);
	}
	public void setLayer2Status(String txt) {
		lblLayer2Status.setText("LAYER2: " +txt);
	}
	
	public void setDirectoryData(String satName, String[][] data) {
		SpacecraftTab tab = spacecraftTabs.get(satName);
		if (tab != null)
			tab.dirPanel.setDirectoryData(data);
	}
	public void setOutboxData(String satName, String[][] data) {
		SpacecraftTab tab = spacecraftTabs.get(satName);
		if (tab != null)
			tab.outbox.setDirectoryData(data);
	}
	
	public static void setPBStatus(String pb) {
		lblPBStatus.setText(pb);
	}

	public static void setPGStatus(String pg) {
		lblPGStatus.setText(pg);
	}
	
	public void append(String s) {
		logTextArea.append(s);
		if (Config.getBoolean(Config.KEEP_CARET_AT_END_OF_LOG))
			try {
				logTextArea.setCaretPosition(logTextArea.getText().length()); // set the caret at the end continually
			} catch (Exception e) { }; // not fatal if the length of the text has changed and we get an exception
	}

	/**
	 * Start the TNC interface and read bytes from a file.
	 * @param fileName
	 */
	private void initDecoder(String fileName) {
		if (frameDecoder != null) {
			frameDecoder.close();
		}
		frameDecoder = new FrameDecoder(this);
		frameDecoderThread = new Thread(frameDecoder);
		frameDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		frameDecoderThread.setName("Frame Decoder");
		frameDecoderThread.start();


		if (tncDecoder != null) {
			tncDecoder.close();
		}
		tncDecoder = new SerialTncDecoder(frameDecoder, this, fileName);
		for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
			spacecraftSettings.downlink.setTncDecoder(tncDecoder, this);
			spacecraftSettings.uplink.setTncDecoder(tncDecoder, this);
			spacecraftSettings.layer2data.setTncDecoder(tncDecoder, this);
		}
		tncDecoderThread = new Thread(tncDecoder);
		tncDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		tncDecoderThread.setName("Tnc Decoder");
		tncDecoderThread.start();

	}
		
	/*
	 * Only called if we want to truely stop and restart.  At launch or if the config has changed
	 */
	void initDecoder() {
		if (frameDecoder != null) {
			frameDecoder.close();
		}
		frameDecoder = new FrameDecoder(this);
		frameDecoderThread = new Thread(frameDecoder);
		frameDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		frameDecoderThread.setName("Frame Decoder");
		frameDecoderThread.start();
		
		
		if (tncDecoder != null) {
			tncDecoder.close();
		}
		if (Config.getBoolean(Config.KISS_TCP_INTERFACE)) {
			String hostname = Config.get(Config.TNC_TCP_HOSTNAME);
			if (hostname == null) return;
			int port = Config.getInt(Config.TNC_TCP_PORT);
			if (hostname == null) return;
			tncDecoder = new TcpTncDecoder(hostname, port,frameDecoder, this);
		} else {
			String com = Config.get(Config.TNC_COM_PORT);
			if (com == null) return;
			tncDecoder = new SerialTncDecoder(com, Config.getInt(Config.TNC_BAUD_RATE), Config.getInt(Config.TNC_DATA_BITS), 
					Config.getInt(Config.TNC_STOP_BITS), Config.getInt(Config.TNC_PARITY),frameDecoder, this);
		}
		for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
			spacecraftSettings.downlink.setTncDecoder(tncDecoder, this);
			spacecraftSettings.uplink.setTncDecoder(tncDecoder, this);
			spacecraftSettings.layer2data.setTncDecoder(tncDecoder, this);
		}
		tncDecoderThread = new Thread(tncDecoder);
		tncDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		tncDecoderThread.setName("Tnc Decoder");
		tncDecoderThread.start();
	}
	
	private void makeTopPanel() {
		JPanel topPanel = new JPanel();
		getContentPane().add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new FlowLayout (FlowLayout.LEFT));
		
		butDirReq = new JButton("DIR");
		butDirReq.setMargin(new Insets(0,0,0,0));
		butDirReq.addActionListener(this);
		butDirReq.setToolTipText("Request the lastest directory entries");

		butDirReq.setFont(sysFont);
		JLabel bar = new JLabel("|");
		JLabel bar2 = new JLabel("|");
		
		JLabel lblReq = new JLabel("Request: ");
		lblReq.setFont(sysFont);
		
		butFileReq = new JButton("FILE");
		butFileReq.setMargin(new Insets(0,0,0,0));
		butFileReq.addActionListener(this);
		butFileReq.setToolTipText("Request the file with this ID");
		butFileReq.setFont(sysFont);
		JLabel dash = new JLabel("-");
		txtFileId = new JTextField();
		txtFileId.setColumns(4);
		
		butFilter = new JButton();
		if (showFilter)
			butFilter.setText(SHOW_USER_LBL);
		else
			butFilter.setText(SHOW_ALL_LBL);

		butFilter.setMargin(new Insets(0,0,0,0));
		butFilter.addActionListener(this);
		butFilter.setToolTipText("Toggle ALL or User Files");
		butFilter.setFont(sysFont);
		
		butNew = new JButton("New Msg");
		butNew.setMargin(new Insets(0,0,0,0));
		butNew.addActionListener(this);
		butNew.setToolTipText("Create a new Message");
		if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
			butNew.setEnabled(false);			
		} else {
			butNew.setEnabled(true);
		}
		butNew.setFont(sysFont);
		
//		butLogin = new JButton("Login");
//		butLogin.setMargin(new Insets(0,0,0,0));
//		butLogin.addActionListener(this);
//		butLogin.setToolTipText("Assume spacecraft is open and attempt to login");
//
//		cbUplink = new JCheckBox("Uplink");
//		cbUplink.setSelected(Config.getBoolean(Config.UPLINK_ENABLED));
//		cbUplink.addActionListener(this);
//		
//		cbDownlink = new JCheckBox("Downlink");
//		cbDownlink.setSelected(Config.getBoolean(Config.DOWNLINK_ENABLED));
//		cbDownlink.addActionListener(this);
		
		topPanel.add(butNew);
		topPanel.add(bar2);

		topPanel.add(lblReq);
		topPanel.add(butDirReq);
		topPanel.add(butFileReq);
		topPanel.add(dash);
		topPanel.add(txtFileId);
//		topPanel.add(butLogin);
//		topPanel.add(cbUplink);
//		topPanel.add(cbDownlink);
		topPanel.add(bar);
		topPanel.add(butFilter);
		
		
		
	}
	
	private void makeCenterPanel() {
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout ());
		
		// Bottom has the log view
		JPanel centerBottomPanel = makeLogPanel();
		//centerPanel.add(centerBottomPanel, BorderLayout.SOUTH);
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		
		splitPaneHeight = Config.getInt(WINDOW_SPLIT_PANE_HEIGHT);
		
		spacecraftTabbedPanel = new JTabbedPane(JTabbedPane.LEFT);
		spacecraftTabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		spacecraftTabbedPanel.setFont(sysFont);
		
		for (SpacecraftSettings satSettings : Config.spacecraftSettings) {
			SpacecraftTab center = makeSpacecraftPanel(satSettings);
			spacecraftTabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>"+satSettings.name+"</body></html>", center );
		}
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				spacecraftTabbedPanel, centerBottomPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
		if (splitPaneHeight != 0) 
			splitPane.setDividerLocation(splitPaneHeight);
		else
			splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);

		SplitPaneUI spui = splitPane.getUI();
		if (spui instanceof BasicSplitPaneUI) {
			// Setting a mouse listener directly on split pane does not work, because no events are being received.
			((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					splitPaneHeight = splitPane.getDividerLocation();
					//Log.println("SplitPane: " + splitPaneHeight);
					Config.set(WINDOW_SPLIT_PANE_HEIGHT, splitPaneHeight);
				}
			});
		}
		centerPanel.add(splitPane, BorderLayout.CENTER);
	}
	
	private SpacecraftTab makeSpacecraftPanel(SpacecraftSettings spacecraftSettings) {
		
		SpacecraftTab centerPanel = new SpacecraftTab(spacecraftSettings);
		centerPanel.setLayout(new BorderLayout ());

		spacecraftTabs.put(spacecraftSettings.name, centerPanel);
		
		// Top panel has the buttons and options
//		JPanel centerTopPanel = makeFilePanel();
//		centerPanel.add(centerTopPanel, BorderLayout.NORTH);

		// Scroll panel holds the directory
		centerPanel.dirPanel = new DirectoryPanel(spacecraftSettings);
		centerPanel.outbox = new OutboxPanel(spacecraftSettings);
		
		//JTabbedPane tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
		centerPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		centerPanel.setFont(sysFont);
		
		addDirTabs(centerPanel);
		addTelemTabs(centerPanel, spacecraftSettings);

		//centerPanel.add(tabbedPanel, BorderLayout.CENTER);
		return centerPanel;	
	}
	
	public void refreshTabs(SpacecraftSettings spacecraftSettings) {
		SpacecraftTab tab = spacecraftTabs.get(spacecraftSettings.name);
		if (tab != null) {
			removeTabs(tab);
			addDirTabs(tab);
			addTelemTabs(tab, spacecraftSettings);   
		}
	}
	
	private void removeTabs(SpacecraftTab tab) {
		tab.remove(tab.dirPanel);
		//tabbedPanel.remove(systemDirPanel);
		tab.remove(tab.outbox);
		
		//////////////////////////////////////// This does not work
		wodPanel.stopProcessing();
		tab.remove(wodPanel);
		wodPanel = null;
		tlmIPanel.stopProcessing();
		tab.remove(tlmIPanel);
		tlmIPanel = null;
		tlm2Panel.stopProcessing();
		tab.remove(tlm2Panel);
		tlm2Panel = null;
	}
	
	private void addDirTabs(SpacecraftTab tab) {
		tab.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Directory</body></html>", tab.dirPanel );
		//tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>System Files</body></html>", systemDirPanel );
		tab.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Outbox</body></html>", tab.outbox );
	}
	
	private void addTelemTabs(JTabbedPane tabbedPanel, SpacecraftSettings spacecraftSettings) {
		ByteArrayLayout wodLayout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.WOD_LAYOUT);
		if (wodLayout != null) {
			TelemTab wodPanel = new TelemTab(wodLayout, spacecraftSettings.spacecraft, spacecraftSettings.db);
			Thread wodPanelThread = new Thread(wodPanel);
			wodPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
			wodPanelThread.setName("WODTab");
			wodPanelThread.start();
			tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>WOD</body></html>", wodPanel );
		}

		ByteArrayLayout tlmLayout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLMI_LAYOUT);
		if (tlmLayout != null) {
			TelemTab tlmIPanel = new TelemTab(tlmLayout, spacecraftSettings.spacecraft, spacecraftSettings.db);
			Thread telemIPanelThread = new Thread(tlmIPanel);
			telemIPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
			telemIPanelThread.setName("TLMItab");
			telemIPanelThread.start();
			tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>TLMI</body></html>", tlmIPanel );
		}

		ByteArrayLayout tlm2Layout = spacecraftSettings.spacecraft.getLayoutByName(SpacecraftSettings.TLMI_LAYOUT);
		if (tlm2Layout != null) {
			TelemTab tlm2Panel = new TelemTab(tlm2Layout, spacecraftSettings.spacecraft, spacecraftSettings.db);
			Thread telem2PanelThread = new Thread(tlm2Panel);
			telem2PanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
			telem2PanelThread.setName("TLM2tab");
			telem2PanelThread.start();
			tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>TLM2</body></html>", tlm2Panel );
		}
	}
	
	private JPanel makeFilePanel() {
		JPanel centerTopPanel = new JPanel();
		
		filePanel = new JPanel();
		filePanel.setVisible(false);
		centerTopPanel.add(filePanel);
		lblFileName = new JLabel();
		filePanel.add(lblFileName);
		return centerTopPanel;
	}

	
	private JPanel makeLogPanel() {
		JPanel centerBottomPanel = new JPanel();
		
		centerBottomPanel.setLayout(new BoxLayout(centerBottomPanel, BoxLayout.Y_AXIS));
		logTextArea = new JTextArea(10,135);
		logTextArea.setLineWrap(true);
		logTextArea.setWrapStyleWord(true);
		logTextArea.setEditable(false);
		// get the current font size
		//int size = Config.getInt(Config.FONT_SIZE);
		Font f = logTextArea.getFont();
		//if (size == 0) {
		//	size = f.getSize();
		//	Config.set(Config.FONT_SIZE, size);
		//}
		// create a new, smaller font from the current font
		Font f2 = new Font(f.getFontName(), f.getStyle(), (int) (fontSize));
		// set the new font in the editing area
		logTextArea.setFont(f2);
		DefaultCaret caret = (DefaultCaret)logTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		logTextArea.getDocument().addDocumentListener(
			    new LimitLinesDocumentListener(100) );
		JScrollPane logScroll = new JScrollPane (logTextArea);
		logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		logScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		centerBottomPanel.add(logScroll);
		
		return centerBottomPanel;
	}
	
	private void makeBottomPanel() {
		JPanel bottomPanel = new JPanel();
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BorderLayout ());
		JPanel satStatusPanel = makeSatStatusPanel();
		bottomPanel.add(satStatusPanel, BorderLayout.CENTER);
		JPanel statusPanel = makeStatusPanel();
		bottomPanel.add(statusPanel, BorderLayout.SOUTH);
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
		centerSat.add(lblPBStatus, BorderLayout.CENTER);
		lblPBStatus.setFont(sysFont);
			
		JPanel pbStatusPanel = new JPanel();
		pbStatusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		centerSat.add(pbStatusPanel, BorderLayout.SOUTH);
		lblDirHoles = new JLabel("DIR: ?? Holes");
		lblDirHoles.setFont(sysFont);
		lblDirHoles.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pbStatusPanel.add(lblDirHoles);
		
		JLabel bar = new JLabel("|");
		pbStatusPanel.add(bar);
		
		lblDownlinkStatus = new JLabel("Start");
		lblDownlinkStatus.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblDownlinkStatus.setFont(sysFont);
		pbStatusPanel.add(lblDownlinkStatus);

		JLabel bar2 = new JLabel("|");
		pbStatusPanel.add(bar2);
		
		lblEfficiency = new JLabel("Sent: 0 / Rec: 0 / Eff: 0%");
		lblEfficiency.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblEfficiency.setFont(sysFont);
		pbStatusPanel.add(lblEfficiency);
		
		JPanel rightSat = new JPanel();
		rightSat.setLayout(new BorderLayout());
		rightSat.setBorder(loweredbevel);
		satStatusPanel.add(rightSat, BorderLayout.EAST);
		lblPGStatus = new JLabel("Open: ??????");
		lblPGStatus.setFont(sysFont);
		rightSat.add(lblPGStatus, BorderLayout.CENTER);
		rightSat.add(new Box.Filler(new Dimension(10,40), new Dimension(400,40), new Dimension(1500,500)), BorderLayout.NORTH);
		
		JPanel pgStatusPanel = new JPanel();
		pgStatusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		rightSat.add(pgStatusPanel, BorderLayout.SOUTH);
		lblFileUploading = new JLabel("File: None");
		lblFileUploading.setFont(sysFont);
		this.setFileUploading(0, 0, 0);
		lblFileUploading.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pgStatusPanel.add(lblFileUploading);
		
		JLabel bar4 = new JLabel("|");
		pgStatusPanel.add(bar4);
		
		lblUplinkStatus = new JLabel("Init");
		lblUplinkStatus.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblUplinkStatus.setFont(sysFont);
		pgStatusPanel.add(lblUplinkStatus);

		JLabel bar3 = new JLabel("|");
		pgStatusPanel.add(bar3);
		
		lblLayer2Status = new JLabel("LAYER2: UNK");
		lblLayer2Status.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		lblLayer2Status.setFont(sysFont);
		pgStatusPanel.add(lblLayer2Status);

		
		return satStatusPanel;
	}
	
	private JPanel makeStatusPanel() {
		JPanel statusPanel = new JPanel();
		
		statusPanel.setLayout(new BorderLayout ());
		JPanel rightBottom = new JPanel();
		rightBottom.setLayout(new BoxLayout(rightBottom, BoxLayout.X_AXIS));
		
		lblVersion = new JLabel("Version " + Config.VERSION);
		Font bf = new Font(sysFont.getFontName(), sysFont.getStyle(), (int) (sysFont.getSize()*0.9));
		lblVersion.setFont(bf);
		lblVersion.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		statusPanel.add(lblVersion, BorderLayout.WEST);
		
		lblLogFileDir = new JLabel();
		updateLogfileDir();
		
		lblLogFileDir.setFont(bf);
		//lblLogFileDir.setMinimumSize(new Dimension(1600, 14)); // forces the next label to the right side of the screen
		//lblLogFileDir.setMaximumSize(new Dimension(1600, 14));
		lblLogFileDir.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		
		JPanel rightStatusPanel = new JPanel();
		rightStatusPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		statusPanel.add(rightStatusPanel, BorderLayout.EAST);
		
//		lblDCD = new JLabel("DATA");
//		lblDCD.setForeground(Color.GRAY);
//		rightStatusPanel.add(lblDCD);
//		lblDCD.setBorder(new EmptyBorder(2, 10, 2, 5) ); // top left bottom right
		
		JLabel bar2 = new JLabel("|");
		rightStatusPanel.add(bar2);
		
		if (Config.getBoolean(Config.KISS_TCP_INTERFACE))
			lblComPort = new JLabel(Config.get(Config.TNC_TCP_HOSTNAME)+":"+Config.get(Config.TNC_TCP_PORT));
		else
			lblComPort = new JLabel(Config.get(Config.TNC_COM_PORT));
		lblComPort.setBorder(new EmptyBorder(2, 5, 2, 5) ); // top left bottom right
		lblComPort.setToolTipText("TNC Connection");
		lblComPort.setFont(bf);
		rightStatusPanel.add(lblComPort);
		
		JLabel bar3 = new JLabel("|");
		rightStatusPanel.add(bar3);

//		JLabel lblFrames = new JLabel("Frames: ");
//		rightStatusPanel.add(lblFrames);
//		
//		lblFrames.setToolTipText("Total number of telemetry frames decoded");
//		lblFrames.setBorder(new EmptyBorder(2, 5, 2, 0) ); // top left bottom right
//		lblTotalFrames = new JLabel("0");
//		lblTotalFrames.setToolTipText("Total number of telemetry frames decoded");
//		rightStatusPanel.add(lblTotalFrames);
//		lblTotalFrames.setBorder(new EmptyBorder(2, 0, 2, 5) ); // top left bottom right

		JLabel lblTotal = new JLabel("Telem: ");
		lblTotal.setFont(bf);
		rightStatusPanel.add(lblTotal);
		lblTotal.setToolTipText("Total number of telemetry records decoded and stored");
		lblTotal.setBorder(new EmptyBorder(2, 5, 2, 0) ); // top left bottom right
		lblTotalTelem = new JLabel("0");
		lblTotalTelem.setToolTipText("Total number of telemetry records decoded and stored");
		lblTotalTelem.setFont(bf);
		rightStatusPanel.add(lblTotalTelem);
		lblTotalTelem.setBorder(new EmptyBorder(2, 0, 2, 5) ); // top left bottom right
		
		JLabel bar1 = new JLabel("/");
		rightStatusPanel.add(bar1);
		lblServerQueue = new JLabel("-");
		lblServerQueue.setBorder(new EmptyBorder(2, 5, 2, 10) ); // top left bottom right
		lblServerQueue.setToolTipText("Telemetry records to be sent to the AMSAT telemetry server");
		lblServerQueue.setFont(bf);
		rightStatusPanel.add(lblServerQueue);
		
		statusPanel.add(lblLogFileDir, BorderLayout.CENTER );
		
		return statusPanel;
	}
	
	public void updateLogfileDir() {
		if (Config.get(Config.LOGFILE_DIR).equals(""))
			lblLogFileDir.setText("Logs: Current Directory");
		else
			lblLogFileDir.setText("Logs: " + Config.get(Config.LOGFILE_DIR));
	}
	
//	public void setDCD(boolean dcd) {
//		if (dcd) 
//			lblDCD.setForeground(Color.RED);
//		else 
//			lblDCD.setForeground(Color.GRAY);
//	}
	
	public static void setFileUploading(long id, long offset, long length) {
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

	private void initMenu() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		Font f = mnFile.getFont();
		if (fontSize == 0) {
			fontSize = f.getSize();
			Config.set(Config.FONT_SIZE, fontSize);
		}
		sysFont = new Font(f.getFontName(), f.getStyle(), (int) (fontSize));
		mnFile.setFont(sysFont);
		menuBar.add(mnFile);
		
//		mntmDelete = new JMenuItem("Delete Payload Files");
//		mnFile.add(mntmDelete);
//		mntmDelete.addActionListener(this);
		mntmNewMsg = new JMenuItem("New Message");
		mntmNewMsg.setFont(sysFont);

		mnFile.addSeparator();
		
		mntmLoadKissFile = new JMenuItem("Load Kiss File");
		mntmLoadKissFile.setFont(sysFont);
		mnFile.setFont(sysFont);
		mnFile.add(mntmLoadKissFile);
		mntmLoadKissFile.addActionListener(this);

		if (!Config.isMacOs()) {
			mntmSettings = new JMenuItem("Settings");
			mntmSettings.setFont(sysFont);
			mnFile.add(mntmSettings);
			mntmSettings.addActionListener(this);
			mnFile.addSeparator();
		}
		mntmGetServerData = new JMenuItem("Fetch Server Data");
		mntmGetServerData.setFont(sysFont);
		mnFile.add(mntmGetServerData);
		mntmGetServerData.addActionListener(this);

		mntmArchiveDir = new JMenuItem("Archive the Directory");
		mntmArchiveDir.setFont(sysFont);
		mnFile.add(mntmArchiveDir);
		mntmArchiveDir.addActionListener(this);

		mntmExit = new JMenuItem("Exit");
		mntmExit.setFont(sysFont);
		mnFile.add(mntmExit);
		mntmExit.addActionListener(this);

/*		JMenu mnRequest = new JMenu("Request");
		menuBar.add(mnRequest);
		mntmReqDir = new JMenuItem("Directory");
		mnRequest.add(mntmReqDir);
		mntmReqDir.addActionListener(this);

		mntmReqFile = new JMenuItem("File");
		mnRequest.add(mntmReqFile);
		mntmReqFile.addActionListener(this);
*/
		JMenu mnSats = new JMenu("Spacecraft");
		mnSats.setFont(sysFont);
		menuBar.add(mnSats);
		mntmSat = new JMenuItem[Config.spacecraftSettings.size()];
		int i = 0;
		for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
			mntmSat[i] = new JMenuItem(spacecraftSettings.name);
			mntmSat[i].setFont(sysFont);
			mnSats.add(mntmSat[i]);
			mntmSat[i].addActionListener(this);
			i++;
		}
			
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setFont(sysFont);
		menuBar.add(mnHelp);

		mntmManual = new JMenuItem("Open Manual");
		mntmManual.setFont(sysFont);
		mnHelp.add(mntmManual);
		mntmManual.addActionListener(this);
		
		mntmLeaderboard = new JMenuItem("View Telemetry Leaderboard");
		mntmLeaderboard.setFont(sysFont);
		mnHelp.add(mntmLeaderboard);
		mntmLeaderboard.addActionListener(this);

		mntmWebsite = new JMenuItem("PacSat Website");
		mntmWebsite.setFont(sysFont);
		mnHelp.add(mntmWebsite);
		mntmWebsite.addActionListener(this);
		
		if (!Config.isMacOs()) {
			mntmAbout = new JMenuItem("About");
			mntmAbout.setFont(sysFont);
			mnHelp.add(mntmAbout);
			mntmAbout.addActionListener(this);
		}
	}

	public void shutdownWindow() {
		if (frameDecoder != null)
			frameDecoder.close();
		if (tncDecoder != null)
			tncDecoder.close();
		try {
			for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
				spacecraftSettings.directory.save();
			}
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not save the directory\n" + e.getMessage());
		}
		Log.println("Window Closed");
		Log.close();
		this.dispose();
		saveProperties();
		System.exit(0);	
	}
	
	public void saveProperties() {
		Config.set(MAINWINDOW_HEIGHT, this.getHeight());
		Config.set(MAINWINDOW_WIDTH, this.getWidth());
		Config.set(MAINWINDOW_X, this.getX());
		Config.set(MAINWINDOW_Y, this.getY());
		
		Config.save();
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) {

	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		shutdownWindow();
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		shutdownWindow();
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
	
	private void newMessage(SpacecraftSettings sat) {
		EditorFrame editor = null;
		editor = new EditorFrame(sat);
		editor.setVisible(true);
	}
	
	private void archiveDir(SpacecraftSettings spacecraftSettings) {
		String archiveDir = Config.get(Config.ARCHIVE_DIR);
		if (archiveDir.equalsIgnoreCase("")) {
			Log.errorDialog("Invalid archive directory", "Can not archive into the same folder as the current data\n"
					+ "Go to the File > Settings window and set a valid Archive folder.");
			
			return;
		}
		File archiveFile = new File(archiveDir + File.separator + spacecraftSettings.name + File.separator + Directory.DIR_FILE_NAME);
		if (archiveFile.exists()) {
			Log.errorDialog("Archive directory already exists", "This would overwrite the data in archive:  " + archiveDir +"\n"
					+ "Go to the File > Settings window and choose a new archive folder, or delete this data on disk before archiving.");
			
			return;
		}
		
		String message = "Move Headers and Files to Archive Folder?\n"
				+ "This will move your headers and files to the archive folder:  "  + archiveDir +"\nIt will keep "
				+ spacecraftSettings.get(SpacecraftSettings.NUMBER_DIR_TABLE_ENTRIES) + " headers and their files\n"
				+ "To archive more (or less) headers, adjust the number on the Spacecraft settings window\n\n"
				+ "To access the archive later change the log file folder on the File > Settings window, or \n"
				+ "run another copy of PacSatGround and pass in the archive folder path on the command line\n"
				+ "(In either case it is probablly best to enable 'Inhibit Transmitter' while browsing the archive)";
		Object[] options = {"Yes",
		"No"};
		int n = JOptionPane.showOptionDialog(
				MainWindow.frame,
				message,
				"Do you want to continue?",
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[1]);

		if (n == JOptionPane.NO_OPTION) {
			return;
		}
		ProgressPanel refreshProgress = new ProgressPanel(this, "Archiving to "+ archiveDir, false);
		refreshProgress.setVisible(true);
		
		spacecraftSettings.directory.archiveDir(archiveDir);
		setDirectoryData(spacecraftSettings.name, spacecraftSettings.directory.getTableData());
		refreshProgress.updateProgress(100);
	}
	
	private void replaceServerData(SpacecraftSettings spacecraftSettings) {

		if (Config.get(Config.LOGFILE_DIR).equalsIgnoreCase("")) {
			Log.errorDialog("CAN'T EXTRACT SERVER DATA INTO CURRENT DIRECTORY", "You can not replace the log files in the current directory.  "
					+ "Pick another directory from the settings menu\n");
			return;
					
		}
		String message = "Do you want to download server data to REPLACE your existing data?\n"
				+ "THIS WILL OVERWRITE YOUR EXISTING LOG FILES. Switch to a new directory if you have live data stored\n"
				+ "To import into into a different set of log files select NO, then choose a new log file directory from the settings menu";
		Object[] options = {"Yes",
		"No"};
		int n = JOptionPane.showOptionDialog(
				MainWindow.frame,
				message,
				"Do you want to continue?",
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[1]);

		if (n == JOptionPane.NO_OPTION) {
			return;
		}

		// Get the server data for each spacecraft we have
		//		sats = Config.satManager.getSpacecraftList();
		//		for (Spacecraft sat : sats) {
		// We can not rely on the name of the spacecraft being the same as the directory name on the server
		// because the user can change it.  So we have a hard coded routine to look it up
		//			String dir = getFoxServerDir(sat.foxId);
		String dir = spacecraftSettings.spacecraft.name;
		if (dir == null) {
			// no server data for this satellite.  Skip
		} else {
			downloadServerData(spacecraftSettings, dir);
		}

		ProgressPanel refreshProgress = new ProgressPanel(this, "refreshing tabs ...", false);
		refreshProgress.setVisible(true);
		
		try {
			spacecraftSettings.initSegDb(spacecraftSettings.spacecraft);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Could not reload the Telemetry Layouts: " + e.getMessage());
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not reload the Telemetry Data: " + e.getMessage());
		}
		spacecraftSettings.initDirectory();
		refreshTabs(spacecraftSettings);
		spacecraftSettings.directory.setShowFiles(showFilter);
		setDirectoryData(spacecraftSettings.name, spacecraftSettings.directory.getTableData());
		setOutboxData(spacecraftSettings.name, spacecraftSettings.outbox.getTableData());
		// We are fully updated, remove the database loading message
		refreshProgress.updateProgress(100);
		
	}

private void downloadServerData(SpacecraftSettings spacecraftSettings, String dir) {
	String file = "TLMDB.tar.gz";
	file = Config.get(Config.LOGFILE_DIR) + File.separator + spacecraftSettings.name + File.separator + file;
	// We have the dir, so pull down the file
	ProgressPanel fileProgress = new ProgressPanel(this, "Downloading " + dir + " data, please wait ...", false);
	fileProgress.setVisible(true);

	String urlString = Config.get(Config.WEB_SITE_URL) + "/" + dir + "/TLMDB.tar.gz";
	try {
		URL website = new URL(urlString);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos;
		fos = new FileOutputStream(file);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	} catch (FileNotFoundException e) {
		// The file was not found on the server.  This is probablly because there was no data for this spacecraft
		
		Log.errorDialog("ERROR", "Reading the server data for: " + file + "\n" +
				e.getMessage());
		e.printStackTrace(Log.getWriter());
		fileProgress.updateProgress(100);
		return;
	} catch (MalformedURLException e) {
		Log.errorDialog("ERROR", "ERROR can't access the server data at: " + urlString );
		e.printStackTrace(Log.getWriter());
		fileProgress.updateProgress(100);
		return;
	} catch (IOException e) {
		Log.errorDialog("ERROR", "ERROR reading/writing the server data from server: " + file  + "\n+"
				+ e.getMessage() );
		e.printStackTrace(Log.getWriter());
		fileProgress.updateProgress(100);
		return;
	}

	fileProgress.updateProgress(100);
	File archive = new File(file);

	if (archive.length() == 0) {
		// No data in file, so we skip
	} else {
		ProgressPanel decompressProgress = new ProgressPanel(this, "decompressing " + dir + " data ...", false);
		decompressProgress.setVisible(true);

		// Now decompress it and expand
		File destination = new File(Config.get(Config.LOGFILE_DIR) + File.separator + spacecraftSettings.name);

		Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
		try {
			archiver.extract(archive, destination);
		} catch (IOException e) {
			Log.errorDialog("ERROR", "ERROR could not uncompress the server data\n+"
					+ e.getMessage() );
			e.printStackTrace(Log.getWriter());
			decompressProgress.updateProgress(100);
			return;
		} catch (IllegalArgumentException e) {
			Log.errorDialog("ERROR", "ERROR could not uncompress the server data\n+"
					+ e.getMessage() );
			e.printStackTrace(Log.getWriter());
			decompressProgress.updateProgress(100);
			return;
		}

		decompressProgress.updateProgress(100);
	}

}
	
	public void setFrames(int amount) {
        SwingUtilities.invokeLater(new Runnable() {
        	public void run() {
        		lblTotalFrames.setText(""+amount);
        	}
        });

	}
	
	public void setEfficiency(int sent, int received) {
		if (sent == 0) return; // avoid divide by zero error if no bytes received since last check even though we have a B: packet
        SwingUtilities.invokeLater(new Runnable() {
        	public void run() {
        		double eff = 100* received/(double)sent;
        		lblEfficiency.setText("Sent: " + sent + " / Rec: " + received + " / Eff: " +  String.format("%.1f", eff) + "%");
        	}
        });

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			// periodic update
			int num = Config.stpQueue.getSize();
			lblServerQueue.setText(""+num);
			int total = 0;
			for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
				total += spacecraftSettings.db.getNumberOfFrames();
			}
			lblTotalTelem.setText(""+total);
		}
		if (e.getSource() == mntmLoadKissFile) {
			loadFile();
		}
		if (e.getSource() == mntmGetServerData) {
			for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
				replaceServerData(spacecraftSettings);
			}
		}
		if (e.getSource() == mntmArchiveDir) {
			for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
				archiveDir(spacecraftSettings);
			}
		}
		if (e.getSource() == mntmNewMsg) {
			
			// TODO - this is hard coded because it is the only spacecraft we can currently send to
			SpacecraftSettings spacecraftSettings = Config.getSatSettingsByName("FalconSat-3");
			newMessage(spacecraftSettings);
		}
		if (e.getSource() == mntmExit) {
			windowClosed(null);
		}
		if (e.getSource() == mntmSettings) {
			SettingsFrame f = new SettingsFrame(this, true);
			f.setVisible(true);
		}
		
		for (int i=0; i < mntmSat.length; i++) {
			if (e.getSource() == mntmSat[i]) {
				String n = mntmSat[i].getText();
				SpacecraftSettings spacecraftSettings = Config.getSatSettingsByName(n);
				SpacecraftFrame f = new SpacecraftFrame(spacecraftSettings, this, true);
				f.setVisible(true);
			}
		}
		if (e.getSource() == mntmManual) {
			try {
				DesktopApi.browse(new URI(HelpAbout.MANUAL));
			} catch (URISyntaxException ex) {
				//It looks like there's a problem
				ex.printStackTrace();
			}

		}
		if (e.getSource() == mntmLeaderboard) {
			try {
				DesktopApi.browse(new URI(HelpAbout.LEADERBOARD));
			} catch (URISyntaxException ex) {
				//It looks like there's a problem
				ex.printStackTrace();
			}

		}
		if (e.getSource() == mntmWebsite) {
			try {
				DesktopApi.browse(new URI(HelpAbout.SOFTWARE));
			} catch (URISyntaxException ex) {
				//It looks like there's a problem
				ex.printStackTrace();
			}

		}
		
		if (e.getSource() == mntmAbout) {
			HelpAbout help = new HelpAbout(this, true);
			help.setVisible(true);
		}
		if (e.getSource() == butNew) {
			// TODO - this is hard coded because it is the only spacecraft we can currently send to
			SpacecraftSettings spacecraftSettings = Config.getSatSettingsByName("FalconSat-3");
			newMessage(spacecraftSettings);
		}
		if (e.getSource() == butDirReq) {
			if (!Config.getBoolean(Config.TX_INHIBIT)) {
				// TODO - this is hard coded because it is the only spacecraft we can currently send to
				SpacecraftSettings spacecraftSettings = Config.getSatSettingsByName("FalconSat-3");
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
			if (!Config.getBoolean(Config.TX_INHIBIT)) {
			// Make a DIR Request Frame and Send it to the TNC for TX
				// TODO - this is hard coded because it is the only spacecraft we can currently send to
				SpacecraftSettings spacecraftSettings = Config.getSatSettingsByName("FalconSat-3");
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
			if (butFilter.getText().equalsIgnoreCase(SHOW_USER_LBL)) {
				butFilter.setText(SHOW_ALL_LBL);
				showFilter = SHOW_ALL;
			} else {
				butFilter.setText(SHOW_USER_LBL);
				showFilter = SHOW_USER;
			}
			Log.println("SHOW: " + showFilter);
			
			Config.set(Config.SHOW_USER_FILES, showFilter);
			for (SpacecraftSettings spacecraftSettings : Config.spacecraftSettings) {
				spacecraftSettings.directory.setShowFiles(showFilter);
				setDirectoryData(spacecraftSettings.name, spacecraftSettings.directory.getTableData());
			}
		}
//		if (e.getSource() == butLogin) {
//			Config.uplink.attemptLogin();
//		}
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
	
	public boolean loadFile() {
		File file = null;
		File dir = null;
		String d = Config.get(WINDOW_CURRENT_DIR);
		if (d == null)
			dir = new File(".");
		else
			if (d != "") {
				dir = new File(Config.get(WINDOW_CURRENT_DIR));
			}

		if(Config.getBoolean(Config.USE_NATIVE_FILE_CHOOSER)) {

			if (dir != null) {
				fd.setDirectory(dir.getAbsolutePath());
			}
			fd.setVisible(true);
			String filename = fd.getFile();
			String dirname = fd.getDirectory();
			if (filename == null)
				Log.println("You cancelled the choice");
			else {
				Log.println("File: " + filename);
				Log.println("DIR: " + dirname);
				file = new File(dirname + filename);
			}
		} else {
			if (Config.getInt(WINDOW_FC_WIDTH) == 0) {
				Config.set(WINDOW_FC_WIDTH, 600);
				Config.set(WINDOW_FC_HEIGHT, 600);
			}
			fc.setPreferredSize(new Dimension(Config.getInt(WINDOW_FC_WIDTH), Config.getInt(WINDOW_FC_HEIGHT)));
			if (dir != null)
				fc.setCurrentDirectory(dir);

			int returnVal = fc.showOpenDialog(this);
			Config.set(WINDOW_FC_HEIGHT, fc.getHeight());
			Config.set(WINDOW_FC_WIDTH,fc.getWidth());		
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
			}
		}

		if (file != null) {
			Config.set(WINDOW_CURRENT_DIR, file.getParent());	
			// TODO - this needs to go on the right tab.  Comment out for now
//			lblFileName.setText(file.getName());
//			filePanel.setVisible(true);
			
			initDecoder(file.getAbsolutePath());
			
			return true;
		}
		return false;
	}

	

}
