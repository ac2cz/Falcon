package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import common.Config;
import common.Log;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

public abstract class TablePanel extends JScrollPane implements MouseListener {

	private static final long serialVersionUID = 1L;
	FileHeaderTableModel fileHeaderTableModel;
	JTable directoryTable;
	
	TablePanel() {	
		fileHeaderTableModel = new FileHeaderTableModel();
		directoryTable = new JTable(fileHeaderTableModel);
		directoryTable.setAutoCreateRowSorter(true);
		this.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
		this.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
		this.getViewport().add(directoryTable);
		directoryTable.setFillsViewportHeight(true);
		directoryTable.setAutoResizeMode(JTable. AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		
		int[] columnWidths = FileHeaderTableModel.columnWidths;

		for (int i=0; i< directoryTable.getColumnModel().getColumnCount(); i++) {
			TableColumn column = directoryTable.getColumnModel().getColumn(i);
			column.setPreferredWidth(columnWidths[i]);
			column.setCellRenderer(new DirTableCellRenderer());
		}
		
		// Hide the old/new dates
		if (!Config.getBoolean("SHOW_DIR_TIMES")) {
			TableColumnModel tcm = directoryTable.getColumnModel();
			tcm.removeColumn( tcm.getColumn(5) );
			tcm.removeColumn( tcm.getColumn(6) ); // its not 7 because we already removed a column
		}

		directoryTable.addMouseListener(this);
		String PREV = "prev";
		String NEXT = "next";
		String ENTER = "enter";
		String ZERO = "zero";
		String ONE = "one";
		String TWO = "two";
		String THREE = "three";
		String FOUR = "four";
		InputMap inMap = directoryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		inMap.put(KeyStroke.getKeyStroke("ENTER"), ENTER);
		inMap.put(KeyStroke.getKeyStroke("0"), ZERO);
		inMap.put(KeyStroke.getKeyStroke("1"), ONE);
		inMap.put(KeyStroke.getKeyStroke("2"), TWO);
		inMap.put(KeyStroke.getKeyStroke("3"), THREE);
		inMap.put(KeyStroke.getKeyStroke("4"), FOUR);
		ActionMap actMap = directoryTable.getActionMap();

		actMap.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = directoryTable.getSelectedRow();
				if (row > 0) {
					directoryTable.setRowSelectionInterval(row-1, row-1);
					directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(row-1, 0, true)));
				}
			}
		});
		actMap.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = directoryTable.getSelectedRow();
				if (row < directoryTable.getRowCount()-1) {
					directoryTable.setRowSelectionInterval(row+1, row+1);
					directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(row+1, 0, true)));
				}
			}
		});
		actMap.put(ENTER, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("ENTER");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					displayRow(directoryTable,row);        
			}
		});
		actMap.put(ZERO, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("NONE");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 0);        
			}
		});
		actMap.put(ONE, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("ONE");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 1);        
			}
		});
		actMap.put(TWO, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
		//		System.out.println("TWO");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 2);        
			}
		});
		actMap.put(THREE, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
		//		System.out.println("THREE");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 3);        
			}
		});
		actMap.put(FOUR, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("FOUR");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 4);        
			}
		});
	}

	public void setDirectoryData(String[][] data) {
		if (data.length > 0) {
			int row = directoryTable.getSelectedRow();
			if (MainWindow.butFilter.getText().equalsIgnoreCase(MainWindow.SHOW_ALL))
				fileHeaderTableModel.setData(data);
			else {
				int i = 0;
				String[][] filtered = new String[data.length][];
				for (String[] header : data) {
					String toCall = header[FileHeaderTableModel.TO];
					if (toCall != null && !toCall.equalsIgnoreCase(""))
						filtered[i++] = header;
				}
				data = new String[i][];
				int j = 0;
///////				if (filtered.length > 0 && filtered[0] != null) {
				for (int j1=0; j1<i; j1++)
					data[j1] = filtered[j1];
				if (data.length > 0)
					fileHeaderTableModel.setData(data);
				else {
					fileHeaderTableModel.setData(FileHeaderTableModel.BLANK);
				}
			}
			if (row >=0)
				directoryTable.setRowSelectionInterval(row, row);
		} else {
			fileHeaderTableModel.setData(FileHeaderTableModel.BLANK);
		}
		String holes = "??";
		int h = Config.spacecraft.directory.getHolesList().size();
		int age = Config.spacecraft.directory.getAge();
		MainWindow.lblDirHoles.setText("DIR: " + h + " holes. Age: " + age + " days");
	}
	
	abstract protected void displayRow(JTable table, int row);

	public void setPriority(long id, int pri) {
		Config.spacecraft.directory.setPriority(id, pri);
		setDirectoryData(Config.spacecraft.directory.getTableData());
	}

	protected void setPriority(JTable table, int row, int pri) {
		String idstr = (String) table.getValueAt(row, 0);
		//Log.println("Set Priority" +idstr + " to " + pri);
		Long id = Long.decode("0x"+idstr);
		if (Config.spacecraft.directory.getPfhById(id).getState() == PacSatFileHeader.MISSING)
			Log.infoDialog("Request Ignored", "This file is missing on the server, so it cannot be requested");
		else if (Config.spacecraft.directory.getPfhById(id).getState() == PacSatFileHeader.MSG ||
				Config.spacecraft.directory.getPfhById(id).getState() == PacSatFileHeader.NEWMSG)
			;
		else {
			setPriority(id, pri);
			if (row < directoryTable.getRowCount()-1) {
				directoryTable.setRowSelectionInterval(row+1, row+1);
				directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(row+1, 0, true)));
			} else
				directoryTable.setRowSelectionInterval(row, row);
		}
	}

	public void mouseClicked(MouseEvent e) {
		int row = directoryTable.rowAtPoint(e.getPoint());
		int col = directoryTable.columnAtPoint(e.getPoint());
		if (row >= 0 && col >= 0) {
			//Log.println("CLICKED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());

			String id = (String) directoryTable.getValueAt(row, 0);
			MainWindow.txtFileId.setText(id);
			Long lid = Long.decode("0x"+id);
			PacSatFile pf = new PacSatFile(Config.spacecraft.directory.dirFolder, lid);
			//Log.println(pf.getHoleListString());
			if (e.getClickCount() == 2)
				displayRow(directoryTable, row);
			directoryTable.setRowSelectionInterval(row, row);
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
	
	/**
	 * Color the rows in the directory so that we know when we have data
	 * @author chris
	 *
	 */
	public class DirTableCellRenderer extends DefaultTableCellRenderer {

	    // This is a overridden function which gets executed for each action to the dir table
		public Component getTableCellRendererComponent (JTable table, 
				Object obj, boolean isSelected, boolean hasFocus, int row, int column) {

			Component cell = super.getTableCellRendererComponent(
					table, obj, isSelected, hasFocus, row, column);
			// Color the row based on its status
			String status = (String) table.getValueAt(row, 2);
			String toCallsign = (String) table.getValueAt(row, 3);
			if (!isSelected)
			if (status.equalsIgnoreCase("")) { // We have header but no content
				cell.setForeground(Color.gray);
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.QUE]) ) { 
				cell.setForeground(Color.blue);
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.SENT]) ) { 
				cell.setForeground(Color.black);
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.REJ])  ) {
				cell.setForeground(Color.red);
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.PARTIAL]) ) { 
				cell.setForeground(Color.black);
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.NEWMSG])) { 
				Font font = cell.getFont();
				cell.setFont(font.deriveFont(Font.BOLD));
				if (toCallsign.startsWith(Config.get(Config.CALLSIGN)))
					cell.setForeground(Color.red);
				else
					cell.setForeground(Color.blue);
			}  else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.MSG])) { 
				Font font = cell.getFont();
				cell.setFont(font.deriveFont(Font.PLAIN));
				if (toCallsign.startsWith(Config.get(Config.CALLSIGN)))
					cell.setForeground(Color.red);
				else
					cell.setForeground(Color.blue);
			}
			return cell;
		}
	} 
}
