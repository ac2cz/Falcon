package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import javax.swing.*;

import common.Config;
import common.Log;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

@SuppressWarnings("serial")
public class EditorFrame extends JFrame implements ActionListener, WindowListener {

	int type = 0;
	int compressionType = 0;
	
	private JTextArea ta;
	private int count;
	private JMenuBar menuBar;
	private JMenu fileM,editM,viewM;
	private JScrollPane scpane;
	private JMenuItem exitI,cutI,copyI,pasteI,selectI,saveI,loadI,statusI;
	private String pad;
	private JToolBar toolBar;

	JTextField txtTo, txtFrom, txtDate, txtTitle, txtKeywords;
	JLabel lblCrDate;
	JComboBox cbType;
	JPanel centerpane; // the main display area for text and images
	ImagePanel image;
	
	private PacSatFile psf;
	private PacSatFileHeader pfh;
	private boolean editable = true;
	public static final boolean READ_ONLY = false;
	public static final boolean EDITABLE = true;

	public static final String EDIT_WINDOW_X = "edit_window_x";
	public static final String EDIT_WINDOW_Y = "edit_window_y";
	public static final String EDIT_WINDOW_WIDTH = "edit_window_width";
	public static final String EDIT_WINDOW_HEIGHT = "edit_window_height";

	
	/**
	 * Call to create a new file
	 */
	public EditorFrame() {
		super("Message Editor");
		editable = true;
		saveI = new JMenuItem("Save and Exit"); //menuitems
		makeFrame(editable);
		loadI = new JMenuItem("Load"); //menuitems
		fileM.add(loadI);
		loadI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
		loadI.addActionListener(this);
		
		addTextArea();
		scpane.setVisible(true);
	}
	
	public EditorFrame(PacSatFile file) throws IOException {
		super("Message Viewer");
		psf = file;
		editable = false;
		saveI = new JMenuItem("Save As"); //menuitems
		makeFrame(editable);
		
		pfh = psf.getPfh();
		txtTo.setText(pfh.getFieldString(PacSatFileHeader.DESTINATION));
		txtFrom.setText(pfh.getFieldString(PacSatFileHeader.SOURCE));
		txtTitle.setText(pfh.getFieldString(PacSatFileHeader.TITLE));
		txtKeywords.setText(pfh.getFieldString(PacSatFileHeader.KEYWORDS));
		lblCrDate.setText("Created: " + pfh.getDateString(PacSatFileHeader.CREATE_TIME) + " UTC");
		cbType.setSelectedIndex(PacSatFileHeader.getTypeIdByString(pfh.getTypeString()));
		String ty = pfh.getTypeString();
		type = pfh.getType();
		int j = PacSatFileHeader.getTypeIndexByString(ty);
		cbType.setSelectedIndex(j);
		if (ty.equalsIgnoreCase("JPG")) {
			addImageArea();
			image.setBufferedImage(psf.getBytes());
		} else {
			addTextArea();
			ta.append(pfh.toFullString());
			ta.append(psf.getText());
			ta.setCaretPosition(0);
		}
	}
	
	private void addTextArea() {
		ta = new JTextArea(); //textarea
		scpane = new JScrollPane(ta); //scrollpane  and add textarea to scrollpane
		ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setEditable(editable);
		ta.setVisible(true);
		centerpane.add(scpane,BorderLayout.CENTER);
	}
	
	private void addImageArea() {
		image = new ImagePanel();
		centerpane.add(image,BorderLayout.CENTER);
	}
	
