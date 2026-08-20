package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import java.awt.event.KeyEvent;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.PacSatFileHeader;

/**
 * NOTE - this used to extend JScrollPane.  It now extends JPanel and holds the
 * scroll pane itself, so that the filter bar can be added below the table.  If
 * any caller was treating a TablePanel as a JScrollPane, e.g. calling
 * getViewport() or getVerticalScrollBar(), that call site needs to move to
 * getScrollPane().
 */
public abstract class TablePanel extends JPanel implements MouseListener {

	private static final long serialVersionUID = 1L;

	/**
	 * The format that the date columns are rendered in.  This must match the
	 * format used to build the directory data, otherwise the date columns fall
	 * back to a lexical sort, which is what we have today.  A wrong pattern is
	 * therefore harmless, it just means no improvement on those columns.
	 */
	public static final String DIR_DATE_FORMAT = "dd MMM yy HH:mm:ss";

	FileHeaderTableModel fileHeaderTableModel;
	JTable directoryTable;
	JScrollPane scrollPane;
	SpacecraftSettings spacecraftSettings;
	SpacecraftTab spacecraftTab;
	int holes;
	int age;

	private TableRowSorter<FileHeaderTableModel> sorter;
	private JTextField[] filterFields;
	private JButton resetSort;
	private JButton butPri[], butDelete;

