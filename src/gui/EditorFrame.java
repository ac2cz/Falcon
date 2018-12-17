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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private String filename;

	JTextField txtTo, txtFrom, txtDate, txtTitle, txtKeywords;
	JButton butReply, butSave, butDelete, butExit;
	JLabel lblCrDate;
	JComboBox cbType;
	JPanel centerpane; // the main display area for text and images
	JPanel editPane; // where the content is displayed
	ImagePanel imagePanel;
	byte[] imageBytes;
	
	private PacSatFile psf;
	private PacSatFileHeader pfh;
	private boolean editable = true;
	public static final boolean READ_ONLY = false;
	public static final boolean EDITABLE = true;

	public static final String EDIT_WINDOW_X = "edit_window_x";
	public static final String EDIT_WINDOW_Y = "edit_window_y";
	public static final String EDIT_WINDOW_WIDTH = "edit_window_width";
	public static final String EDIT_WINDOW_HEIGHT = "edit_window_height";

	public static final int IMAGE_SIZE_LIMIT = 250000; // need some sort of sensible limit to prevent files that are too large being uploaded
	
	
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
		txtFrom.setText(Config.get(Config.CALLSIGN));
		txtFrom.setEditable(false);
		addTextArea();
		addImageArea();		
		
		scpane.setVisible(true);
		ta.append(".... select document type to edit");
		ta.setVisible(true);
		ta.setEditable(false);

