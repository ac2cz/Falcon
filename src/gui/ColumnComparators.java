package gui;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

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
 *
 * Comparators used by TableRowSorter for table models that hold their data as
 * Strings.  The cell values are Strings, so the default sort is lexical.  These
 * comparators recover the underlying ordering without changing the model.
 *
 * IMPORTANT - total ordering.  A naive "parse as a number, otherwise compare as
 * a String" comparator is not transitive, and TimSort will eventually detect
 * that and throw IllegalArgumentException: "Comparison method violates its
 * general contract".  These comparators avoid that by ranking the values first:
 *
 *   rank 0 - the value parsed, compare by the parsed value
 *   rank 1 - the value did not parse, compare as a String
 *
 * All of rank 0 sorts before all of rank 1, and the comparison within each rank
 * is itself a total order, so the whole comparator is a total order.
 *
 * These are only ever called from the Swing Event Dispatch Thread, which is why
 * the shared SimpleDateFormat below is safe.  Do not call them from a worker
 * thread without giving each thread its own format.
 *
 */
public class ColumnComparators {

	private ColumnComparators() { } // static only

	/**
	 * Sort a column of numbers held as Strings.  Handles thousands separators
	 * and decimals.  Blank cells and anything that is not a number sort after
	 * all of the numbers.
	 */
	public static final Comparator<Object> NUMERIC = new Comparator<Object>() {
		public int compare(Object o1, Object o2) {
			String s1 = str(o1);
			String s2 = str(o2);
			BigDecimal n1 = number(s1);
			BigDecimal n2 = number(s2);
			if (n1 != null && n2 != null) {
				int c = n1.compareTo(n2);
				if (c != 0) return c;
				return s1.compareToIgnoreCase(s2); // stable tie break, e.g. "01" vs "1"
			}
			if (n1 != null) return -1; // numbers before non numbers
			if (n2 != null) return 1;
			return s1.compareToIgnoreCase(s2);
		}
	};

	/**
	 * Sort a column of dates held as Strings.  The pattern must match the format
	 * that was used to build the table data, otherwise every cell fails to parse
	 * and the column falls back to the lexical order, which is the behaviour we
	 * have today.  So a wrong pattern is not a crash, it is just no improvement.
	 *
	 * @param pattern a SimpleDateFormat pattern, e.g. "dd MMM yy HH:mm:ss"
	 */
	public static Comparator<Object> dateComparator(final String pattern) {
		final SimpleDateFormat fmt = new SimpleDateFormat(pattern);
		fmt.setLenient(false);
		return new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				String s1 = str(o1);
				String s2 = str(o2);
				Date d1 = date(fmt, s1);
				Date d2 = date(fmt, s2);
				if (d1 != null && d2 != null) {
					int c = d1.compareTo(d2);
					if (c != 0) return c;
					return s1.compareToIgnoreCase(s2);
				}
				if (d1 != null) return -1; // parsed dates before unparsable cells
				if (d2 != null) return 1;
				return s1.compareToIgnoreCase(s2);
			}
		};
	}

	/**
	 * Sort a column of hex numbers held as Strings, with or without a 0x prefix.
	 * Not wired up by default, but here in case the File id column is ever
	 * displayed in hex again.
	 */
	public static final Comparator<Object> HEX = new Comparator<Object>() {
		public int compare(Object o1, Object o2) {
			String s1 = str(o1);
			String s2 = str(o2);
			BigDecimal n1 = hex(s1);
			BigDecimal n2 = hex(s2);
			if (n1 != null && n2 != null) {
				int c = n1.compareTo(n2);
				if (c != 0) return c;
				return s1.compareToIgnoreCase(s2);
			}
			if (n1 != null) return -1;
			if (n2 != null) return 1;
			return s1.compareToIgnoreCase(s2);
		}
	};

	private static String str(Object o) {
		if (o == null) return "";
		return o.toString().trim();
	}

	/**
	 * @return the value as a BigDecimal, or null if it is blank or does not parse
	 */
	private static BigDecimal number(String s) {
		if (s.length() == 0) return null;
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ',' || c == ' ') continue; // 1,024 and 1 024 are both 1024
			sb.append(c);
		}
		if (sb.length() == 0) return null;
		try {
			return new BigDecimal(sb.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static BigDecimal hex(String s) {
		if (s.length() == 0) return null;
		String t = s;
		if (t.length() > 2 && (t.startsWith("0x") || t.startsWith("0X")))
			t = t.substring(2);
		try {
			return new BigDecimal(new java.math.BigInteger(t, 16));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * @return the value as a Date, or null if it is blank or does not parse
	 */
	private static Date date(SimpleDateFormat fmt, String s) {
		if (s.length() == 0) return null;
		try {
			return fmt.parse(s);
		} catch (ParseException e) {
			return null;
		}
	}
}