	TablePanel(SpacecraftSettings spacecraftSettings, SpacecraftTab spacecraftTab) {	
		super();
		setLayout(new BorderLayout());
		this.spacecraftSettings = spacecraftSettings;
		this.spacecraftTab = spacecraftTab;
		fileHeaderTableModel = new FileHeaderTableModel();
		directoryTable = new JTable(fileHeaderTableModel);

		// We install our own sorter below so that we hold a reference to it for
		// the comparators, the un-sort button and the filter bar.
		directoryTable.setAutoCreateRowSorter(false);

		scrollPane = new JScrollPane(directoryTable,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);
		directoryTable.setFillsViewportHeight(true);
		directoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
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
		if (!Config.getBoolean(Config.SHOW_DIR_TIMES)) {
			TableColumnModel tcm = directoryTable.getColumnModel();
			tcm.removeColumn( tcm.getColumn(5) );
			tcm.removeColumn( tcm.getColumn(6) ); // its not 7 because we already removed a column
		}

		// The sorter works in model coordinates, so hiding columns above does not
		// affect it.  The header still sorts the right column because JTableHeader
		// converts the view index for us.
		installSorter();
		if (Config.getBoolean(Config.SHOW_PRIORITY_BAR))
			add(createPriorityBar(), BorderLayout.NORTH);
		if (Config.getBoolean(Config.SHOW_DIR_FILTER_BAR))
		add(createFilterBar(), BorderLayout.SOUTH);

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
		// WHEN_ANCESTOR_OF_FOCUSED_COMPONENT is on the table, and the filter fields
		// are siblings of the scroll pane rather than descendants of the table, so
		// typing digits or N into a filter box does not fire these actions.
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
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// Ctrl-F now jumps into the first filter box
				if (filterFields != null && filterFields.length > 0) {
					filterFields[0].requestFocusInWindow();
					filterFields[0].selectAll();
				}
			}
		});

		actMap.put(PREV, new AbstractAction() {
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("ENTER");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					displayRow(directoryTable,row);        
			}
		});
		actMap.put(ZERO, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("NONE");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 0);        
			}
		});
		actMap.put(ONE, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("ONE");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 1);        
			}
		});
		actMap.put(TWO, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
		//		System.out.println("TWO");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 2);        
			}
		});
		actMap.put(THREE, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
		//		System.out.println("THREE");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 3);        
			}
		});
		actMap.put(FOUR, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("FOUR");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 4);        
			}
		});
		actMap.put(NINE, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("FOUR");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 9);        
			}
		});
		actMap.put(N, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
			//	System.out.println("FOUR");
				int row = directoryTable.getSelectedRow();
				if (row >= 0 && row < directoryTable.getRowCount())
					setPriority(directoryTable,row, 9);        
			}
		});
	}

	/**
	 * Install the row sorter and give each column the comparator that its data
	 * actually needs.  Columns marked SORT_STRING are left with the default,
	 * which is the locale Collator for a String column.
	 */
	private void installSorter() {
		sorter = new TableRowSorter<FileHeaderTableModel>(fileHeaderTableModel);
		for (int col = 0; col < FileHeaderTableModel.MAX_TABLE_FIELDS; col++) {
			switch (FileHeaderTableModel.columnSortType[col]) {
			case FileHeaderTableModel.SORT_NUM:
				sorter.setComparator(col, ColumnComparators.NUMERIC);
				break;
			case FileHeaderTableModel.SORT_DATE:
				sorter.setComparator(col, ColumnComparators.dateComparator(DIR_DATE_FORMAT));
				break;
			default:
				break; // leave the default String comparator in place
			}
		}
		// Do not re-sort on a single cell update.  Otherwise the row you just set
		// the priority on jumps out from under the cursor.
		sorter.setSortsOnUpdates(false);
		directoryTable.setRowSorter(sorter);
	}

	private JPanel createPriorityBar() {
		JPanel priPanel = new JPanel();
		priPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel lblPriority = new JLabel("Set Priority: ");
		Font lf = lblPriority.getFont();
		lblPriority.setFont(lf.deriveFont(lf.getStyle() | Font.BOLD));
		priPanel.add(lblPriority);
		
		butPri = new JButton[5];
		for (int i=0; i<5; i++) {
			 final int pri = i;          // effectively final, safe to capture
			butPri[i] = new JButton(""+i);
			butPri[i].setToolTipText("Select a file and press to set priority for download");
			butPri[i].addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int row = directoryTable.getSelectedRow();
					if (row >= 0 && row < directoryTable.getRowCount()) {
						setPriority(directoryTable,row, pri);
					}
				}
			});
			priPanel.add(butPri[i]);
		}
		if (spacecraftSettings.getBoolean(SpacecraftSettings.IS_COMMAND_STATION)) {
			JLabel lblCmds = new JLabel("    Cmds: ");
			lblCmds.setFont(lf.deriveFont(lf.getStyle() | Font.BOLD));
			priPanel.add(lblCmds);
			butDelete = new JButton("Delete");
			butDelete.setToolTipText("Delete the file");
			butDelete.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(ActionEvent e) {
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
			priPanel.add(butDelete);
		}
		priPanel.add(new Box.Filler(new Dimension(20,10), new Dimension(20,10), new Dimension(20,10)));

		return priPanel;
	}

	/**
	 * The bar along the bottom holding the un-sort button and the filter boxes
	 */
	private JPanel createFilterBar() {
		JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		resetSort = new JButton("Un-Sort");
		resetSort.setToolTipText("Clear the sort and any filters, and show the directory in its natural order");
		resetSort.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetSortAndFilter();
			}
		});
		filterPanel.add(resetSort);

		filterPanel.add(new Box.Filler(new Dimension(20,10), new Dimension(20,10), new Dimension(20,10)));
		JLabel lblFilter = new JLabel("Filter: ");
		Font lf = lblFilter.getFont();
		lblFilter.setFont(lf.deriveFont(lf.getStyle() | Font.BOLD));
		filterPanel.add(lblFilter);

		int[] cols = FileHeaderTableModel.filterColumns;
		filterFields = new JTextField[cols.length];
		for (int i = 0; i < cols.length; i++) {
			filterFields[i] = addFilterField(filterPanel,
					fileHeaderTableModel.getColumnName(cols[i]), 8);
		}
		return filterPanel;
	}

	private JTextField addFilterField(JPanel parent, String name, int width) {
		parent.add(new JLabel(name + " "));
		JTextField field = new JTextField(width);
		field.setToolTipText("Show only the rows where " + name + " contains this text");
		field.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { applyFilter(); }
			public void removeUpdate(DocumentEvent e) { applyFilter(); }
			public void changedUpdate(DocumentEvent e) { applyFilter(); }
		});
		parent.add(field);
		return field;
	}

	/**
	 * Build a filter from whatever is typed in the boxes.  An empty box is
	 * ignored, several non empty boxes are ANDed together.  The text is quoted
	 * so that a stray * or ( from the user is matched literally rather than
	 * throwing PatternSyntaxException.
	 */
	private void applyFilter() {
		if (sorter == null) return;
		List<RowFilter<Object,Object>> filters = new ArrayList<RowFilter<Object,Object>>();
		int[] cols = FileHeaderTableModel.filterColumns;
		for (int i = 0; i < cols.length; i++) {
			String text = filterFields[i].getText().trim();
			if (text.length() == 0) continue;
			filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), cols[i]));
		}
		if (filters.isEmpty())
			sorter.setRowFilter(null);
		else
			sorter.setRowFilter(RowFilter.<Object,Object>andFilter(filters));
	}

	/**
	 * Put the table back the way it came off the spacecraft.  setSortKeys(null)
	 * restores the model order and clears the arrows in the header.
	 */
	public void resetSortAndFilter() {
		if (filterFields != null)
			for (int i = 0; i < filterFields.length; i++)
				filterFields[i].setText(""); // fires applyFilter, which clears the filter
		if (sorter != null) {
			sorter.setRowFilter(null);
			sorter.setSortKeys(null);
		}
		directoryTable.getTableHeader().repaint();
		directoryTable.clearSelection();
	}

	/**
	 * For any caller that used to treat this panel as a JScrollPane
	 */
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public void setDirectoryData(String[][] data) {
		// With a sorter installed, remembering the row number is meaningless
		// because the row that number points at will have moved.  Remember the
		// file id instead and find it again after the refresh.
		String selectedId = null;
		int viewRow = directoryTable.getSelectedRow();
		if (viewRow >= 0 && viewRow < directoryTable.getRowCount()) {
			Object o = directoryTable.getValueAt(viewRow, 0);
			if (o != null && o.toString().length() > 0)
				selectedId = o.toString();
		}

		if (data != null && data.length > 0)
			fileHeaderTableModel.setData(data);
		else
			fileHeaderTableModel.setData(FileHeaderTableModel.BLANK);

		if (selectedId != null) {
			for (int r = 0; r < directoryTable.getRowCount(); r++) {
				if (selectedId.equals(directoryTable.getValueAt(r, 0))) {
					directoryTable.setRowSelectionInterval(r, r);
					directoryTable.scrollRectToVisible(new Rectangle(directoryTable.getCellRect(r, 0, true)));
					break;
				}
			}
		}
		holes = spacecraftSettings.directory.getHolesList().size();
		if (holes > 0) holes = holes -1;
		age = spacecraftSettings.directory.getAge();

	}

	/**
	 * NOTE - row is a VIEW row.  Use table.getValueAt(row, col), which converts
	 * for you.  If an implementation reaches into the table model directly then
	 * it must call table.convertRowIndexToModel(row) first.
	 */
	abstract protected void displayRow(JTable table, int row);

	/** NOTE - row is a VIEW row, see displayRow */
	abstract protected void deleteRow(JTable table, int row);

	/** NOTE - row is a VIEW row, see displayRow */
	abstract protected void setPriority(JTable table, int row, long id, int pri);


	protected void setPriority(JTable table, int row, int pri) {
		String idstr = (String) table.getValueAt(row, 0);
		if (idstr == null || idstr.length() == 0) return;
		//Log.println("Set Priority" +idstr + " to " + pri);
		//Long id = Long.decode("0x"+idstr);
		Long id;
		try {
			id = Long.parseLong(idstr);
		} catch (NumberFormatException e) {
			return; // not a valid row, e.g. the blank row
		}
		if (spacecraftSettings.directory.getPfhById(id) != null) {
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
					//Long lid = Long.parseLong(id);
					//PacSatFile pf = new PacSatFile(spacecraftSettings, spacecraftSettings.directory.dirFolder, lid);
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
		static final long serialVersionUID = 1L;

		// This is a overridden function which gets executed for each action to the dir table
		public Component getTableCellRendererComponent (JTable table, 
				Object obj, boolean isSelected, boolean hasFocus, int row, int column) {

			Component cell = super.getTableCellRendererComponent(
					table, obj, isSelected, hasFocus, row, column);
			// Color the row based on its status.  row and column here are VIEW
			// coordinates, and getValueAt converts them, so this is still correct
			// when the table is sorted or filtered.
			String status = asString(table.getValueAt(row, FileHeaderTableModel.STATE));
			String toCallsign = asString(table.getValueAt(row, FileHeaderTableModel.TO));
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

		private String asString(Object o) {
			if (o == null) return "";
			return o.toString();
		}
	} 
}