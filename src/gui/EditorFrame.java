package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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
	
	private PacSatFile psf;
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
	
		JPanel header = new JPanel();
	
		PacSatFileHeader pfh = psf.getPfh();
		JLabel lblType = new JLabel("Type: " + pfh.getTypeString());
		header.add(lblType);
		
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