///		((CardLayout)editPane.getLayout()).minimumLayoutSize(editPane);
	}
	
	/**
	 * Call to reply to a message
	 * @param toCallsign
	 * @param fromCallsign
	 * @param title
	 * @param keywords
	 * @param origText
	 */
	public EditorFrame(String toCallsign, String fromCallsign, String title, String keywords, String origText) {
		super("Message Editor");
		editable = true;

		saveI = new JMenuItem("Save and Exit"); //menuitems
		makeFrame(editable);
		loadI = new JMenuItem("Load"); //menuitems
		fileM.add(loadI);
		loadI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
		loadI.addActionListener(this);
		txtFrom.setText(Config.get(Config.CALLSIGN));
		txtFrom.setEditable(false);
		
		txtTo.setText(toCallsign.toUpperCase());
		txtTitle.setText(title);
		txtKeywords.setText(keywords);
///		lblCrDate.setText("Created: " + pfh.getDateString(PacSatFileHeader.CREATE_TIME) + " UTC");
		cbType.setSelectedIndex(PacSatFileHeader.getTypeIndexByString("ASCII"));
		addTextArea();
		ta.append(origText);
		ta.setCaretPosition(0);

	}
	
	/**
	 * Call to open an existing file, may be for edit or not
	 * @param file
	 * @throws IOException
	 */
	public EditorFrame(PacSatFile file, boolean edit) throws IOException {
		super("Message Viewer");
		psf = file;
		editable = edit;
		saveI = new JMenuItem("Save As"); //menuitems
		makeFrame(editable);
		
		pfh = psf.getPfh();
		filename = pfh.getFieldString(PacSatFileHeader.USER_FILE_NAME);
		txtTo.setText(pfh.getFieldString(PacSatFileHeader.DESTINATION).toUpperCase());
		txtFrom.setText(pfh.getFieldString(PacSatFileHeader.SOURCE).toUpperCase());
		txtTitle.setText(pfh.getFieldString(PacSatFileHeader.TITLE));
		txtKeywords.setText(pfh.getFieldString(PacSatFileHeader.KEYWORDS));
		lblCrDate.setText("Date: " + pfh.getDateString(PacSatFileHeader.CREATE_TIME) + " UTC");
		cbType.setSelectedIndex(PacSatFileHeader.getTypeIndexByString(pfh.getTypeString()));
		String ty = pfh.getTypeString();
		type = pfh.getType();
		int j = PacSatFileHeader.getTypeIndexByString(ty);
		cbType.setSelectedIndex(j);
		if (ty.equalsIgnoreCase("JPG")) {
			addImageArea();
			imagePanel.setBufferedImage(psf.getBytes());
		} else {
			addTextArea();
			///////////  DEBUG ta.append(pfh.toFullString());
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
		editPane.add(scpane);
	}
	
	private void addImageArea() {
		imagePanel = new ImagePanel();
		editPane.add(imagePanel);
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
//		viewM = new JMenu("View"); //edit menu

		exitI = new JMenuItem("Delete & Exit");
		cutI = new JMenuItem("Cut");
		copyI = new JMenuItem("Copy");
		pasteI = new JMenuItem("Paste");
		selectI = new JMenuItem("Select All"); //menuitems
		
		
		statusI = new JMenuItem("Status"); //menuitems
		toolBar = new JToolBar();

		setJMenuBar(menuBar);
		menuBar.add(fileM);
		menuBar.add(editM);
//		menuBar.add(viewM);

		fileM.add(saveI);
		fileM.add(exitI);

		editM.add(cutI);
		editM.add(copyI);
		editM.add(pasteI);        
		editM.add(selectI);

//		viewM.add(statusI);

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
		editPane = new JPanel();
		editPane.setLayout(new CardLayout());
		centerpane.add(editPane, BorderLayout.CENTER);

		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		centerpane.add(topPanel,BorderLayout.NORTH);

		// Button bar
		JPanel buttonBar = new JPanel();
		buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(buttonBar, BorderLayout.NORTH);
		
		butReply = new JButton("Reply");
		butReply.setMargin(new Insets(0,0,0,0));
		butReply.addActionListener(this);
		butReply.setToolTipText("Reply to this message");
		if (editable) butReply.setEnabled(false);
		buttonBar.add(butReply);

		butSave = new JButton("Save");
		butSave.setMargin(new Insets(0,0,0,0));
		butSave.addActionListener(this);
		butSave.setToolTipText("Save this message");
		if (editable) butSave.setEnabled(false);
		buttonBar.add(butSave);

		butDelete = new JButton("Del");
		butDelete.setMargin(new Insets(0,0,0,0));
		butDelete.addActionListener(this);
		butDelete.setToolTipText("Cancel and delete to this message");
		buttonBar.add(butDelete);

		butExit = new JButton("Exit");
		butExit.setMargin(new Insets(0,0,0,0));
		butExit.addActionListener(this);
		butExit.setToolTipText("Save and exit this message");
		buttonBar.add(butExit);

		// Pacsat File Header
		JPanel header = new JPanel();
		header.setLayout(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		topPanel.add(header, BorderLayout.SOUTH);
		
		JPanel headerFields = new JPanel();
		header.add(headerFields, BorderLayout.CENTER);
		JPanel header1 = new JPanel();
		JPanel header2 = new JPanel();
		JPanel header3 = new JPanel();
		JPanel header4 = new JPanel();

		headerFields.setLayout(new BoxLayout(headerFields, BoxLayout.Y_AXIS));
		headerFields.add(header1);
		headerFields.add(header2);
		headerFields.add(header3);
		headerFields.add(header4);
		header1.setLayout(new BoxLayout(header1, BoxLayout.X_AXIS)); // FlowLayout(FlowLayout.LEFT));
		header2.setLayout(new BoxLayout(header2, BoxLayout.X_AXIS)); //FlowLayout(FlowLayout.LEFT));
		header3.setLayout(new BoxLayout(header3, BoxLayout.X_AXIS)); //FlowLayout(FlowLayout.LEFT));
		header4.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		header1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		header2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		header3.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		header4.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		JLabel lblTo = new JLabel("To:    ");
		txtTo = new JTextField();
		txtTo.setColumns(20);
		txtTo.setEditable(edit);
		header1.add(lblTo);
		header1.add(new Box.Filler(new Dimension(10,10), new Dimension(23,20), new Dimension(23,20)));
		header1.add(txtTo);

		header1.add(new Box.Filler(new Dimension(10,10), new Dimension(200,20), new Dimension(1000,20)));

		if (editable)
			cbType = new JComboBox(PacSatFileHeader.userTypeStrings);			
		else
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

//		JPanel headerRight = new JPanel();
//		headerRight.setLayout(new BorderLayout());
//		header.add(headerRight, BorderLayout.EAST);
		lblCrDate = new JLabel();		
		header4.add(lblCrDate, BorderLayout.NORTH);
		
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
	
	/**
	 * Reply to the current message by spawning another editor
	 */
	private void reply() {
		// We want to respond to this person or persons, so grab the From list
		EditorFrame editor = null;
		Pattern pattern = Pattern.compile("(^.*)", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(ta.getText());
		String text = matcher.replaceAll("> ($1)");
		String origText = "\r\n\r\n" + "Previously on PacSat, " + txtFrom.getText() + " said:\r\n" + text;

		editor = new EditorFrame(txtFrom.getText(), txtFrom.getText(), txtTitle.getText(), txtKeywords.getText(), origText);
		editor.setVisible(true);
		dispose();
	}
	
	private void savePacsatFile() {
		// build the pacsatfile header then create file with header and text
		int bodySize = 0;
		short bodyChecksum = 0;
		String ext = ".txt";
		if (type == 0) { 
			String s = ta.getText();
			bodySize = s.length();
			bodyChecksum = PacSatFileHeader.checksum(s.getBytes());
		} else {
			bodySize = imageBytes.length;
			bodyChecksum = PacSatFileHeader.checksum(imageBytes);
			ext = ".jpg";
		}
		
		PacSatFileHeader pfh = new PacSatFileHeader(txtFrom.getText().toUpperCase(), txtTo.getText().toUpperCase(), bodySize, bodyChecksum, type, compressionType, txtTitle.getText(), txtKeywords.getText(), filename);
		byte[] bytes = null;
		if (type == 0) { // ASCII
			bytes = ta.getText().getBytes();
		} else { // assume image
			bytes = imageBytes;
		}

		psf = new PacSatFile(Config.spacecraft.directory.dirFolder + File.separator + filename + ".out", pfh, bytes);		
		psf.save();
		if (Config.mainWindow != null)
			Config.mainWindow.setOutboxData(Config.spacecraft.outbox.getTableData());
	}
	
	private File pickFile(String title, String buttonText, int type) {
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
			FileDialog fd = new FileDialog(this, title, type);
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
			fc.setApproveButtonText(buttonText);
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
		if (file != null)
			Config.set(MainWindow.WINDOW_CURRENT_DIR, file.getParent());	
		return file;
	}

	private void saveFile() {
		File file = null;
		file = pickFile("Save As", "Save", FileDialog.SAVE);
		
		if (file != null) {				
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
		//cbType.setEnabled(false);
		type = PacSatFileHeader.getTypeIdByString(ty);

		if (ty.equalsIgnoreCase("JPG") || ty.equalsIgnoreCase("GIF") || ty.equalsIgnoreCase("PNG")) {
			if (editable) {
				String ext = ".jpg";
				if (ty.equalsIgnoreCase("GIF")) ext = ".gif";
				if (ty.equalsIgnoreCase("PNG")) ext = ".png";
				
				if (filename == null)
					filename = Config.get(Config.CALLSIGN) + Config.spacecraft.getNextSequenceNum() + ext;
				File file = null;
				file = pickFile("Open Image", "Open", FileDialog.LOAD);
				if (file != null) {
					Config.set(MainWindow.WINDOW_CURRENT_DIR, file.getParent());					
					try {
						RandomAccessFile loadImage = new RandomAccessFile(file, "r");
						if (loadImage.length() > IMAGE_SIZE_LIMIT) {
							Log.errorDialog("ERROR - TOO LARGE", "You can't create a pacsat file with a "+loadImage.length()+ " byte image.\n"
									+ "Maximum image size is: " + IMAGE_SIZE_LIMIT);
							cbType.setSelectedIndex(0);
							return;
						}
						imageBytes = new byte[(int) loadImage.length()];
						for (int i = 0; i < loadImage.length(); i++)
							imageBytes[i] = loadImage.readByte();
						if (scpane != null) {
							centerpane.remove(scpane);
							scpane.setVisible(false);
						}
						//addImageArea();
						imagePanel.setVisible(true);
						imagePanel.setBufferedImage(imageBytes);
					} catch (FileNotFoundException e) {
						Log.errorDialog("ERROR", "Error with file name: " + file.getAbsolutePath() + "\n" + e.getMessage());
					} catch (IOException e) {
						Log.errorDialog("ERROR", "Error writing file: " + file.getAbsolutePath() + "\n" + e.getMessage());
					}
				}
			}
		} else if (ty.equalsIgnoreCase("ASCII")) {
			if (filename == null)
				filename = Config.get(Config.CALLSIGN) + Config.spacecraft.getNextSequenceNum() + ".txt";
			if (imagePanel != null)
				centerpane.remove(imagePanel);
			//scpane.setVisible(true);
			if (ta != null) {
				ta.setEditable(true);
				ta.setText("");  // zero out when ASCII selected
			}
		} else {
			// we don't know how to edit the type
		}
		
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == butSave) {
			if (editable) {
				savePacsatFile();
			} else
				saveFile();			
		} else
		// save and exit or save as
		if (e.getSource() == saveI || e.getSource() == butExit) {
			if (editable) {
				savePacsatFile();
				dispose();
			} else
				saveFile();
		}
		else if (e.getSource() == butReply)
			reply();
		else if (e.getSource() == butSave)
			saveFile();
		else if (e.getSource() == exitI || e.getSource() == butDelete)
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
			if (editable)
				processTypeSelection(PacSatFileHeader.userTypeStrings[i]);
			else
				processTypeSelection(PacSatFileHeader.typeStrings[i]);
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
			Config.set(EDIT_WINDOW_WIDTH, 650);
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