package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.*;

import common.Config;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

@SuppressWarnings("serial")
public class EditorFrame extends JFrame implements ActionListener, WindowListener
{
	private JTextArea ta;
	private int count;
	private JMenuBar menuBar;
	private JMenu fileM,editM,viewM;
	private JScrollPane scpane;
	private JMenuItem exitI,cutI,copyI,pasteI,selectI,saveI,loadI,statusI;
	private String pad;
	private JToolBar toolBar;
	
	JTextField txtTo, txtFrom, txtDate, txtTitle;
	
	private PacSatFile psf;
	private PacSatFileHeader pfh;
	private boolean editable = true;
	public static final boolean READ_ONLY = false;
	public static final boolean EDITABLE = true;
	
	public static final String EDIT_WINDOW_X = "edit_window_x";
	public static final String EDIT_WINDOW_Y = "edit_window_y";
	public static final String EDIT_WINDOW_WIDTH = "edit_window_width";
	public static final String EDIT_WINDOW_HEIGHT = "edit_window_height";
	
	public EditorFrame(PacSatFile file, boolean edit) throws IOException {
		super("Editor");
		psf = file;
		editable = edit;
		if (edit)
			pfh = null; // we create it from the fields that the user fills in
		else
			pfh = psf.getPfh();

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
		saveI = new JMenuItem("Save"); //menuitems
		loadI = new JMenuItem("Load"); //menuitems
		statusI = new JMenuItem("Status"); //menuitems
		toolBar = new JToolBar();

		setJMenuBar(menuBar);
		menuBar.add(fileM);
		menuBar.add(editM);
		menuBar.add(viewM);

		fileM.add(saveI);
		fileM.add(loadI);
		fileM.add(exitI);

		editM.add(cutI);
		editM.add(copyI);
		editM.add(pasteI);        
		editM.add(selectI);

		viewM.add(statusI);

		saveI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		loadI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
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

		JPanel centerpane = new JPanel();
		pane.add(centerpane,BorderLayout.CENTER);
		centerpane.setLayout(new BorderLayout());

		
		// Pacsat File Header
		JPanel header = new JPanel();
		JPanel header1 = new JPanel();
		JPanel header2 = new JPanel();
		JPanel header3 = new JPanel();
	
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.add(header1);
		header.add(header2);
		header.add(header3);
		header1.setLayout(new FlowLayout(FlowLayout.LEFT));
		header2.setLayout(new FlowLayout(FlowLayout.LEFT));
		header3.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		
		JLabel lblTo = new JLabel("To:    ");
		txtTo = new JTextField(pfh.getFieldString(PacSatFileHeader.DESTINATION));
		txtTo.setColumns(20);
		txtTo.setEditable(edit);
		header1.add(lblTo);
		header1.add(new Box.Filler(new Dimension(10,10), new Dimension(20,20), new Dimension(30,20)));
		header1.add(txtTo);

		header1.add(new Box.Filler(new Dimension(10,10), new Dimension(20,20), new Dimension(20,20)));
		
		JLabel lblType = new JLabel("Type:   " + pfh.getTypeString());
		header1.add(lblType);
		
		JLabel lblFrom = new JLabel("From: ");
		txtFrom = new JTextField(pfh.getFieldString(PacSatFileHeader.SOURCE));			
		txtFrom.setColumns(20);
		txtFrom.setEditable(edit);
		header2.add(lblFrom);
		header2.add(new Box.Filler(new Dimension(10,10), new Dimension(20,20), new Dimension(30,20)));
		header2.add(txtFrom);

		JLabel lblTitle = new JLabel("");
		txtTitle = new JTextField(pfh.getFieldString(PacSatFileHeader.TITLE));			
		txtTitle.setColumns(50);
		txtTitle.setEditable(edit);
		header3.add(lblTitle);
//		header2.add(new Box.Filler(new Dimension(10,10), new Dimension(20,20), new Dimension(30,20)));
		header3.add(txtTitle);

		
		header1.add(new Box.Filler(new Dimension(10,10), new Dimension(20,20), new Dimension(200,20)));
		JLabel lblDate = new JLabel("Created: " + pfh.getDateString(PacSatFileHeader.CREATE_TIME) + " UTC");
		header1.add(lblDate);

		
		
		centerpane.add(header,BorderLayout.NORTH);
		
		if (pfh.getTypeString().equalsIgnoreCase("JPG")) {
			ImagePanel image = new ImagePanel();
			image.setBufferedImage(psf.getImageBytes());
			centerpane.add(image,BorderLayout.CENTER);
		} else {
			ta = new JTextArea(); //textarea
			scpane = new JScrollPane(ta); //scrollpane  and add textarea to scrollpane
			ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			ta.setLineWrap(true);
			ta.setWrapStyleWord(true);
			ta.setEditable(editable);
////// DEBUG			ta.append(pfh.toFullString());
			ta.append(psf.getText());
			ta.setCaretPosition(0);
			centerpane.add(scpane,BorderLayout.CENTER);
		}
		
		pane.add(toolBar,BorderLayout.SOUTH);

		saveI.addActionListener(this);
		loadI.addActionListener(this);
		exitI.addActionListener(this);
		cutI.addActionListener(this);
		copyI.addActionListener(this);
		pasteI.addActionListener(this);
		selectI.addActionListener(this);
		statusI.addActionListener(this);
		
	
		setVisible(true);
	}
	public void actionPerformed(ActionEvent e) 
	{
		JMenuItem choice = (JMenuItem) e.getSource();
		if (choice == saveI) {
			//not yet implmented
		}
		else if (choice == exitI)
			dispose();
		else if (choice == cutI) {
			pad = ta.getSelectedText();
			ta.replaceRange("", ta.getSelectionStart(), ta.getSelectionEnd());
		}
		else if (choice == copyI)
			pad = ta.getSelectedText();
		else if (choice == pasteI)
			ta.insert(pad, ta.getCaretPosition());
		else if (choice == selectI)
			ta.selectAll();
		else if (e.getSource() == statusI) {
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