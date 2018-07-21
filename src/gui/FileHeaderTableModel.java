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
@SuppressWarnings({ "serial", "unchecked" })
public class FileHeaderTableModel extends TemplateTableModel {
	
	public static int[] columnWidths = {55,25,30,55,55,80,80,80,35,30,200,55,60};
	public static String[][] BLANK = {{"","","","","","","","","","","","",""}};
	
	public static final int MAX_TABLE_FIELDS = 13;
	public static final int TO = 3;
	public static final int HOLES = 9;
	
	FileHeaderTableModel() {
		columnNames = new String[MAX_TABLE_FIELDS];
		columnNames[0] = "FileNum";
		columnNames[1] = "Pri";
		columnNames[2] = "State";
		columnNames[TO] = "To";
		columnNames[4] = "From";
		columnNames[5] = "Old Time";
		columnNames[6] = "Upload Time";
		columnNames[7] = "New Time";
		columnNames[8] = "Size";
		columnNames[HOLES] = "Holes";
		columnNames[10] = "Title";
		columnNames[11] = "Type";
		columnNames[12] = "Keywods";
	}
}