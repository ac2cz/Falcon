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
class FileHeaderTableModel extends TemplateTableModel {
	
	FileHeaderTableModel() {
		columnNames = new String[9];
		columnNames[0] = "FileNum";
		columnNames[1] = "S";
		columnNames[2] = "To";
		columnNames[3] = "From";
		columnNames[4] = "Upload Time";
		columnNames[5] = "Size";
		columnNames[6] = "%";
		columnNames[7] = "Title";
		columnNames[8] = "Keywods";
	}
}