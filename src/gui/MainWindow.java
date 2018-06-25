package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
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
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultCaret;

import pacSat.TncDecoder;
import pacSat.frames.RequestDirFrame;
import passControl.PacSatEvent;
import common.Config;
import common.DesktopApi;
import common.Log;
import common.Spacecraft;

@SuppressWarnings("serial")
public class MainWindow extends JFrame implements ActionListener, WindowListener, MouseListener {

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
	static JLabel lblPBStatus;
	static JLabel lblPGStatus;
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
	static JMenuItem mntmReqDir;

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
		if (Config.directory.getTableData().length > 0)
			setDirectoryData(Config.directory.getTableData());
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
	
	public static void setPBStatus(String pb) {
		lblPBStatus.setText(pb);
	}

	public static void setPGStatus(String pg) {
		lblPGStatus.setText(pg);
	}

	private void initDecoder(String fileName) {
		tncDecoder = new TncDecoder(logTextArea, fileName);
		Config.downlink.setTncDecoder(tncDecoder);
		tncDecoderThread = new Thread(tncDecoder);
		tncDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		tncDecoderThread.start();
	}
	private void initDecoder() {
		tncDecoder = new TncDecoder(Config.get(Config.TNC_COM_PORT), logTextArea);
		Config.downlink.setTncDecoder(tncDecoder);
		tncDecoderThread = new Thread(tncDecoder);
		tncDecoderThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		tncDecoderThread.start();
	}
	
	private void makeCenterPanel() {
		JPanel centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout ());
		JPanel centerBottomPanel = makeLogPanel();
		centerPanel.add(centerBottomPanel, BorderLayout.SOUTH);
		JScrollPane scrollPane = makeDirPanel();
		centerPanel.add(scrollPane, BorderLayout.CENTER);
		JPanel centerTopPanel = makeFilePanel();
		centerPanel.add(centerTopPanel, BorderLayout.NORTH);
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
	private JScrollPane makeDirPanel() {	
		fileHeaderTableModel = new FileHeaderTableModel();
		directoryTable = new JTable(fileHeaderTableModel);
		directoryTable.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane (directoryTable, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		directoryTable.setFillsViewportHeight(true);
		directoryTable.setAutoResizeMode(JTable. AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		
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
		directoryTable.addMouseListener(this);
		String PREV = "prev";
		String NEXT = "next";
		String ENTER = "enter";
		InputMap inMap = directoryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		inMap.put(KeyStroke.getKeyStroke("ENTER"), ENTER);
		ActionMap actMap = directoryTable.getActionMap();

		actMap.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = directoryTable.getSelectedRow();
				if (row > 0)
					directoryTable.setRowSelectionInterval(row-1, row-1);
			}
		});
		actMap.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = directoryTable.getSelectedRow();
				if (row < directoryTable.getRowCount()-1)
					directoryTable.setRowSelectionInterval(row+1, row+1);
			}
		});
		actMap.put(ENTER, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("ENTER");
				int row = directoryTable.getSelectedRow();
				if (row > 0 && row < directoryTable.getRowCount()-1)
					displayRow(directoryTable,row);        
			}
		});
		return scrollPane;
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
		centerSat.setLayout(new FlowLayout(FlowLayout.LEFT));
		centerSat.setBorder(loweredbevel);
		satStatusPanel.add(centerSat, BorderLayout.CENTER );
		lblPBStatus = new JLabel("PB: ?????? ");
		centerSat.add(lblPBStatus);

		JPanel rightSat = new JPanel();
		rightSat.setLayout(new FlowLayout(FlowLayout.LEFT));
		rightSat.setBorder(loweredbevel);
		satStatusPanel.add(rightSat, BorderLayout.EAST );
		lblPGStatus = new JLabel("Open: ??????");
		rightSat.add(lblPGStatus);
		rightSat.add(new Box.Filler(new Dimension(10,40), new Dimension(150,40), new Dimension(500,500)));
		
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
		statusPanel.add(lblLogFileDir, BorderLayout.CENTER );
		return statusPanel;
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

		JMenu mnTest = new JMenu("Test");
		menuBar.add(mnTest);
		mntmReqDir = new JMenuItem("Request Directory");
		mnTest.add(mntmReqDir);
		mntmReqDir.addActionListener(this);
		
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
			SettingsFrame f = new SettingsFrame(this, true);
			f.setVisible(true);
		}
		if (e.getSource() == mntmReqDir) {
			// Make a DIR Request Frame and Send it to the TNC for TX
			RequestDirFrame dirFrame = new RequestDirFrame(Config.get(Config.CALLSIGN), Config.spacecraft.get(Spacecraft.BBS_CALLSIGN), true, null);
			Config.downlink.processEvent(dirFrame);
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
			
			initDecoder(file.getAbsolutePath());
			
			return true;
		}
		return false;
	}

	protected void displayRow(JTable table, int row) {
		String id = (String) table.getValueAt(row, 0);
		Log.println("Open file: " +id + ".act");
		File f = new File("C:/Users/chris/Desktop/workspace/Falcon/" + id + ".act");
		if (f.exists())
			DesktopApi.edit(f);
    	table.setRowSelectionInterval(row, row);
	}

	public void mouseClicked(MouseEvent e) {
		int row = directoryTable.rowAtPoint(e.getPoint());
		int col = directoryTable.columnAtPoint(e.getPoint());
		if (row >= 0 && col >= 0) {
			Log.println("CLICKED ROW: "+row+ " and COL: " + col);
			displayRow(directoryTable, row);
		}
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
