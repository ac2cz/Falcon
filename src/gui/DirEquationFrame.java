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
import fileStore.DirSelectionEquation;

public class DirEquationFrame extends JDialog implements ActionListener, ItemListener, WindowListener {
	
	public static final String DIRSELECTION_WINDOW_X = "DIRSELECTION_window_x";
	public static final String DIRSELECTION_WINDOW_Y = "DIRSELECTION_window_y";
	public static final String DIRSELECTION_WINDOW_WIDTH = "DIRSELECTION_window_width";
	public static final String DIRSELECTION_WINDOW_HEIGHT = "DIRSELECTION_window_height";
	
	private final JPanel contentPanel = new JPanel();
	Spacecraft sat;
	
	JButton btnCancel, btnSave, btnAnd;
	
	public static final int MAX_ROWS = 4;
	private int numOfRows = 3;
	
	JPanel selectionRows;
	JPanel[] row = new JPanel[numOfRows];
	private JComboBox[] cbField = new JComboBox[numOfRows];
	private JComboBox[] cbOp  = new JComboBox[numOfRows];
	private JTextField[] txtValue  = new JTextField[numOfRows];
	int opType[] = new int[numOfRows];
	SpacecraftFrame caller;
	DirSelectionEquation equation;
	
	/**
	 * Create the dialog.
	 */
	public DirEquationFrame(Spacecraft sat, JFrame owner, boolean modal, SpacecraftFrame caller) {
		super(owner, modal);
		this.caller = caller;
		makeDialog();
		addFields(null);
	}
	
	public DirEquationFrame(Spacecraft sat, JFrame owner, boolean modal, SpacecraftFrame caller, DirSelectionEquation equation) {
		super(owner, modal);
		this.caller = caller;
		this.equation = equation;
		makeDialog();
		addFields(equation);
	}
	
	private void makeDialog() {
		setTitle("Directory Selection Equation");
		addWindowListener(this);
		this.sat = sat;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		contentPanel.setLayout(new BorderLayout(0, 0));
		addButtons();
		
	}
	
	private void addFields(DirSelectionEquation equation) {	
		selectionRows = new JPanel();
		selectionRows.setLayout(new BoxLayout(selectionRows, BoxLayout.Y_AXIS));
		getContentPane().add(selectionRows, BorderLayout.CENTER);
		JPanel north = new JPanel();
		btnAnd = new JButton("Add");
		btnAnd.addActionListener(this);
		getContentPane().add(north, BorderLayout.NORTH);
	//	north.add(btnAnd);
		generateRows(equation);
	}
	
	private void generateRows(DirSelectionEquation equation) {
		selectionRows.removeAll();
		row = new JPanel[numOfRows];
		cbField = new JComboBox[numOfRows];
		cbOp  = new JComboBox[numOfRows];
		txtValue  = new JTextField[numOfRows];
		opType = new int[numOfRows];
		JLabel[] lblAnd = new JLabel[numOfRows -1];
		JPanel[] panAnd = new JPanel[numOfRows -1];
		if (numOfRows > MAX_ROWS) numOfRows = MAX_ROWS;
		for (int i=0; i<numOfRows; i++) {
			addSelectionRow(selectionRows,i, equation);
			if (i < numOfRows - 1) {
				lblAnd[i] = new JLabel("AND");
				panAnd[i] = new JPanel();
				selectionRows.add(panAnd[i]);
				panAnd[i].setLayout(new FlowLayout(FlowLayout.LEFT));
				panAnd[i].add(lblAnd[i]);
			}
		}
		this.repaint();
	}
	
	private JPanel addSelectionRow(JPanel parent, int rowNum, DirSelectionEquation equation) {
		row[rowNum] = new JPanel();
		cbOp[rowNum] = new JComboBox(DirSelectionCriteria.STRING_OPS);
		if (equation != null) opType[rowNum] = equation.getOpType(rowNum);
		row[rowNum].setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel lbl = new JLabel("Select: ");
		cbField[rowNum] = new JComboBox(DirSelectionCriteria.FIELDS);
// set field
		if (equation != null) cbField[rowNum].setSelectedItem(equation.getField(rowNum));
// set op and opType
		populateOperation(rowNum);
		if (equation != null) cbOp[rowNum].setSelectedIndex(equation.getOperation(rowNum));
		txtValue[rowNum] = new JTextField();
		if (equation != null) txtValue[rowNum].setText(equation.getValue(rowNum));
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

	private void populateOperation(int i) {
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
	
	private void addButtons() {
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		JLabel footer = new JLabel("Clear the text to remove a criteria");
		footer.setBorder(new EmptyBorder(5, 5, 5, 50));
		buttonPane.add(footer);
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
			Config.set(DIRSELECTION_WINDOW_HEIGHT, 256);
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
		if (e.getSource() == btnAnd) {
			numOfRows++;
			if (numOfRows > MAX_ROWS) 
				numOfRows = MAX_ROWS;
			else
				generateRows(null);
		}
		if (e.getSource() == btnSave) {
			boolean added = false;
			DirSelectionEquation equation = new DirSelectionEquation(Config.spacecraft.name);
			for (int i=0; i < numOfRows; i++) {
				if (cbField[i] != null) {
					if (!txtValue[i].getText().equalsIgnoreCase("")) {
						try {
						if (opType[i] == DirSelectionCriteria.NUM_OP) {
							int valueTest = Integer.parseInt(txtValue[i].getText());
						}
						DirSelectionCriteria select = new DirSelectionCriteria(((String)cbField[i].getSelectedItem()).toUpperCase(), 
								opType[i], cbOp[i].getSelectedIndex(), txtValue[i].getText().toUpperCase());
						if (i > 0) Log.print(" AND ");
						Log.print("Select: "+select);
						equation.add(select);
						added = true;
						} catch (NumberFormatException n1) {
							Log.errorDialog("ERROR", "" +(String)cbField[i].getSelectedItem() +": must be numeric, criteria ignored");
						}
					}
				}
			}
			if (added)
			try {
				Log.println("");
				if (this.equation != null)
					Config.spacecraft.directory.deleteEquation(this.equation.getHashKey());	 // delete if it already exists			
				Config.spacecraft.directory.add(equation);
				caller.updateDirEquations();
			} catch (IOException e1) {
				Log.errorDialog("ERROR", "Could not save the Directory Selection Equation");
			}
			this.dispose();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		for (int i=0; i < numOfRows; i++)
		if (cbField[i] != null && e.getSource() == cbField[i]) {
			populateOperation(i);
		}
		
	}

}
