package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import com.g0kla.telem.data.LayoutLoadException;

import common.Config;
import common.Log;
import fileStore.MalformedPfhException;
import fileStore.PacSatField;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;
import fileStore.ZipFile;
import fileStore.telem.LogFileWE;

@SuppressWarnings("serial")
public class EditorFrame extends JFrame implements ActionListener, WindowListener {

	int type = 0;
	int compressionType = 0;
	
	public static final String TEXT_CARD = "text";
	public static final String IMAGE_CARD = "image";
	
	public static final String OUT = "out";
	public static final String ERR = "err";
	public static final String UL = "ul";
	public static final String DRAFT = "outtmp";
	
	private JTextArea ta;
	private JMenuBar menuBar;
	private JMenu fileM,editM;
	private JScrollPane scpane;
	private JMenuItem cancelI,cutI,copyI,pasteI,selectI,saveAndExitI,exportI, loadI,statusI;
	private String pad;
	private JToolBar toolBar;
	private String filename;
	private boolean buildingGui = true;

	JTextField txtTo, txtFrom, txtDate, txtTitle, txtKeywords;
	JButton butReply, butReplyInclude, butExport, butCancel, butSaveDraft, butSaveAndExit;
	JCheckBox cbZipped;
	JLabel lblCrDate;
	JComboBox<String> cbType;
	JPanel centerpane; // the main display area
	JSplitPane editPanes; // where the content is displayed
	JPanel textPane; // where the text of the document is dsiplayed
	ImagePanel imagePanel;
	byte[] imageBytes;
	
	private PacSatFile psf;
	private PacSatFileHeader pfh;
	private boolean editable = true;
	public static final boolean READ_ONLY = false;
	public static final boolean EDITABLE = true;
	public static final String RE = "Re: ";

	public static final String EDIT_WINDOW_X = "edit_window_x";
	public static final String EDIT_WINDOW_Y = "edit_window_y";
	public static final String EDIT_WINDOW_WIDTH = "edit_window_width";
	public static final String EDIT_WINDOW_HEIGHT = "edit_window_height";

	public static final int IMAGE_SIZE_LIMIT = 250000; // need some sort of sensible limit to prevent files that are too large being uploaded
	
	
	/**
	 * Call to create a new file
	 */
	public EditorFrame() {
		super("New Message");
		editable = true;
		makeFrame(editable);
		exportI.setEnabled(false);
		butExport.setEnabled(false);
		butSaveAndExit.setEnabled(false);
		butSaveDraft.setEnabled(false);
		saveAndExitI.setEnabled(false);
		butReply.setVisible(false);
		butReplyInclude.setVisible(false);
		loadI = new JMenuItem("Load"); //menuitems
//		fileM.add(loadI);
//		loadI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
//		loadI.addActionListener(this);
		txtFrom.setText(Config.get(Config.CALLSIGN));
		txtFrom.setEditable(false);
		addTextArea();
		addImageArea();		
		
		scpane.setVisible(true);
		ta.append(".... select document type to edit");
		ta.setVisible(true);
		ta.setEditable(false);
//		((CardLayout)editPane.getLayout()).show(editPane, TEXT_CARD);
		buildingGui = false;
		// Filename will be set by the type selection
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

		makeFrame(editable);

		butReply.setVisible(false);
		butReplyInclude.setVisible(false);
		
		txtFrom.setText(Config.get(Config.CALLSIGN));
		txtFrom.setEditable(false);
		
		txtTo.setText(toCallsign.toUpperCase());
		txtTitle.setText(title);
		txtKeywords.setText(keywords);
		if (filename == null)
			filename = Config.get(Config.CALLSIGN) + Config.spacecraftSettings.getNextSequenceNum() + ".txt";
///		lblCrDate.setText("Created: " + pfh.getDateString(PacSatFileHeader.CREATE_TIME) + " UTC");
		cbType.setSelectedIndex(PacSatFileHeader.getTypeIndexByString("ASCII"));
		addImageArea();
		addTextArea();
		ta.append(origText);
		ta.setCaretPosition(0);
//		((CardLayout)editPane.getLayout()).show(editPane, TEXT_CARD);
		buildingGui = false;
	}
	
