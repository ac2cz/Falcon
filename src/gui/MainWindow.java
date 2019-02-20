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
	static JLabel lblLogFileDir;
	JTextArea logTextArea;
	JLabel lblFileName;
	static JLabel lblPBStatus;
	static JLabel lblPGStatus;
	static JLabel lblFileUploading;
	static JLabel lblUplinkStatus;
	static JLabel lblLayer2Status;
	JPanel filePanel;
	public DirectoryPanel dirPanel;
	public DirectoryPanel systemDirPanel;
	OutboxPanel outbox;
	TelemTab wodPanel;
	TelemTab tlmIPanel;
	TelemTab tlm2Panel;
	
	JButton butDirReq;
	JButton butFileReq;
	static JTextField txtFileId;
//	static JButton butFilter;
	static JButton butNew;
	static JButton butLogin;
	JCheckBox cbUplink, cbDownlink;
	
	public static final boolean SHOW_ALL = false;
	public static final boolean SHOW_USER = true;
	JTabbedPane tabbedPanel;
	int splitPaneHeight = DEFAULT_DIVIDER_LOCATION;
	
	TncDecoder tncDecoder;
	Thread tncDecoderThread;
	FrameDecoder frameDecoder;
	Thread frameDecoderThread;
	
	// Status indicators
	JLabel lblDownlinkStatus;
	JLabel lblComPort;
	JLabel lblServerQueue;
	static JLabel lblDCD;
	static JLabel lblDirHoles;
	
	// Menu items
	static JMenuItem mntmNewMsg, mntmGetServerData;
	static JMenuItem mntmExit;
	static JMenuItem mntmLoadKissFile;
	static JMenuItem mntmSettings;
	static JMenuItem mntmManual;
	static JMenuItem mntmAbout;
	static JMenuItem mntmFs3;
	static JMenuItem mntmReqDir;
	
	Timer timer;

	public MainWindow() {
		frame = this; // a handle for error dialogues
		initialize();
		
		if (Config.isMacOs()) {
			macApplication = com.apple.eawt.Application.getApplication();
			macApplication.setAboutHandler(new MacAboutHandler());
			macApplication.setPreferencesHandler(new MacPreferencesHandler());
			macApplication.setQuitHandler(new MacQuitHandler(this));
		}
		
		if (Config.spacecraftSettings.directory.getTableData().length > 0) 
			setDirectoryData(Config.spacecraftSettings.directory.getTableData());
		if (Config.spacecraftSettings.outbox.getTableData() != null) 
			setOutboxData(Config.spacecraftSettings.outbox.getTableData());
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
	
	public void setDirectoryData(String[][] data) {
		dirPanel.setDirectoryData(data);
		systemDirPanel.setDirectoryData(data);
	}
	public void setOutboxData(String[][] data) {
		outbox.setDirectoryData(data);
	}
	
	public static void setPBStatus(String pb) {
		lblPBStatus.setText(pb);
	}

	public static void setPGStatus(String pg) {
		lblPGStatus.setText(pg);
	}

	/**
	 * Start the TNC interface and read bytes from a file.
	 * @param fileName
	 */
	private void initDecoder(String fileName) {
		if (frameDecoder != null) {
			frameDecoder.close();
		}
		frameDecoder = new FrameDecoder(logTextArea);
		frameDecoderThread = new Thread(frameDecoder);
		frameDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		frameDecoderThread.setName("Frame Decoder");
		frameDecoderThread.start();


		if (tncDecoder != null) {
			tncDecoder.close();
		}
		tncDecoder = new SerialTncDecoder(frameDecoder, logTextArea, fileName);
		Config.downlink.setTncDecoder(tncDecoder, logTextArea);
		Config.uplink.setTncDecoder(tncDecoder, logTextArea);
		Config.layer2data.setTncDecoder(tncDecoder, logTextArea);
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
		frameDecoder = new FrameDecoder(logTextArea);
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
			tncDecoder = new TcpTncDecoder(hostname, port,frameDecoder, logTextArea);
		} else {
			String com = Config.get(Config.TNC_COM_PORT);
			if (com == null) return;
			tncDecoder = new SerialTncDecoder(com, Config.getInt(Config.TNC_BAUD_RATE), Config.getInt(Config.TNC_DATA_BITS), 
					Config.getInt(Config.TNC_STOP_BITS), Config.getInt(Config.TNC_PARITY),frameDecoder, logTextArea);
		}
		Config.downlink.setTncDecoder(tncDecoder, logTextArea);
		Config.uplink.setTncDecoder(tncDecoder, logTextArea);
		Config.layer2data.setTncDecoder(tncDecoder, logTextArea);
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

		JLabel bar = new JLabel("|");
		JLabel bar2 = new JLabel("|");
		
		JLabel lblReq = new JLabel("Request: ");
		butFileReq = new JButton("FILE");
		butFileReq.setMargin(new Insets(0,0,0,0));
		butFileReq.addActionListener(this);
		butFileReq.setToolTipText("Request the file with this ID");
		JLabel dash = new JLabel("-");
		txtFileId = new JTextField();
		txtFileId.setColumns(4);
		
//		butFilter = new JButton(SHOW_ALL);
//		butFilter.setMargin(new Insets(0,0,0,0));
//		butFilter.addActionListener(this);
//		butFilter.setToolTipText("Toggle ALL or User Files");

		butNew = new JButton("New Msg");
		butNew.setMargin(new Insets(0,0,0,0));
		butNew.addActionListener(this);
		butNew.setToolTipText("Create a new Message");
		if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
			butNew.setEnabled(false);			
		} else {
			butNew.setEnabled(true);
		}
		
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
//		topPanel.add(butFilter);
		
		
		
	}
	
	private void makeCenterPanel() {
		
		JPanel centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout ());

		
		// Top panel has the buttons and options
		JPanel centerTopPanel = makeFilePanel();
		centerPanel.add(centerTopPanel, BorderLayout.NORTH);

		// Scroll panel holds the directory
		dirPanel = new DirectoryPanel(SHOW_USER);
		systemDirPanel = new DirectoryPanel(SHOW_ALL);
		outbox = new OutboxPanel(SHOW_USER);
		
		
		
		// Bottom has the log view
		JPanel centerBottomPanel = makeLogPanel();
		//centerPanel.add(centerBottomPanel, BorderLayout.SOUTH);

		splitPaneHeight = Config.getInt(WINDOW_SPLIT_PANE_HEIGHT);
		tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
		tabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		
		addDirTabs();
		addTelemTabs();

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				tabbedPanel, centerBottomPanel);
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
	
	public void refreshTabs() {
		removeTabs();
		addDirTabs();
		addTelemTabs();
	}
	
	private void removeTabs() {
		tabbedPanel.remove(dirPanel);
		tabbedPanel.remove(systemDirPanel);
		tabbedPanel.remove(outbox);
		wodPanel.stopProcessing();
		tabbedPanel.remove(wodPanel);
		wodPanel = null;
		tlmIPanel.stopProcessing();
		tabbedPanel.remove(tlmIPanel);
		tlmIPanel = null;
		tlm2Panel.stopProcessing();
		tabbedPanel.remove(tlm2Panel);
		tlm2Panel = null;
	}
	
	private void addDirTabs() {
		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Directory</body></html>", dirPanel );
		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>System Files</body></html>", systemDirPanel );
		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Outbox</body></html>", outbox );
	}
	
	private void addTelemTabs() {
		wodPanel = new TelemTab(Config.spacecraft.getLayoutByName(SpacecraftSettings.WOD_LAYOUT), Config.spacecraft, Config.db);
		Thread wodPanelThread = new Thread(wodPanel);
		wodPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodPanelThread.setName("WODTab");
		wodPanelThread.start();

		tlmIPanel = new TelemTab(Config.spacecraft.getLayoutByName(SpacecraftSettings.TLMI_LAYOUT), Config.spacecraft, Config.db);
		Thread telemIPanelThread = new Thread(tlmIPanel);
		telemIPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		telemIPanelThread.setName("TLMItab");
		telemIPanelThread.start();

		tlm2Panel = new TelemTab(Config.spacecraft.getLayoutByName(SpacecraftSettings.TLM2_LAYOUT), Config.spacecraft, Config.db);
		Thread telem2PanelThread = new Thread(tlm2Panel);
		telem2PanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		telem2PanelThread.setName("TLM2tab");
		telem2PanelThread.start();

		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>WOD</body></html>", wodPanel );
		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>TLMI</body></html>", tlmIPanel );
		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>TLM2</body></html>", tlm2Panel );

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
		// get the current font
		Font f = logTextArea.getFont();
		// create a new, smaller font from the current font
		Font f2 = new Font(f.getFontName(), f.getStyle(), f.getSize()-2);
		// set the new font in the editing area
		logTextArea.setFont(f2);
		DefaultCaret caret = (DefaultCaret)logTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		logTextArea.getDocument().addDocumentListener(
			    new LimitLinesDocumentListener(3000) );
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
		
		JPanel pbStatusPanel = new JPanel();
		pbStatusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		centerSat.add(pbStatusPanel, BorderLayout.SOUTH);
		lblDirHoles = new JLabel("DIR: ?? Holes");
		lblDirHoles.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pbStatusPanel.add(lblDirHoles);
		
		JLabel bar = new JLabel("|");
		pbStatusPanel.add(bar);
		
		lblDownlinkStatus = new JLabel("Start");
		lblDownlinkStatus.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pbStatusPanel.add(lblDownlinkStatus);

		
		JPanel rightSat = new JPanel();
		rightSat.setLayout(new BorderLayout());
		rightSat.setBorder(loweredbevel);
		satStatusPanel.add(rightSat, BorderLayout.EAST);
		lblPGStatus = new JLabel("Open: ??????");
		rightSat.add(lblPGStatus, BorderLayout.CENTER);
		rightSat.add(new Box.Filler(new Dimension(10,40), new Dimension(400,40), new Dimension(1500,500)), BorderLayout.NORTH);
		
		JPanel pgStatusPanel = new JPanel();
		pgStatusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		rightSat.add(pgStatusPanel, BorderLayout.SOUTH);
		lblFileUploading = new JLabel("File: None");
		lblFileUploading.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pgStatusPanel.add(lblFileUploading);
		
		JLabel bar2 = new JLabel("|");
		pgStatusPanel.add(bar2);
		
		lblUplinkStatus = new JLabel("Init");
		lblUplinkStatus.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pgStatusPanel.add(lblUplinkStatus);

		JLabel bar3 = new JLabel("|");
		pgStatusPanel.add(bar3);
		
		lblLayer2Status = new JLabel("LAYER2: UNK");
		lblLayer2Status.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		pgStatusPanel.add(lblLayer2Status);

		
		return satStatusPanel;
	}
	
	private JPanel makeStatusPanel() {
		JPanel statusPanel = new JPanel();
		
		statusPanel.setLayout(new BorderLayout ());
		JPanel rightBottom = new JPanel();
		rightBottom.setLayout(new BoxLayout(rightBottom, BoxLayout.X_AXIS));
		
		lblVersion = new JLabel("Version " + Config.VERSION);
		lblVersion.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblVersion.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		statusPanel.add(lblVersion, BorderLayout.WEST);
		
		lblLogFileDir = new JLabel();
		updateLogfileDir();
		lblLogFileDir.setFont(new Font("SansSerif", Font.BOLD, 10));
		//lblLogFileDir.setMinimumSize(new Dimension(1600, 14)); // forces the next label to the right side of the screen
		//lblLogFileDir.setMaximumSize(new Dimension(1600, 14));
		lblLogFileDir.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		
		JPanel rightStatusPanel = new JPanel();
		rightStatusPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		statusPanel.add(rightStatusPanel, BorderLayout.EAST);
		
		lblDCD = new JLabel("DATA");
		lblDCD.setForeground(Color.GRAY);
		rightStatusPanel.add(lblDCD);
		lblDCD.setBorder(new EmptyBorder(2, 10, 2, 5) ); // top left bottom right
		
		JLabel bar2 = new JLabel("|");
		rightStatusPanel.add(bar2);
		
		if (Config.getBoolean(Config.KISS_TCP_INTERFACE))
			lblComPort = new JLabel(Config.get(Config.TNC_TCP_HOSTNAME)+":"+Config.get(Config.TNC_TCP_PORT));
		else
			lblComPort = new JLabel(Config.get(Config.TNC_COM_PORT));
		lblComPort.setBorder(new EmptyBorder(2, 5, 2, 5) ); // top left bottom right
		lblComPort.setToolTipText("TNC Connection");
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
		rightStatusPanel.add(lblTotal);
		lblTotal.setToolTipText("Total number of telemetry records decoded and stored");
		lblTotal.setBorder(new EmptyBorder(2, 5, 2, 0) ); // top left bottom right
		lblTotalTelem = new JLabel("0");
		lblTotalTelem.setToolTipText("Total number of telemetry records decoded and stored");
		rightStatusPanel.add(lblTotalTelem);
		lblTotalTelem.setBorder(new EmptyBorder(2, 0, 2, 5) ); // top left bottom right
		
		JLabel bar1 = new JLabel("/");
		rightStatusPanel.add(bar1);
		lblServerQueue = new JLabel("-");
		lblServerQueue.setBorder(new EmptyBorder(2, 5, 2, 10) ); // top left bottom right
		lblServerQueue.setToolTipText("Telemetry records to be sent to the AMSAT telemetry server");
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
	
	public void setDCD(boolean dcd) {
		if (dcd) 
			lblDCD.setForeground(Color.RED);
		else 
			lblDCD.setForeground(Color.GRAY);
	}

	private void initMenu() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
//		mntmDelete = new JMenuItem("Delete Payload Files");
//		mnFile.add(mntmDelete);
//		mntmDelete.addActionListener(this);
		mntmNewMsg = new JMenuItem("New Message");

		mnFile.addSeparator();
		
		mntmLoadKissFile = new JMenuItem("Load Kiss File");
		mnFile.add(mntmLoadKissFile);
		mntmLoadKissFile.addActionListener(this);

		if (!Config.isMacOs()) {
			mntmSettings = new JMenuItem("Settings");
			mnFile.add(mntmSettings);
			mntmSettings.addActionListener(this);
			mnFile.addSeparator();
		}
		mntmGetServerData = new JMenuItem("Fetch Server Data");
		mnFile.add(mntmGetServerData);
		mntmGetServerData.addActionListener(this);

		mntmExit = new JMenuItem("Exit");
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
		menuBar.add(mnSats);
		mntmFs3 = new JMenuItem(Config.spacecraftSettings.name);
		mnSats.add(mntmFs3);
		mntmFs3.addActionListener(this);
		
//		ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
//		mntmSat = new JMenuItem[sats.size()];
//		for (int i=0; i<sats.size(); i++) {
//			mntmSat[i] = new JMenuItem(sats.get(i).name);
//			mnSats.add(mntmSat[i]);
//			mntmSat[i].addActionListener(this);
//		}
			
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		mntmManual = new JMenuItem("Open Manual");
		mnHelp.add(mntmManual);
		mntmManual.addActionListener(this);

		if (!Config.isMacOs()) {
			mntmAbout = new JMenuItem("About");
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
			Config.spacecraftSettings.directory.save();
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
	
	private void newMessage() {
		EditorFrame editor = null;
		editor = new EditorFrame();
		editor.setVisible(true);
	}
	
	private void replaceServerData() {

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
		String dir = Config.spacecraft.name;
		if (dir == null) {
			// no server data for this satellite.  Skip
		} else {
			downloadServerData(dir);
		}

		ProgressPanel refreshProgress = new ProgressPanel(this, "refreshing tabs ...", false);
		refreshProgress.setVisible(true);
		
		try {
			Config.initSegDb();
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Could not reload the Telemetry Layouts: " + e.getMessage());
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Could not reload the Telemetry Data: " + e.getMessage());
		}
		Config.spacecraftSettings.initDirectory();
		refreshTabs();
		setDirectoryData(Config.spacecraftSettings.directory.getTableData());
		setOutboxData(Config.spacecraftSettings.outbox.getTableData());
		// We are fully updated, remove the database loading message
		refreshProgress.updateProgress(100);
		
	}

private void downloadServerData(String dir) {
	String file = "TLMDB.tar.gz";
	file = Config.get(Config.LOGFILE_DIR) + File.separator + Config.spacecraftSettings.name + File.separator + file;
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
		File destination = new File(Config.get(Config.LOGFILE_DIR) + File.separator + Config.spacecraftSettings.name);

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

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			// periodic update
			int num = Config.stpQueue.getSize();
			lblServerQueue.setText(""+num);
			lblTotalTelem.setText(""+Config.db.getNumberOfFrames());
		}
		if (e.getSource() == mntmLoadKissFile) {
			loadFile();
		}
		if (e.getSource() == mntmGetServerData) {
			replaceServerData();
		}
		if (e.getSource() == mntmNewMsg) {
			newMessage();
		}
		if (e.getSource() == mntmExit) {
			windowClosed(null);
		}
		if (e.getSource() == mntmSettings) {
			SettingsFrame f = new SettingsFrame(this, true);
			f.setVisible(true);
		}
		
		if (e.getSource() == mntmFs3) {
			SpacecraftFrame f = new SpacecraftFrame(Config.spacecraftSettings, this, true);
			f.setVisible(true);
		}
		if (e.getSource() == mntmManual) {
			try {
				DesktopApi.browse(new URI(HelpAbout.MANUAL));
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
			newMessage();
		}
		if (e.getSource() == butDirReq) {
			if (!Config.getBoolean(Config.TX_INHIBIT)) {
			// Make a DIR Request Frame and Send it to the TNC for TX
			RequestDirFrame dirFrame = null;
			SortedArrayList<DirHole> holes = Config.spacecraftSettings.directory.getHolesList();
			if (holes != null) {
			//for (DirHole hole : holes)
			//	Log.println("" + hole);
				dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraftSettings.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, holes);
			} else {
				Log.errorDialog("ERROR", "Something has gone wrong and the directory holes file is missing or corrupt\nCan't request the directory\n");
				//Date fromDate = Config.spacecraft.directory.getLastHeaderDate();
				//dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, fromDate);
			}
			Config.downlink.processEvent(dirFrame);
			} else {
				Log.errorDialog("Transmitted Disabled", "Directory can't be requested when the transmitter is inhibitted\n"
						+ "Disable 'Inhibit Tranmitter' on the settings tab" );

			}
		}
		if (e.getSource() == butFileReq) {
			if (!Config.getBoolean(Config.TX_INHIBIT)) {
			// Make a DIR Request Frame and Send it to the TNC for TX
			String fileIdstr = txtFileId.getText(); 
			if (fileIdstr == null || fileIdstr.length() == 0 || fileIdstr.length() > 4)
				Log.errorDialog("File Request Error", "File Id should be 1-4 digits in HEX.  Invalid: " + fileIdstr);
			else {
				try {
					long fileId = Long.decode("0x" + fileIdstr);
					PacSatFile psf = new PacSatFile(Config.spacecraftSettings.directory.dirFolder, fileId);
					////// IF THE HOLE LIST IS TOO LARGE THIS WILL BLOW UP!
					////// FIRST INSTANCE WHERE I NEED TO KNOW THE DEFAULT BLOCK SIZE AND HENCE MAX HOLE LIST LENGTH
					SortedArrayList<FileHole> holes = psf.getHolesList();
					//for (FileHole fh : holes)
					//	Log.print(fh + " ");
					//Log.println("");
					RequestFileFrame fileFrame = new RequestFileFrame(Config.get(Config.CALLSIGN), Config.spacecraftSettings.get(SpacecraftSettings.BROADCAST_CALLSIGN), true, fileId, holes);
					Config.downlink.processEvent(fileFrame);
				} catch (NumberFormatException ne) {
					Log.errorDialog("File Request Error", "File Id should be 1-4 digits in HEX. Invalid: " + fileIdstr);
				}
			}
			} else {
				Log.errorDialog("Transmitted Disabled", "File Id can't be requested when the transmitter is inhibitted\n"
						+ "Disable 'Inhibit Tranmitter' on the settings tab" );
			}
		}
//		if (e.getSource() == butFilter) {
//			if (butFilter.getText().equalsIgnoreCase(SHOW_USER))
//				butFilter.setText(SHOW_ALL);
//			else
//				butFilter.setText(SHOW_USER);
//			setDirectoryData(Config.spacecraft.directory.getTableData());
//		}
		if (e.getSource() == butLogin) {
			Config.uplink.attemptLogin();
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
			lblFileName.setText(file.getName());
			filePanel.setVisible(true);
			
			initDecoder(file.getAbsolutePath());
			
			return true;
		}
		return false;
	}

	

}
