package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultCaret;

import pacSat.TncDecoder;
import common.Config;
import common.Log;

@SuppressWarnings("serial")
public class MainWindow extends JFrame implements ActionListener, WindowListener {

	public static final String MAINWINDOW_X = "mainwindow_x";
	public static final String MAINWINDOW_Y = "mainwindow_y";
	public static final String MAINWINDOW_WIDTH = "mainwindow_width";
	public static final String MAINWINDOW_HEIGHT = "mainwindow_height";
	public static final String WINDOW_FC_WIDTH = "mainwindow_width";
	public static final String WINDOW_FC_HEIGHT = "mainwindow_height";
	public static final String WINDOW_CURRENT_DIR = "mainwindow_current_dir";
	
	public static JFrame frame;
	// Swing File Chooser
	JFileChooser fc = null;
	//AWT file chooser for the Mac
	FileDialog fd = null;
	JLabel lblVersion;
	static JLabel lblLogFileDir;
	JTextArea logTextArea;
	JLabel lblFileName;
	JPanel filePanel;
	FileHeaderTableModel fileHeaderTableModel;
	JTable directoryTable;
	
	TncDecoder tncDecoder;
	Thread tncDecoderThread;
	
	// Menu items
	static JMenuItem mntmExit;
	static JMenuItem mntmLoadKissFile;
	static JMenuItem mntmSettings;
	static JMenuItem mntmManual;
	static JMenuItem mntmAbout;
	static JMenuItem mntmFs3;

	public MainWindow() {
		frame = this; // a handle for error dialogues
		initialize();
		/*
		if (Config.isMacOs()) {
			macApplication = com.apple.eawt.Application.getApplication();
			macApplication.setAboutHandler(new MacAboutHandler());
			macApplication.setPreferencesHandler(new MacPreferencesHandler());
			macApplication.setQuitHandler(new MacQuitHandler(this));
		}
		*/
		//initDecoder();
	}
	
	private void initialize() {
		if (Config.getInt(MAINWINDOW_X) == 0) {
			Config.set(MAINWINDOW_X, 100);
			Config.set(MAINWINDOW_Y, 100);
			Config.set(MAINWINDOW_WIDTH, 800);
			Config.set(MAINWINDOW_HEIGHT, 800);
		}
		setBounds(Config.getInt(MAINWINDOW_X), Config.getInt(MAINWINDOW_Y), 
				Config.getInt(MAINWINDOW_WIDTH), Config.getInt(MAINWINDOW_HEIGHT));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle("AMSAT PacSat Ground Station");
		initMenu();
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
	
	public void setDirectoryData(String[][] data) {
		fileHeaderTableModel.setData(data);
	}
	
	private void initDecoder(String fileName) {
		tncDecoder = new TncDecoder(logTextArea, fileName);
		tncDecoderThread = new Thread(tncDecoder);
		tncDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		tncDecoderThread.start();
	}
	private void initDecoder() {
		tncDecoder = new TncDecoder("COM8", logTextArea);
		tncDecoderThread = new Thread(tncDecoder);
		tncDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		tncDecoderThread.start();
	}
	
	private void makeCenterPanel() {
		JPanel centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout ());
		
		JPanel centerBottomPanel = new JPanel();
		centerPanel.add(centerBottomPanel, BorderLayout.SOUTH);
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
				
		fileHeaderTableModel = new FileHeaderTableModel();
		directoryTable = new JTable(fileHeaderTableModel);
		directoryTable.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane (directoryTable, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		directoryTable.setFillsViewportHeight(true);
		directoryTable.setAutoResizeMode(JTable. AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		centerPanel.add(scrollPane, BorderLayout.CENTER);
		TableColumn column = directoryTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(55);
		column = directoryTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(35);
		column = directoryTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(55);
		column = directoryTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(55);
		column = directoryTable.getColumnModel().getColumn(4);
		column.setPreferredWidth(80);
		column = directoryTable.getColumnModel().getColumn(5);
		column.setPreferredWidth(55);
		column = directoryTable.getColumnModel().getColumn(6);
		column.setPreferredWidth(35);
		column = directoryTable.getColumnModel().getColumn(7);
		column.setPreferredWidth(200);
		column = directoryTable.getColumnModel().getColumn(8);
		column.setPreferredWidth(60);
		
		JPanel centerTopPanel = new JPanel();
		centerPanel.add(centerTopPanel, BorderLayout.NORTH);
		filePanel = new JPanel();
		filePanel.setVisible(false);
		centerTopPanel.add(filePanel);
		lblFileName = new JLabel();
		filePanel.add(lblFileName);
		
	}
	
	private void makeBottomPanel() {
		JPanel bottomPanel = new JPanel();
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BorderLayout ());
		JPanel rightBottom = new JPanel();
		rightBottom.setLayout(new BoxLayout(rightBottom, BoxLayout.X_AXIS));
		
		lblVersion = new JLabel("Version " + Config.VERSION);
		lblVersion.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblVersion.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		bottomPanel.add(lblVersion, BorderLayout.WEST);
		
		if (Config.get(Config.LOGFILE_DIR).equals(""))
			lblLogFileDir = new JLabel("Logs: Current Directory");
		else
			lblLogFileDir = new JLabel("Logs: " + Config.get(Config.LOGFILE_DIR));
		lblLogFileDir.setFont(new Font("SansSerif", Font.BOLD, 10));
		//lblLogFileDir.setMinimumSize(new Dimension(1600, 14)); // forces the next label to the right side of the screen
		//lblLogFileDir.setMaximumSize(new Dimension(1600, 14));
		lblLogFileDir.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		bottomPanel.add(lblLogFileDir, BorderLayout.CENTER );
	}

	private void initMenu() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
//		mntmDelete = new JMenuItem("Delete Payload Files");
//		mnFile.add(mntmDelete);
//		mntmDelete.addActionListener(this);

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

		
		JMenu mnSats = new JMenu("Spacecraft");
		menuBar.add(mnSats);
		mntmFs3 = new JMenuItem("FalconSat");
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
		if (tncDecoder != null)
			tncDecoder.close();
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
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mntmLoadKissFile) {
			loadFile();
		}
		if (e.getSource() == mntmExit) {
			this.windowClosed(null);
		}
		if (e.getSource() == mntmSettings) {
	//		SettingsFrame f = new SettingsFrame(this, true);
	//		f.setVisible(true);
		}
		if (e.getSource() == mntmFs3) {
			initDecoder();
		}
		if (e.getSource() == mntmManual) {
//			try {
//				DesktopApi.browse(new URI(HelpAbout.MANUAL));
//			} catch (URISyntaxException ex) {
//				//It looks like there's a problem
//				ex.printStackTrace();
//			}

		}
		if (e.getSource() == mntmAbout) {
//			HelpAbout help = new HelpAbout(this, true);
//			help.setVisible(true);
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
			
			initDecoder(file.getName());
			
			return true;
		}
		return false;
	}

}
