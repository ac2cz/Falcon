package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultCaret;

import com.apple.eawt.Application;

import pacSat.FrameDecoder;
import pacSat.TncDecoder;
import pacSat.frames.RequestDirFrame;
import pacSat.frames.RequestFileFrame;
import common.Config;
import common.DesktopApi;
import common.Log;
import common.Spacecraft;
import fileStore.DirHole;
import fileStore.FileHole;
import fileStore.Outbox;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;
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
	
	public static final String WINDOW_SPLIT_PANE_HEIGHT = "window_split_pane_height";
	
	public static final int DEFAULT_DIVIDER_LOCATION = 450;
	
	static Application macApplication;
	
	public static JFrame frame;
	// Swing File Chooser
	JFileChooser fc = null;
	//AWT file chooser for the Mac
	FileDialog fd = null;
	JLabel lblVersion;
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
	OutboxPanel outbox;
	
	JButton butDirReq;
	JButton butFileReq;
	static JTextField txtFileId;
	static JButton butFilter;
	static JButton butNew;
	static JButton butLogin;
	JCheckBox cbUplink, cbDownlink;
	
	public static final String SHOW_ALL = "All Files";
	public static final String SHOW_USER = "User Files";
	
	int splitPaneHeight = DEFAULT_DIVIDER_LOCATION;
	
	TncDecoder tncDecoder;
	Thread tncDecoderThread;
	FrameDecoder frameDecoder;
	Thread frameDecoderThread;
	
	// Status indicators
	JLabel lblDownlinkStatus;
	JLabel lblComPort;
	static JLabel lblDCD;
	static JLabel lblDirHoles;
	
	// Menu items
	static JMenuItem mntmNewMsg;
	static JMenuItem mntmExit;
	static JMenuItem mntmLoadKissFile;
	static JMenuItem mntmSettings;
	static JMenuItem mntmManual;
	static JMenuItem mntmAbout;
	static JMenuItem mntmFs3;
	static JMenuItem mntmReqDir;

	public MainWindow() {
		frame = this; // a handle for error dialogues
		initialize();
		
		if (Config.isMacOs()) {
			macApplication = com.apple.eawt.Application.getApplication();
			macApplication.setAboutHandler(new MacAboutHandler());
			macApplication.setPreferencesHandler(new MacPreferencesHandler());
			macApplication.setQuitHandler(new MacQuitHandler(this));
		}
		
		if (Config.spacecraft.directory.getTableData().length > 0) 
			setDirectoryData(Config.spacecraft.directory.getTableData());
		if (Config.spacecraft.outbox.getTableData() != null) 
			setOutboxData(Config.spacecraft.outbox.getTableData());
		initDecoder();
	}
	
	private void initialize() {
		if (Config.getInt(MAINWINDOW_X) == 0) {
			Config.set(MAINWINDOW_X, 100);
			Config.set(MAINWINDOW_Y, 100);
			Config.set(MAINWINDOW_WIDTH, 680);
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
		
		fd = new FileDialog(MainWindow.frame, "Select Wav file",FileDialog.LOAD);
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
		tncDecoder = new TncDecoder(frameDecoder, logTextArea, fileName);
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
		String com = Config.get(Config.TNC_COM_PORT);
		if (com == null) return;
		tncDecoder = new TncDecoder(com, Config.getInt(Config.TNC_BAUD_RATE), Config.getInt(Config.TNC_DATA_BITS), 
				Config.getInt(Config.TNC_STOP_BITS), Config.getInt(Config.TNC_PARITY),frameDecoder, logTextArea);
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
		
		butFilter = new JButton(SHOW_ALL);
		butFilter.setMargin(new Insets(0,0,0,0));
		butFilter.addActionListener(this);
		butFilter.setToolTipText("Toggle ALL or User Files");

		butNew = new JButton("New Msg");
		butNew.setMargin(new Insets(0,0,0,0));
		butNew.addActionListener(this);
		butNew.setToolTipText("Create a new Message");
		if (Config.get(Config.CALLSIGN).equalsIgnoreCase(Config.DEFAULT_CALLSIGN)) {
			butNew.setEnabled(false);			
		} else {
			butNew.setEnabled(true);
		}
		
		butLogin = new JButton("Login");
		butLogin.setMargin(new Insets(0,0,0,0));
		butLogin.addActionListener(this);
		butLogin.setToolTipText("Assume spacecraft is open and attempt to login");

		cbUplink = new JCheckBox("Uplink");
		cbUplink.setSelected(Config.getBoolean(Config.UPLINK_ENABLED));
		cbUplink.addActionListener(this);
		
		cbDownlink = new JCheckBox("Downlink");
		cbDownlink.setSelected(Config.getBoolean(Config.DOWNLINK_ENABLED));
		cbDownlink.addActionListener(this);
		
		topPanel.add(butNew);
		topPanel.add(bar2);

		topPanel.add(lblReq);
		topPanel.add(butDirReq);
		topPanel.add(butFileReq);
		topPanel.add(dash);
		topPanel.add(txtFileId);
		topPanel.add(butLogin);
		topPanel.add(cbUplink);
		topPanel.add(cbDownlink);
		topPanel.add(bar);
		topPanel.add(butFilter);
		
		
		
	}
	
	private void makeCenterPanel() {
		
		JPanel centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout ());

		
		// Top panel has the buttons and options
		JPanel centerTopPanel = makeFilePanel();
		centerPanel.add(centerTopPanel, BorderLayout.NORTH);

		// Scroll panel holds the directory
		dirPanel = new DirectoryPanel();
		outbox = new OutboxPanel();
		
		// Bottom has the log view
		JPanel centerBottomPanel = makeLogPanel();
		//centerPanel.add(centerBottomPanel, BorderLayout.SOUTH);
		
		JTabbedPane tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
		tabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		
		splitPaneHeight = Config.getInt(WINDOW_SPLIT_PANE_HEIGHT);
		
		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Directory</body></html>", dirPanel );
		tabbedPanel.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Outbox</body></html>", outbox );


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
		
		if (Config.get(Config.LOGFILE_DIR).equals(""))
			lblLogFileDir = new JLabel("Logs: Current Directory");
		else
			lblLogFileDir = new JLabel("Logs: " + Config.get(Config.LOGFILE_DIR));
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
		
		JLabel bar2 = new JLabel("|");
		rightStatusPanel.add(bar2);
		
		lblComPort = new JLabel(Config.get(Config.TNC_COM_PORT));
		lblComPort.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		rightStatusPanel.add(lblComPort);
		
		statusPanel.add(lblLogFileDir, BorderLayout.CENTER );
		
		
		
		return statusPanel;
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
		mntmFs3 = new JMenuItem(Config.spacecraft.name);
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
			Config.spacecraft.directory.save();
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
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mntmLoadKissFile) {
			loadFile();
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
			SpacecraftFrame f = new SpacecraftFrame(Config.spacecraft, this, true);
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
			// Make a DIR Request Frame and Send it to the TNC for TX
			RequestDirFrame dirFrame = null;
			SortedArrayList<DirHole> holes = Config.spacecraft.directory.getHolesList();
			if (holes != null) {
			//for (DirHole hole : holes)
			//	Log.println("" + hole);
				dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, holes);
			} else {
				Log.errorDialog("ERROR", "Something has gone wrong and the directory holes file is missing or corrupt\nCan't request the directory\n");
				//Date fromDate = Config.spacecraft.directory.getLastHeaderDate();
				//dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, fromDate);
			}
			Config.downlink.processEvent(dirFrame);
		}
		if (e.getSource() == butFileReq) {
			// Make a DIR Request Frame and Send it to the TNC for TX
			String fileIdstr = txtFileId.getText(); 
			if (fileIdstr == null || fileIdstr.length() == 0 || fileIdstr.length() > 4)
				Log.errorDialog("File Request Error", "File Id should be 1-4 digits in HEX.  Invalid: " + fileIdstr);
			else {
				try {
					long fileId = Long.decode("0x" + fileIdstr);
					PacSatFile psf = new PacSatFile(Config.spacecraft.directory.dirFolder, fileId);
					////// IF THE HOLE LIST IS TOO LARGE THIS WILL BLOW UP!
					////// FIRST INSTANCE WHERE I NEED TO KNOW THE DEFAULT BLOCK SIZE AND HENCE MAX HOLE LIST LENGTH
					SortedArrayList<FileHole> holes = psf.getHolesList();
					//for (FileHole fh : holes)
					//	Log.print(fh + " ");
					//Log.println("");
					RequestFileFrame fileFrame = new RequestFileFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BROADCAST_CALLSIGN), true, fileId, holes);
					Config.downlink.processEvent(fileFrame);
				} catch (NumberFormatException ne) {
					Log.errorDialog("File Request Error", "File Id should be 1-4 digits in HEX. Invalid: " + fileIdstr);
				}
			}
		}
		if (e.getSource() == butFilter) {
			if (butFilter.getText().equalsIgnoreCase(SHOW_USER))
				butFilter.setText(SHOW_ALL);
			else
				butFilter.setText(SHOW_USER);
			setDirectoryData(Config.spacecraft.directory.getTableData());
		}
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
			// use the native file dialog on the mac

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
