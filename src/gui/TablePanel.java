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

import java.awt.event.KeyEvent;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

public abstract class TablePanel extends JScrollPane implements MouseListener {

	private static final long serialVersionUID = 1L;
	public static final String SHOW_DIR_TIMES = "SHOW_DIR_TIMES";
	FileHeaderTableModel fileHeaderTableModel;
	JTable directoryTable;
	SpacecraftSettings spacecraftSettings;
	SpacecraftTab spacecraftTab;
	int holes;
	int age;
	
	TablePanel(SpacecraftSettings spacecraftSettings, SpacecraftTab spacecraftTab) {	
		super();
		this.spacecraftSettings = spacecraftSettings;
		this.spacecraftTab = spacecraftTab;
		fileHeaderTableModel = new FileHeaderTableModel();
		directoryTable = new JTable(fileHeaderTableModel);
		directoryTable.setAutoCreateRowSorter(true);
		this.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
		this.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
		this.getViewport().add(directoryTable);
		directoryTable.setFillsViewportHeight(true);
		directoryTable.setAutoResizeMode(JTable. AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		Font f = directoryTable.getFont();
		Font f2 = new Font(f.getFontName(), f.getStyle(), Config.getInt(Config.FONT_SIZE));
		directoryTable.setFont(f2);
		directoryTable.getTableHeader().setFont(f2);
		int[] columnWidths = FileHeaderTableModel.columnWidths;

		for (int i=0; i< directoryTable.getColumnModel().getColumnCount(); i++) {
			TableColumn column = directoryTable.getColumnModel().getColumn(i);
			column.setPreferredWidth(columnWidths[i]);
			column.setCellRenderer(new DirTableCellRenderer());
		}
		
		// Hide the old/new dates
		if (!Config.getBoolean(SHOW_DIR_TIMES)) {
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
		String NINE = "nine";
		String N = "N";
		String DELETE = "del";
		String BACK = "back";
		String FIND = "find";
		InputMap inMap = directoryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		inMap.put(KeyStroke.getKeyStroke("ENTER"), ENTER);
		inMap.put(KeyStroke.getKeyStroke("0"), ZERO);
		inMap.put(KeyStroke.getKeyStroke("1"), ONE);
		inMap.put(KeyStroke.getKeyStroke("2"), TWO);
		inMap.put(KeyStroke.getKeyStroke("3"), THREE);
		inMap.put(KeyStroke.getKeyStroke("4"), FOUR);
		inMap.put(KeyStroke.getKeyStroke("9"), NINE);
		inMap.put(KeyStroke.getKeyStroke("N"), N);
		inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE);
		inMap.put(KeyStroke.getKeyStroke("BACK_SPACE"), BACK);
		inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), FIND);
		ActionMap actMap = directoryTable.getActionMap();

		actMap.put(DELETE, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				System.out.println("DEL");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount()) {
					deleteRow(directoryTable,row);
					if (row >=0 && row < directoryTable.getRowCount()) {
						directoryTable.setRowSelectionInterval(row, row);
						directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(row, 0, true)));
					}
				}
			}
		});
		
		actMap.put(BACK, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				System.out.println("BACK");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount()) {
					deleteRow(directoryTable,row);
					if (row >=0 && row < directoryTable.getRowCount()) {
						directoryTable.setRowSelectionInterval(row, row);
						directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(row, 0, true)));
					}
				}
			}
		});
		
		actMap.put(FIND, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				System.out.println("FIND");
				int row = directoryTable.getSelectedRow();
				if (row > 0) {
					directoryTable.setRowSelectionInterval(row, row);
					directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(row-1, 0, true)));
				}
			}
		});

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
		actMap.put(NINE, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("FOUR");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 9);        
			}
		});
		actMap.put(N, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("FOUR");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 9);        
			}
		});
		actMap.put(FIND, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			System.out.println("FIND not yet implement");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					;//setPriority(directoryTable,row, 9);        
			}
		});
	}

	public void setDirectoryData(String[][] data) {
		if (data != null && data.length > 0) {
			int row = directoryTable.getSelectedRow();

//			int i = 0;
//			String[][] filtered = new String[data.length][];
//			for (String[] header : data) {
//				String toCall = header[FileHeaderTableModel.TO];
//				if (toCall != null)
//					if (Config.mainWindow.showFilter) {
//						if (!toCall.equalsIgnoreCase("") )
//							filtered[i++] = header;
//					} else // we show everything
//						filtered[i++] = header;
//			}
//			data = new String[i][];
//			int j = 0;
//			///////				if (filtered.length > 0 && filtered[0] != null) {
//			for (int j1=0; j1<i; j1++)
//				data[j1] = filtered[j1];
			if (data.length > 0)
				fileHeaderTableModel.setData(data);
			else {
				fileHeaderTableModel.setData(FileHeaderTableModel.BLANK);
			}

			if (row >=0 && row < directoryTable.getRowCount())
				directoryTable.setRowSelectionInterval(row, row);
		} else {
			fileHeaderTableModel.setData(FileHeaderTableModel.BLANK);
		}
		holes = spacecraftSettings.directory.getHolesList().size();
		if (holes > 0) holes = holes -1;
		age = spacecraftSettings.directory.getAge();

	}
	
	abstract protected void displayRow(JTable table, int row);

	abstract protected void deleteRow(JTable table, int row);
	
	abstract protected void setPriority(JTable table, int row, long id, int pri);


	protected void setPriority(JTable table, int row, int pri) {
		String idstr = (String) table.getValueAt(row, 0);
		//Log.println("Set Priority" +idstr + " to " + pri);
		Long id = Long.decode("0x"+idstr);
		if (spacecraftSettings.directory.getPfhById(id).getState() == PacSatFileHeader.MISSING) {
			if (pri == 0)
				setPriority(table, row, id, pri);
			else
				Log.infoDialog("Request Ignored", "This file is missing on the server, so it cannot be requested");
		} else if (spacecraftSettings.directory.getPfhById(id).getState() == PacSatFileHeader.MSG ||
				spacecraftSettings.directory.getPfhById(id).getState() == PacSatFileHeader.NEWMSG) {
			if (pri == 0)
				setPriority(table, row, id, pri);
		} else {
			setPriority(table, row, id, pri);
		}
		if (row < directoryTable.getRowCount()-1) {
			directoryTable.setRowSelectionInterval(row+1, row+1);
			directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(row+1, 0, true)));
		} else
			directoryTable.setRowSelectionInterval(row, row);
	}

	public void mouseClicked(MouseEvent e) {
		int row = directoryTable.rowAtPoint(e.getPoint());
		int col = directoryTable.columnAtPoint(e.getPoint());
		if (row >= 0 && col >= 0) {
			//Log.println("CLICKED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());

			String id = (String) directoryTable.getValueAt(row, 0);
			if (id != null) {
				spacecraftTab.txtFileId.setText(id);
				try {
					Long lid = Long.decode("0x"+id);
					PacSatFile pf = new PacSatFile(spacecraftSettings, spacecraftSettings.directory.dirFolder, lid);
					//Log.println(pf.getHoleListString());
					if (e.getClickCount() == 2)
						displayRow(directoryTable, row);
					directoryTable.setRowSelectionInterval(row, row);
				} catch (NumberFormatException e1) {
					// Ignore, not a valid row
				}
			}
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
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.MISSING]) ) { 
				cell.setForeground(Color.gray);
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.QUE]) ) { 
				cell.setForeground(Color.blue);
			} else if (status.equalsIgnoreCase(PacSatFileHeader.states[PacSatFileHeader.DRAFT]) ) { 
				cell.setForeground(Color.gray);
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
