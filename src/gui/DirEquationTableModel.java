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
public class DirEquationTableModel extends TemplateTableModel {
	
	public static int[] columnWidths = {0,80};
	public static String[][] BLANK = {{"", ""}};
	
	public static final int MAX_TABLE_FIELDS = 2;
	
	DirEquationTableModel() {
		columnNames = new String[MAX_TABLE_FIELDS];
		columnNames[0] = "Key";
		columnNames[1] = "Selection Equations";
	}
}