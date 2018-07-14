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
	
	public static final int MAX_TABLE_FIELDS = 11;
	public static final int TO = 3;
	
	FileHeaderTableModel() {
		columnNames = new String[MAX_TABLE_FIELDS];
		columnNames[0] = "FileNum";
		columnNames[1] = "Pri";
		columnNames[2] = "State";
		columnNames[3] = "To";
		columnNames[4] = "From";
		columnNames[5] = "Upload Time";
		columnNames[6] = "Size";
		columnNames[7] = "Holes";
		columnNames[8] = "Title";
		columnNames[9] = "Type";
		columnNames[10] = "Keywods";
	}
}