	/**
	 * Call to open an existing file, may be for edit or not
	 * The file might be compressed, if so we use the expanded version.  If we are editing it we need to recompress when it is ready to send
	 * @param file
	 * @throws IOException
	 */
	public EditorFrame(PacSatFile file, boolean edit) throws IOException {
		super("Message Editor");
		psf = file;
		byte[] bytes = psf.getBytes();
		editable = edit;
		makeFrame(editable);
		if (!editable) { // not editing, just viewing
			saveAndExitI.setVisible(false);
			butSaveAndExit.setVisible(false);
			butSaveDraft.setVisible(false);
		} else {
			butReply.setVisible(false);
			butReplyInclude.setVisible(false);
		}
		
		pfh = psf.getPfh();
		filename = pfh.getFieldString(PacSatFileHeader.USER_FILE_NAME);
		txtTo.setText(pfh.getFieldString(PacSatFileHeader.DESTINATION).toUpperCase());
		txtFrom.setText(pfh.getFieldString(PacSatFileHeader.SOURCE).toUpperCase());
		txtTitle.setText(pfh.getFieldString(PacSatFileHeader.TITLE));
		txtKeywords.setText(pfh.getFieldString(PacSatFileHeader.KEYWORDS));
		lblCrDate.setText("Date: " + pfh.getDateString(PacSatFileHeader.CREATE_TIME) + " UTC");

		int compressedBy = 0;
		cbZipped.setSelected(false);
		PacSatField compressionType = pfh.getFieldById(PacSatFileHeader.COMPRESSION_TYPE);
		if (compressionType != null) {
			compressedBy = (int) pfh.getFieldById(PacSatFileHeader.COMPRESSION_TYPE).getLongValue();
			if (compressedBy > 0)
				cbZipped.setSelected(true);
		}

		type = pfh.getType();
		addImageArea();
		addTextArea();
		String ty = pfh.getTypeString();
		int j = 0;
		
		if (editable) {
			j = PacSatFileHeader.getUserTypeIndexByString(ty);
		} else {
			j = PacSatFileHeader.getTypeIndexByString(ty);
		}
		cbType.setSelectedIndex(j);
		if (ty.equalsIgnoreCase("JPG") || ty.equalsIgnoreCase("GIF") || ty.equalsIgnoreCase("PNG")) {
			try {
				imageBytes = bytes;
				imagePanel.setBufferedImage(imageBytes);
				editPanes.setDividerLocation(0); // reduce the text area
				cbZipped.setEnabled(false);
			} catch (Exception e) {
				Log.errorDialog("Can't Parse Image Data", "The image could not be loaded into the editor.");
			}
//			((CardLayout)editPane.getLayout()).show(editPane, IMAGE_CARD);
		} else if (ty.equalsIgnoreCase("WOD")) {
				LogFileWE we = null;
				try {
					we = new LogFileWE(psf.getData(bytes));
					ta.append(we.toString());
					ta.setCaretPosition(0);
//					((CardLayout)editPane.getLayout()).show(editPane, TEXT_CARD);
				} catch (MalformedPfhException e) {
					Log.errorDialog("ERROR", "Could not open log file:" + e.getMessage());
				} catch (LayoutLoadException e) {
					Log.errorDialog("ERROR", "Could not open log file:" + e.getMessage());
				}
				
		} else {
			///////////  DEBUG ta.append(pfh.toFullString());
			if (compressedBy == PacSatFileHeader.BODY_COMPRESSED_PKZIP) {
				// The file should already be decompressed on disk, so read in the text and use that instead
				// It was in a zip file though and we don't know the name of the files.  The files should be in a sub dir with the name of the
				// FileID.  We read in all files and display them in order. 
				//ta.append(pfh.toFullString());
				File destDir = psf.decompress();
				if (destDir != null) {
					// Now display the files that were decompressed
					File[] files = destDir.listFiles();
					int i = 0;
					for (File f : files) {
						//ta.append(f.getName() + ": \n");
						String fileParts[] = f.getName().split("\\.");
						String ext = fileParts[fileParts.length-1];
						if (ext.equalsIgnoreCase("JPG")) {
							byte[] content = null;
							try {
								content = Files.readAllBytes( Paths.get(f.getPath()) ) ;
							} catch (IOException e) {
								e.printStackTrace();
							}
							if (content != null) {
								imagePanel.setBufferedImage(content);
								editPanes.setDividerLocation(400); // reduce the text area size
							}
						} else {
							String content = "";
							try {
								content = new String ( Files.readAllBytes( Paths.get(f.getPath()) ) );
							} catch (IOException e) {
								e.printStackTrace();
							}						
							ta.append(content);
						}
						ta.append("\n\n");
						f.delete();
					}
					destDir.delete();
				} else {
					ta.append("Compressed Archive appears to be empty, or there was an error extracting the data..");
				}
				
			} else {
				ta.append(new String(psf.getBytes()));
			}
			ta.setCaretPosition(0);
//			((CardLayout)editPane.getLayout()).show(editPane, TEXT_CARD);
		}
		if (editable) {
			short bodyChecksum_in_pfh = (short) pfh.getFieldById(PacSatFileHeader.BODY_CHECKSUM).getLongValue();
			// Check the checksums
			int bodySize = 0;
			short bodyChecksum = 0;
			bodySize = bytes.length;
			bodyChecksum = PacSatFileHeader.checksum(bytes);
//			String ext = ".txt";
//			if (type == 0) { 
//				String s = ta.getText();
//			} else {
//				bodySize = imageBytes.length;
//				bodyChecksum = PacSatFileHeader.checksum(imageBytes);
//				ext = ".jpg";
//			}
			if (bodyChecksum_in_pfh != bodyChecksum)
				Log.errorDialog("Error in body checksum", "In header: " + bodyChecksum_in_pfh + " actual: " + bodyChecksum + "\n"
						+ "You need to edit and resave this file to recalculate the checksum.");
		}
		buildingGui = false;
	}
	
