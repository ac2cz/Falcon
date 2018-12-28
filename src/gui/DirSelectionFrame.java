package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import common.Config;
import common.Log;
import common.Spacecraft;
import fileStore.DirSelectionCriteria;
import fileStore.DirSelectionCriteriaList;

public class DirSelectionFrame extends JDialog implements ActionListener, ItemListener, WindowListener {
	
	public static final String DIRSELECTION_WINDOW_X = "DIRSELECTION_window_x";
	public static final String DIRSELECTION_WINDOW_Y = "DIRSELECTION_window_y";
	public static final String DIRSELECTION_WINDOW_WIDTH = "DIRSELECTION_window_width";
	public static final String DIRSELECTION_WINDOW_HEIGHT = "DIRSELECTION_window_height";
	
	private final JPanel contentPanel = new JPanel();
	Spacecraft sat;
	
	JButton btnCancel, btnSave;
	
	public static final int NUM_OF_ROWS = 4;
	
	JPanel[] row = new JPanel[NUM_OF_ROWS];
	private JComboBox[] cbField = new JComboBox[NUM_OF_ROWS];
	private JComboBox[] cbOp  = new JComboBox[NUM_OF_ROWS];
	private JTextField[] txtValue  = new JTextField[NUM_OF_ROWS];
	int opType[] = new int[NUM_OF_ROWS];
	
	/**
	 * Create the dialog.
	 */
	public DirSelectionFrame(Spacecraft sat, JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Spacecraft paramaters");
		addWindowListener(this);
		this.sat = sat;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		contentPanel.setLayout(new BorderLayout(0, 0));
		addFields();
		addButtons();
	}
	
	private void addFields() {	
		JPanel selectionRows = new JPanel();
		selectionRows.setLayout(new BoxLayout(selectionRows, BoxLayout.Y_AXIS));
		getContentPane().add(selectionRows, BorderLayout.CENTER);
		for (int i=0; i<2; i++) {
			addSelectionRow(selectionRows,i);
		}
	}
	
	private JPanel addSelectionRow(JPanel parent, int rowNum) {
		row[rowNum] = new JPanel();
		row[rowNum].setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel lbl = new JLabel("Select: ");
		cbField[rowNum] = new JComboBox(DirSelectionCriteria.FIELDS);
		cbOp[rowNum] = new JComboBox(DirSelectionCriteria.STRING_OPS);
		txtValue[rowNum] = new JTextField();
		txtValue[rowNum].setColumns(10);
		cbField[rowNum].addItemListener(this);
		cbOp[rowNum].addItemListener(this);
		cbField[rowNum].setToolTipText("PacSat File Header field");
		lbl.setToolTipText("Choose selection criteria to automatically download directory entries");
		row[rowNum].add(lbl);
		row[rowNum].add(cbField[rowNum]);
		row[rowNum].add(cbOp[rowNum]);
		row[rowNum].add(txtValue[rowNum]);
		parent.add(row[rowNum]);
		return row[rowNum];
	}

	private void addButtons() {
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		btnSave = new JButton("Save");
		btnSave.setActionCommand("Save");
		buttonPane.add(btnSave);
		btnSave.addActionListener(this);
		getRootPane().setDefaultButton(btnSave);


		btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("Cancel");
		buttonPane.add(btnCancel);
		btnCancel.addActionListener(this);

	}
	public void saveProperties() {
		Config.set(DIRSELECTION_WINDOW_HEIGHT, this.getHeight());
		Config.set(DIRSELECTION_WINDOW_WIDTH, this.getWidth());
		Config.set(DIRSELECTION_WINDOW_X, this.getX());
		Config.set(DIRSELECTION_WINDOW_Y, this.getY());
		
		Config.save();
	}
	
	public void loadProperties() {
		if (Config.getInt(DIRSELECTION_WINDOW_X) == 0) {
			Config.set(DIRSELECTION_WINDOW_X, 100);
			Config.set(DIRSELECTION_WINDOW_Y, 100);
			Config.set(DIRSELECTION_WINDOW_HEIGHT, 400);
			Config.set(DIRSELECTION_WINDOW_WIDTH, 600);
		}
		setBounds(Config.getInt(DIRSELECTION_WINDOW_X), Config.getInt(DIRSELECTION_WINDOW_Y), 
				Config.getInt(DIRSELECTION_WINDOW_WIDTH), Config.getInt(DIRSELECTION_WINDOW_HEIGHT));
	}
	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		saveProperties();
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			this.dispose();
		}

		if (e.getSource() == btnSave) {
			DirSelectionCriteriaList list = new DirSelectionCriteriaList(Config.spacecraft.name);
			for (int i=0; i < NUM_OF_ROWS; i++) {
				if (cbField[i] != null) {
					
					DirSelectionCriteria select = new DirSelectionCriteria((String)cbField[i].getSelectedItem(), 
							opType[i], cbOp[i].getSelectedIndex(), txtValue[i].getText());
					System.err.println(select);
					list.add(select);
				}
			}
			try {
				list.save();
			} catch (IOException e1) {
				Log.errorDialog("ERROR", "Could not save the selection criteria: " + e1.getMessage());
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		for (int i=0; i < NUM_OF_ROWS; i++)
		if (cbField[i] != null && e.getSource() == cbField[i]) {
			opType[i] = DirSelectionCriteria.getOpTypeByField((String)cbField[i].getSelectedItem());
			//System.err.println((String)cbField.getSelectedItem() + " " + opType);
			String[] labels;
			if (opType[i] == DirSelectionCriteria.NUM_OP)
				labels = DirSelectionCriteria.NUMERIC_OPS;
			else
				labels = DirSelectionCriteria.STRING_OPS;
			cbOp[i].removeAllItems();
			cbOp[i].setModel(new DefaultComboBoxModel(labels));
		}
		
	}

}
