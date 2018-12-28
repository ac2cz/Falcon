package fileStore;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import common.Spacecraft;

public class DirSelectionCriteria implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final int STRING_OP = 0;
	public static final int NUM_OP = 1;
	public static final String[] FIELDS = {"To Callsign", "From Callsign", "File Size", "Keywords", "Title", "User Filename"};
	public static final int[] OP_TYPE = {STRING_OP, STRING_OP, NUM_OP, STRING_OP, STRING_OP, STRING_OP};
	public static final int[] PFHFieldKey = {PacSatFileHeader.DESTINATION, PacSatFileHeader.SOURCE, PacSatFileHeader.FILE_SIZE,
			PacSatFileHeader.KEYWORDS, PacSatFileHeader.TITLE, PacSatFileHeader.USER_FILE_NAME};
	public static final int EQ = 0;
	public static final int NE = 1;
	public static final int GT = 2;
	public static final int LT = 3;
	public static final int CONTAINS = 2;
	public static final int EXCLUDES = 3;
	public static final String[] NUMERIC_OPS = {"=", "!=", ">", "<"};
	public static final String[] STRING_OPS = {"=", "!=", "contains", "excludes"};
	
	int pfhFieldKey;
	String value;
	int opType;
	int operation;
	
	public  DirSelectionCriteria(String field, int opType, int operation, String value) {
		int key = getKeyByField(field);
		pfhFieldKey = key;
		this.opType = opType;
		this.operation = operation;
		this.value = value;
	}

	public int getPfhFieldKey() { return pfhFieldKey; }
	
	public boolean matches(String input) {
		if (opType == NUM_OP) {
			try {
				int in = Integer.parseInt(input);
				int val = Integer.parseInt(value);
				if (operation == EQ && in == val) return true;
				if (operation == LT && in < val) return true;
				if (operation == GT && in > val) return true;
				if (operation == NE && in != val) return true;
			} catch (NumberFormatException e) {
				return false;
			}
		} else {
			if (operation == EQ && input.equalsIgnoreCase(value)) return true;
			if (operation == CONTAINS && input.contains(value)) return true;
			if (operation == EXCLUDES && !input.contains(value)) return true;
			if (operation == NE && !input.equalsIgnoreCase(value)) return true;
		}
			
		return false;
	}

	public static int getKeyByField(String field) {
		int j = 0;
		for (String name : FIELDS) {
			if (name.equalsIgnoreCase(field))
				return PFHFieldKey[j];
			j++;
		}
		return 0;
	}

	public static String getFieldByKey(int key) {
		int j = 0;
		for (int field : PFHFieldKey) {
			if (field == key)
				return FIELDS[j];
			j++;
		}
		return null;
	}

	public static int getOpTypeByField(String field) {
		int j = 0;
		for (String name : FIELDS) {
			if (name.equalsIgnoreCase(field))
				return OP_TYPE[j];
			j++;
		}
		return 0;
	}

	
	public String toString() {
		String s = "";
		
		String field = getFieldByKey(pfhFieldKey);
		if (field == null) field = "UNK";
		String op = "x";
		if (opType == STRING_OP) 
			op = STRING_OPS[operation];
		if (opType == NUM_OP) 
			op = NUMERIC_OPS[operation];
		s = s + field + " " + op + " " + value;
		
		return s;
	}
	
}