	private void makeFrame(boolean edit) {
		
		addWindowListener(this);
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("../images/pacsat.jpg")));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());

		count = 0;
		pad = " ";

		menuBar = new JMenuBar(); //menubar
		fileM = new JMenu("File"); //file menu
		editM = new JMenu("Edit"); //edit menu
		viewM = new JMenu("View"); //edit menu

		exitI = new JMenuItem("Exit");
		cutI = new JMenuItem("Cut");
		copyI = new JMenuItem("Copy");
		pasteI = new JMenuItem("Paste");
		selectI = new JMenuItem("Select All"); //menuitems
		
		
		statusI = new JMenuItem("Status"); //menuitems
		toolBar = new JToolBar();

		setJMenuBar(menuBar);
		menuBar.add(fileM);
		menuBar.add(editM);
		menuBar.add(viewM);

		fileM.add(saveI);
		fileM.add(exitI);

		editM.add(cutI);
		editM.add(copyI);
		editM.add(pasteI);        
		editM.add(selectI);

		viewM.add(statusI);

		saveI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		cutI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		copyI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		pasteI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		selectI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));

		// on ESC key close frame
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel"); //$NON-NLS-1$
		getRootPane().getActionMap().put("Cancel", new AbstractAction(){ //$NON-NLS-1$
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});

		centerpane = new JPanel();
		pane.add(centerpane,BorderLayout.CENTER);
		centerpane.setLayout(new BorderLayout());

		// Pacsat File Header
		JPanel header = new JPanel();
		header.setLayout(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JPanel headerFields = new JPanel();
		header.add(headerFields, BorderLayout.CENTER);
		JPanel header1 = new JPanel();
		JPanel header2 = new JPanel();
		JPanel header3 = new JPanel();

		headerFields.setLayout(new BoxLayout(headerFields, BoxLayout.Y_AXIS));
		headerFields.add(header1);
		headerFields.add(header2);
		headerFields.add(header3);
		header1.setLayout(new BoxLayout(header1, BoxLayout.X_AXIS)); // FlowLayout(FlowLayout.LEFT));
		header2.setLayout(new BoxLayout(header2, BoxLayout.X_AXIS)); //FlowLayout(FlowLayout.LEFT));
		header3.setLayout(new BoxLayout(header3, BoxLayout.X_AXIS)); //FlowLayout(FlowLayout.LEFT));
		
		header1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		header2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		header3.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		JLabel lblTo = new JLabel("To:    ");
		txtTo = new JTextField();
		txtTo.setColumns(20);
		txtTo.setEditable(edit);
		header1.add(lblTo);
		header1.add(new Box.Filler(new Dimension(10,10), new Dimension(23,20), new Dimension(23,20)));
		header1.add(txtTo);

		header1.add(new Box.Filler(new Dimension(10,10), new Dimension(200,20), new Dimension(1000,20)));

		cbType = new JComboBox(PacSatFileHeader.typeStrings);
		cbType.setEnabled(edit);
		cbType.addActionListener(this);
		header1.add(cbType);

		JLabel lblFrom = new JLabel("From: ");
		txtFrom = new JTextField();			
		txtFrom.setColumns(20);
		txtFrom.setEditable(edit);
		header2.add(lblFrom);
		header2.add(new Box.Filler(new Dimension(10,10), new Dimension(20,20), new Dimension(30,20)));
		header2.add(txtFrom);

		header2.add(new Box.Filler(new Dimension(10,10), new Dimension(10,20), new Dimension(23,20)));
		JLabel lblKeywords = new JLabel("Keywords: ");
		txtKeywords = new JTextField();			
		txtKeywords.setColumns(30);
		txtKeywords.setEditable(edit);
		header2.add(lblKeywords);
		header2.add(new Box.Filler(new Dimension(10,10), new Dimension(24,20), new Dimension(23,20)));
		header2.add(txtKeywords);

		JLabel lblTitle = new JLabel("Title: ");
		txtTitle = new JTextField();			
		txtTitle.setColumns(60);
		txtTitle.setEditable(edit);
		header3.add(lblTitle);
		header3.add(new Box.Filler(new Dimension(10,10), new Dimension(24,20), new Dimension(23,20)));
		header3.add(txtTitle);

		JPanel headerRight = new JPanel();
		headerRight.setLayout(new BorderLayout());
		header.add(headerRight, BorderLayout.EAST);
		lblCrDate = new JLabel();		
		headerRight.add(lblCrDate, BorderLayout.NORTH);
		centerpane.add(header,BorderLayout.NORTH);

		pane.add(toolBar,BorderLayout.SOUTH);

		saveI.addActionListener(this);
		exitI.addActionListener(this);
		cutI.addActionListener(this);
		copyI.addActionListener(this);
		pasteI.addActionListener(this);
		selectI.addActionListener(this);
		statusI.addActionListener(this);

		setVisible(true);
	}
	
	private void savePacsatFile() {

		// build the pacsatfile header then create file with header and text
		int bodySize = 0;
		String s = ta.getText();
		bodySize = s.length();
		short bodyChecksum = PacSatFileHeader.checksum(s.getBytes());
		PacSatFileHeader pfh = new PacSatFileHeader(txtFrom.getText(), txtTo.getText(), bodySize, bodyChecksum, type, compressionType, txtTitle.getText(), txtKeywords.getText(), "AC2CZ55.OUT");

		File file = null;
		File dir = null;
		String d = Config.get(Config.LOGFILE_DIR);
		if (d == null)
			dir = new File(".");
		else
			if (d != "") {
				dir = new File(Config.get(Config.LOGFILE_DIR));
			}
		
		String filename = Config.get(Config.CALLSIGN) + "55.out";

		Log.println("File: " + filename);
		Log.println("DIR: " + dir.getAbsolutePath());
		file = new File(dir.getAbsolutePath() + File.separator + filename);

		if (file != null) {
			FileOutputStream saveFile = null;
			Config.set(MainWindow.WINDOW_CURRENT_DIR, file.getParent());					
			try {
				saveFile = new FileOutputStream(file);
				for (int i : pfh.getBytes())
					saveFile.write(i);
				saveFile.write(ta.getText().getBytes());
			} catch (FileNotFoundException e) {
				Log.errorDialog("ERROR", "Error with file name: " + file.getAbsolutePath() + "\n" + e.getMessage());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Error writing file: " + file.getAbsolutePath() + "\n" + e.getMessage());
			} finally {
				try { saveFile.close(); } catch (Exception e) {}
			}
		}
	}

	private void saveFile() {

		File file = null;
		File dir = null;
		String d = Config.get(MainWindow.WINDOW_CURRENT_DIR);
		if (d == null)
			dir = new File(".");
		else
			if (d != "") {
				dir = new File(Config.get(MainWindow.WINDOW_CURRENT_DIR));
			}

		if(Config.getBoolean(Config.USE_NATIVE_FILE_CHOOSER)) {
			FileDialog fd = new FileDialog(this, "Save As", FileDialog.SAVE);

			// use the native file dialog on the mac

			if (dir != null) {
				fd.setDirectory(dir.getAbsolutePath());
			}
			fd.setVisible(true);
			String filename = fd.getFile();
			String dirname = fd.getDirectory();
			if (filename == null)
				;//Log.println("You cancelled the choice");
			else {
				Log.println("File: " + filename);
				Log.println("DIR: " + dirname);
				file = new File(dirname + filename);
			}
		} else {
			JFileChooser fc = new JFileChooser();
			fc.setApproveButtonText("Save");
			if (Config.getInt(MainWindow.WINDOW_FC_WIDTH) == 0) {
				Config.set(MainWindow.WINDOW_FC_WIDTH, 600);
				Config.set(MainWindow.WINDOW_FC_HEIGHT, 600);
			}
			fc.setPreferredSize(new Dimension(Config.getInt(MainWindow.WINDOW_FC_WIDTH), Config.getInt(MainWindow.WINDOW_FC_HEIGHT)));
			if (dir != null)
				fc.setCurrentDirectory(dir);

			int returnVal = fc.showOpenDialog(this);
			Config.set(MainWindow.WINDOW_FC_HEIGHT, fc.getHeight());
			Config.set(MainWindow.WINDOW_FC_WIDTH,fc.getWidth());		
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
			}
		}

		if (file != null) {
			Config.set(MainWindow.WINDOW_CURRENT_DIR, file.getParent());					
			try {
				RandomAccessFile saveFile = new RandomAccessFile(file, "rw");
				saveFile.write(psf.getBytes());
			} catch (FileNotFoundException e) {
				Log.errorDialog("ERROR", "Error with file name: " + file.getAbsolutePath() + "\n" + e.getMessage());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Error writing file: " + file.getAbsolutePath() + "\n" + e.getMessage());
			}
		}
	}

	private void processTypeSelection(String ty) {
		type = PacSatFileHeader.getTypeIdByString(ty);
		if (ty.equalsIgnoreCase("JPG") || ty.equalsIgnoreCase("GIF")) {
			// Neeed to pick the image
			scpane.setVisible(false);
			addImageArea();
		} else {
			//scpane.setVisible(true);
		}
		
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == saveI) {
			if (editable) 
				savePacsatFile();
			else
				saveFile();
		}
		else if (e.getSource() == exitI)
			dispose();
		else if (e.getSource() == cutI) {
			pad = ta.getSelectedText();
			ta.replaceRange("", ta.getSelectionStart(), ta.getSelectionEnd());
		}
		else if (e.getSource() == copyI)
			pad = ta.getSelectedText();
		else if (e.getSource() == pasteI)
			ta.insert(pad, ta.getCaretPosition());
		else if (e.getSource() == selectI)
			ta.selectAll();
		else if (e.getSource() == statusI) {
		}
		if (e.getSource() == cbType) {
			int i = cbType.getSelectedIndex();
			processTypeSelection(PacSatFileHeader.getTypeStringByIndex(i));
		}
	}

	public void saveProperties() {
		Config.set(EDIT_WINDOW_HEIGHT, this.getHeight());
		Config.set(EDIT_WINDOW_WIDTH, this.getWidth());
		Config.set(EDIT_WINDOW_X, this.getX());
		Config.set(EDIT_WINDOW_Y, this.getY());

		Config.save();
	}

	public void loadProperties() {
		if (Config.getInt(EDIT_WINDOW_X) == 0) {
			Config.set(EDIT_WINDOW_X, 100);
			Config.set(EDIT_WINDOW_Y, 100);
			Config.set(EDIT_WINDOW_HEIGHT, 600);
			Config.set(EDIT_WINDOW_WIDTH, 500);
		}
		setBounds(Config.getInt(EDIT_WINDOW_X), Config.getInt(EDIT_WINDOW_Y), 
				Config.getInt(EDIT_WINDOW_WIDTH), Config.getInt(EDIT_WINDOW_HEIGHT));
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		saveProperties();
	}
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

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