	private void addTextArea() {
		ta = new JTextArea(); //textarea
		scpane = new JScrollPane(ta); //scrollpane  and add textarea to scrollpane
		ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setEditable(editable);
		ta.setVisible(true);
		ta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, lblCrDate.getFont().getSize()));  // default to same as the system size
//		editPane.add(scpane, TEXT_CARD);
		textPane.add(scpane, BorderLayout.CENTER);
	}
	
	private void addImageArea() {

	}
	
	private void makeFrame(boolean edit) {
		
		addWindowListener(this);
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/pacsat.jpg")));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());

		pad = " ";

		menuBar = new JMenuBar(); //menubar
		fileM = new JMenu("File"); //file menu
		editM = new JMenu("Edit"); //edit menu
//		viewM = new JMenu("View"); //edit menu

		cancelI = new JMenuItem("Exit");
		cutI = new JMenuItem("Cut");
		copyI = new JMenuItem("Copy");
		pasteI = new JMenuItem("Paste");
		selectI = new JMenuItem("Select All"); //menuitems
		exportI = new JMenuItem("Export"); //menuitems
		saveAndExitI = new JMenuItem("Send"); //menuitems
		
		
		statusI = new JMenuItem("Status"); //menuitems
		toolBar = new JToolBar();

		setJMenuBar(menuBar);
		menuBar.add(fileM);
		menuBar.add(editM);
//		menuBar.add(viewM);

		fileM.add(exportI);
		fileM.add(saveAndExitI);
		fileM.add(cancelI);

		editM.add(cutI);
		editM.add(copyI);
		editM.add(pasteI);        
		editM.add(selectI);

//		viewM.add(statusI);

		saveAndExitI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
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
		editPanes = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		//editPane.setLayout(new CardLayout());
		//editPane.setLayout(new BorderLayout());
		centerpane.add(editPanes, BorderLayout.CENTER);

		textPane = new JPanel();
		textPane.setLayout(new BorderLayout());
//		editPane.add(textPane, BorderLayout.CENTER);
		editPanes.setTopComponent(textPane);
		
		imagePanel = new ImagePanel();
		editPanes.setBottomComponent(imagePanel);
		editPanes.setDividerLocation(2000); // maximize the text area
		
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
		
		butReplyInclude = new JButton("Reply Inc");
		butReplyInclude.setMargin(new Insets(0,0,0,0));
		butReplyInclude.addActionListener(this);
		butReplyInclude.setToolTipText("Reply to this message and include the original text");
		if (editable) butReplyInclude.setEnabled(false);
		buttonBar.add(butReplyInclude);

		butExport = new JButton("Export");
		butExport.setMargin(new Insets(0,0,0,0));
		butExport.addActionListener(this);
		butExport.setToolTipText("Save this message to a file");
		buttonBar.add(butExport);

		butSaveDraft = new JButton("Save Draft");
		butSaveDraft.setMargin(new Insets(0,0,0,0));
		butSaveDraft.addActionListener(this);
		butSaveDraft.setToolTipText("Save draft and continue editing this message");
		buttonBar.add(butSaveDraft);

		butSaveAndExit = new JButton("Send");
		butSaveAndExit.setMargin(new Insets(0,0,0,0));
		butSaveAndExit.addActionListener(this);
		butSaveAndExit.setToolTipText("Save and send this message");
		buttonBar.add(butSaveAndExit);

		butCancel = new JButton("Exit");
		butCancel.setMargin(new Insets(0,0,0,0));
		butCancel.addActionListener(this);
		butCancel.setToolTipText("Cancel and exit this message");
		buttonBar.add(butCancel);


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

		cbZipped = new JCheckBox("Compressed  ");
		header1.add(cbZipped);
		cbZipped.setEnabled(edit);
		cbZipped.addActionListener(this);
		
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

		exportI.addActionListener(this);
		saveAndExitI.addActionListener(this);
		cancelI.addActionListener(this);
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
	private void reply(boolean include) {
		// We want to respond to this person or persons, so grab the From list
		EditorFrame editor = null;
		String origText = "";
		if (include) {
			Pattern pattern = Pattern.compile("(^.*)", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(ta.getText());
			String text = matcher.replaceAll("> $1");
			origText = "\r\n\r\n" + "Previously on PacSat, " + txtFrom.getText() + " said:\r\n" + text;
		} 
		String title = txtTitle.getText();
		if (!title.startsWith(RE)) {
			title = RE + title;
		}
		editor = new EditorFrame(txtFrom.getText(), txtFrom.getText(), title, txtKeywords.getText(), origText);
		editor.setVisible(true);
		editor.setBounds(this.getX()+30, this.getY()+30, this.getWidth(), this.getHeight());
		if (include)
			dispose();
	}
	
	private void savePacsatFile(int state) {
		// First get the bytes
		byte[] bytes = null;
		String ext = ".txt";
		if (type == 0) { // ASCII
			String txt = ta.getText();
			String newTxt = "";
			char prevChar = 0;
			// but we must use \r\n rather than just \n
			for (int i=0; i<txt.length(); i++) {
				char ch = txt.charAt(i);
				if (ch == 0x0A && prevChar != 0x0D)
					newTxt = newTxt + "\r\n";
				else
					newTxt = newTxt + ch;
				prevChar = ch;
			}
			bytes = newTxt.getBytes();
		} else { // assume image
			bytes = imageBytes;
			String ty = pfh.getTypeString();
			ext = ".jpg";
			if (ty.equalsIgnoreCase("GIF")) ext = ".gif";
			if (ty.equalsIgnoreCase("PNG")) ext = ".png";
		}

		if (cbZipped.isSelected()) {
			// Then compress the bytes
			try {
				filename = filename.replaceAll("\\..*$", ext); // place the extension (in case it is already .zip because we are editing an existing file
				bytes = ZipFile.zipBytes(filename, bytes);
				compressionType = PacSatFileHeader.BODY_COMPRESSED_PKZIP;
				filename = filename.replaceAll("\\..*$", ".zip");
				//Log.println("Setting filename to: " + filename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.errorDialog("ERROR", "Could not compress the file\n" + e);
			}
		}
		
		// build the pacsatfile header then create file with header and text
		int bodySize = 0;
		short bodyChecksum = 0;
		bodySize = bytes.length;
		bodyChecksum = PacSatFileHeader.checksum(bytes);
		
		PacSatFileHeader pfh = new PacSatFileHeader(txtFrom.getText().toUpperCase(), txtTo.getText().toUpperCase(), bodySize, bodyChecksum, type, compressionType, txtTitle.getText(), txtKeywords.getText(), filename);
		pfh.setState(state);

		String state_ext = OUT;
		if (state == PacSatFileHeader.DRAFT) {
			state_ext = DRAFT;
			File que = new File(Config.spacecraftSettings.directory.dirFolder + File.separator + filename + "." + OUT);
			if (que.exists())
				que.delete();
		} else {
			File draft = new File(Config.spacecraftSettings.directory.dirFolder + File.separator + filename + "." + DRAFT);
			if (draft.exists())
				draft.delete();
		}
			
		psf = new PacSatFile(Config.spacecraftSettings.directory.dirFolder + File.separator + filename + "." + state_ext, pfh, bytes);		
		psf.save();
		if (Config.mainWindow != null)
			Config.mainWindow.setOutboxData(Config.spacecraftSettings.outbox.getTableData());
	}
	
	private File pickFile(String title, String buttonText, int type, String defaultName) {
		File file = null;
		File dir = null;
		String d = Config.get(MainWindow.EDITOR_CURRENT_DIR);
		if (d == null)
			dir = new File(".");
		else
			if (d != "") {
				dir = new File(Config.get(MainWindow.EDITOR_CURRENT_DIR));
			}
		
		if(Config.getBoolean(Config.USE_NATIVE_FILE_CHOOSER)) {
			FileDialog fd = new FileDialog(this, title, type);
			// use the native file dialog on the mac
			if (dir != null) {
				fd.setDirectory(dir.getAbsolutePath());
			}
			if (defaultName != null)
				fd.setFile(defaultName);;
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
			if (defaultName != null)
				fc.setSelectedFile(new File(defaultName));
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
			Config.set(MainWindow.EDITOR_CURRENT_DIR, file.getParent());	
		return file;
	}

	/** 
	 * Save the file.  Note that this does not decompress or interpret.  Just a save of the Bytes.
	 * @throws IOException 
	 */
	private void saveFile() throws IOException {
		if (psf == null) {
			this.savePacsatFile(PacSatFileHeader.DRAFT);
		}
			
		File defaultFile = psf.extractUserFile();
		File file = null;
		file = pickFile("Save As", "Save", FileDialog.SAVE, defaultFile.getName());
		
		if (file != null) {
			RandomAccessFile saveFile = null;
			try {
				saveFile = new RandomAccessFile(file, "rw");
				saveFile.write(psf.getBytes());
			} catch (FileNotFoundException e) {
				Log.errorDialog("ERROR", "Error with file name: " + file.getAbsolutePath() + "\n" + e.getMessage());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Error writing file: " + file.getAbsolutePath() + "\n" + e.getMessage());
			} finally {
				if (saveFile != null) saveFile.close();
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
					filename = Config.get(Config.CALLSIGN) + Config.spacecraftSettings.getNextSequenceNum() + ext;
				File file = null;
				file = pickFile("Open Image", "Open", FileDialog.LOAD, null);
				if (file != null) {
					Config.set(MainWindow.EDITOR_CURRENT_DIR, file.getParent());					
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
//						((CardLayout)editPane.getLayout()).show(editPane, IMAGE_CARD);

						imagePanel.setBufferedImage(imageBytes);
						this.editPanes.setDividerLocation(0); // hide the text pane now
						exportI.setEnabled(true);
						butExport.setEnabled(true);
						butSaveAndExit.setEnabled(true);
						butSaveDraft.setEnabled(true);
						saveAndExitI.setEnabled(true);
						// Zipping images does not work, so for now don't allow it
						cbZipped.setSelected(false);
						cbZipped.setEnabled(false);
					} catch (FileNotFoundException e) {
						Log.errorDialog("ERROR", "Error with file name: " + file.getAbsolutePath() + "\n" + e.getMessage());
					} catch (IOException e) {
						Log.errorDialog("ERROR", "Error writing file: " + file.getAbsolutePath() + "\n" + e.getMessage());
					}
				}
			}
		} else if (ty.equalsIgnoreCase("ASCII")) {
			if (filename == null)
				filename = Config.get(Config.CALLSIGN) + Config.spacecraftSettings.getNextSequenceNum() + ".txt";
			//((CardLayout)editPane.getLayout()).show(editPane, TEXT_CARD);
			if (ta != null) {
				ta.setEditable(true);
				ta.setText("");  // zero out when ASCII selected
				butSaveAndExit.setEnabled(true);
				butSaveDraft.setEnabled(true);
				saveAndExitI.setEnabled(true);
				exportI.setEnabled(true);
				butExport.setEnabled(true);
				cbZipped.setEnabled(true);
				this.editPanes.setDividerLocation(2000); // hide the image pane now
			}
		} else {
			// we don't know how to edit the type
		}
		
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == butExport || e.getSource() == exportI) {
			try {
				saveFile();
			} catch (IOException e1) {
				Log.errorDialog("ERROR", "Could not extract the name of the user file");
			}			
		} else
		// save and exit
		if (e.getSource() == saveAndExitI || e.getSource() == butSaveAndExit) {
			if (editable) {
				if (this.txtTo.getText().equalsIgnoreCase("")) {
					Log.infoDialog("TO is blank", "The message needs to be sent to at least one other station.\nPut something in the TO field.");
					return;
				}
				if (this.txtTitle.getText().equalsIgnoreCase("")) {
					Log.infoDialog("TITLE is blank", "The message needs to have a title so that stations can decide if \n"
							+ "it should be downloaded.  Put something in the TITLE field.");
					return;
				}
				savePacsatFile(PacSatFileHeader.QUE);
				dispose();
			} else
				Log.errorDialog("ERROR", "Can't resave the file when browsing it");
		} else if (/*e.getSource() == saveDraftI || */ e.getSource() == butSaveDraft) {
			if (editable) {
				savePacsatFile(PacSatFileHeader.DRAFT); // note that the attribute is not passed through yet.  Need to save the outbox as a directory
				//dispose();
			} else
				Log.errorDialog("ERROR", "Can't save the file when browsing it");
		}
		else if (e.getSource() == butReply)
			reply(false);
		else if (e.getSource() == butReplyInclude)
			reply(true);
		else if (e.getSource() == cancelI || e.getSource() == butCancel)
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
			if (buildingGui) return;
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