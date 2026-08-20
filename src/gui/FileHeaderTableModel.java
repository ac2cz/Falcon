package gui;

/**
 * 
 * AMSAT PacSat Ground
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
public class FileHeaderTableModel extends TemplateTableModel {
	private static final long serialVersionUID = 1L;
	public static int[] columnWidths = {30,25,35,55,55,80,80,80,35,30,200,35,60,60,30};
	public static String[][] BLANK =  {{"","","","","","","","","","","", "","","",""}};
	
	public static final int MAX_TABLE_FIELDS = 15;
	public static final int FILE_ID = 0;
	public static final int PRI = 1;
	public static final int STATE = 2;
	public static final int TO = 3;
	public static final int FROM = 4;
	public static final int OLD_TIME = 5;
	public static final int UPLOADED = 6;
	public static final int NEW_TIME = 7;
	public static final int SIZE = 8;
	public static final int HOLES = 9;
	public static final int TITLE = 10;
	public static final int TYPE = 11;
	public static final int KEYWORDS = 12;
	public static final int FILENAME = 13;
	public static final int ZIP = 14;

	/**
	 * How each column should be sorted.  The data in this model is held as
	 * Strings, so without this every column sorts lexically, which puts 10
	 * before 2 and sorts dates by the day of the month.  TablePanel reads this
	 * table and installs the matching comparator on the TableRowSorter.
	 */
	public static final int SORT_STRING = 0;
	public static final int SORT_NUM = 1;
	public static final int SORT_DATE = 2;

	/** One entry per column, indexed the same way as columnNames */
	public static final int[] columnSortType = {
		SORT_NUM,     // 0  File
		SORT_NUM,     // 1  Pri
		SORT_STRING,  // 2  State
		SORT_STRING,  // 3  To
		SORT_STRING,  // 4  From
		SORT_DATE,    // 5  Old Time
		SORT_DATE,    // 6  Uploaded
		SORT_DATE,    // 7  New Time
		SORT_NUM,     // 8  Size
		SORT_NUM,     // 9  Holes
		SORT_STRING,  // 10 Title
		SORT_STRING,  // 11 Type
		SORT_STRING,  // 12 Folders
		SORT_STRING,  // 13 Filename
		SORT_STRING   // 14 Zip
	};

	/**
	 * The columns that get a text box in the filter bar, in the order that they
	 * are displayed.  Keep this short, the bar has to fit across the panel.
	 */
	public static final int[] filterColumns = { TO, FROM, STATE, KEYWORDS };

	FileHeaderTableModel() {
		// Fail loud if the parallel tables ever drift apart
		if (columnSortType.length != MAX_TABLE_FIELDS)
			throw new IllegalStateException("FileHeaderTableModel: columnSortType has "
					+ columnSortType.length + " entries but MAX_TABLE_FIELDS is " + MAX_TABLE_FIELDS);
		if (columnWidths.length != MAX_TABLE_FIELDS)
			throw new IllegalStateException("FileHeaderTableModel: columnWidths has "
					+ columnWidths.length + " entries but MAX_TABLE_FIELDS is " + MAX_TABLE_FIELDS);
		for (int i = 0; i < filterColumns.length; i++)
			if (filterColumns[i] < 0 || filterColumns[i] >= MAX_TABLE_FIELDS)
				throw new IllegalStateException("FileHeaderTableModel: filterColumns[" + i
						+ "] is out of range: " + filterColumns[i]);

		columnNames = new String[MAX_TABLE_FIELDS];
		columnNames[FILE_ID] = "File";
		columnNames[PRI] = "Pri";
		columnNames[STATE] = "State";
		columnNames[TO] = "To";
		columnNames[FROM] = "From";
		columnNames[OLD_TIME] = "Old Time";
		columnNames[UPLOADED] = "Uploaded";
		columnNames[NEW_TIME] = "New Time";
		columnNames[SIZE] = "Size";
		columnNames[HOLES] = "Holes";
		columnNames[TITLE] = "Title";
		columnNames[TYPE] = "Type";
		columnNames[KEYWORDS] = "Folders";
		columnNames[FILENAME] = "Filename";
		columnNames[ZIP] = "Zip";
	}
}