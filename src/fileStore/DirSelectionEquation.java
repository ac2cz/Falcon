package fileStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import common.SpacecraftSettings;

public class DirSelectionEquation implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final int DEFAULT_PRIORITY = 2;
	public static final int ALWAYS = 0;
	public static final int CURRENT_DAY = 1;
	public static final int DAY_1_OLD = 2;
	public static final int DAY_2_OLD = 3;
	public static final int DAY_3_OLD = 4;
	public static final int DAY_4_OLD = 5;
	public static final int DAY_5_OLD = 6;
	public static final int DAY_10_OLD = 7;
	public static final int DAY_30_OLD = 8;
	public static final int OLD_FILES = 9;
	public static final String[] DATES_RESTRICTIONS = {"Any date", "Current Day", "1 day or older", "2 days or older",
			"3 days or older", "4 days or older","5 days or older","10 days or older","30 days or older","Old Files"};

	ArrayList<DirSelectionCriteria> selectionCriteria;
	String dirFolder;
	int priority = DEFAULT_PRIORITY;
	int dateRestriction = ALWAYS;
	SpacecraftSettings spacecraftSettings;
	
	public DirSelectionEquation(SpacecraftSettings spacecraftSettings, int priority, int dateRestriction) {
		selectionCriteria = new ArrayList<DirSelectionCriteria>();
		this.spacecraftSettings = spacecraftSettings;
		this.priority = priority;
		this.dateRestriction = dateRestriction;
	}
	
	public void add(DirSelectionCriteria criteria) {
		selectionCriteria.add(criteria);
	}
	
	public String getValue(int row) {
		if (row < selectionCriteria.size())
			return selectionCriteria.get(row).value;
		return "";
	}

	public String getField(int row) {
		if (row < selectionCriteria.size())
			return selectionCriteria.get(row).getField();
		return "";
	}

	public int getOperation(int row) {
		if (row < selectionCriteria.size())
			return selectionCriteria.get(row).operation;
		return 0;
	}
	
	public int getOpType(int row) {
		if (row < selectionCriteria.size())
			return selectionCriteria.get(row).opType;
		return 0;
	}
	
	public void setPriority(int pri) {
		priority = pri;
	}

	public int getPriority() {
		return priority;
	}

	public int getDateRestriction() {
		return dateRestriction;
	}
	
	// Create a hash key so we can avoid duplicate equations
	public String getHashKey() {
		String s = "";
		for (DirSelectionCriteria crit : selectionCriteria)
			s = s + crit.getHashKey() + "|";
		s = s + dateRestriction;
		return s;
	}
	
	/**
	 * Given a list of selection criteria and a PFH, do we select this?
	 * Iterate over the list seeing if all of them match this PFH
	 * but first the date restriction must match
	 * @param pfh
	 * @return
	 */
	public boolean matchesPFH(PacSatFileHeader pfh) {
		if (dateRestriction != ALWAYS) {
			PacSatField field = pfh.getFieldById(PacSatFileHeader.UPLOAD_TIME);
			Date uploadTime = field.getDateValue();
			Calendar cal1 = Calendar.getInstance();
			cal1.setTime(uploadTime); 
			Date now = new Date();
			Calendar cal2 = Calendar.getInstance();
			cal2.setTime(now); 
			if (dateRestriction == CURRENT_DAY) {
				int d1 = cal1.get(Calendar.DAY_OF_YEAR);
				int d2 = cal2.get(Calendar.DAY_OF_YEAR);
				int y1 =  cal1.get(Calendar.YEAR);
				int y2 = cal2.get(Calendar.YEAR);
				boolean sameDay = d1 == d2 && y1 == y2;
				if (!sameDay)
					return false;
			} else if (dateRestriction == OLD_FILES) {
				long diff = now.getTime() - uploadTime.getTime();
				long days = diff/(24*60*60*1000);
				if ( days < spacecraftSettings.getInt(SpacecraftSettings.DIR_AGE) ) {
					return false;
				}
			} else {
				// we have a number of days, which for now are just 1 - 5
				int days_old = dateRestriction - 1;
				if (dateRestriction == DAY_10_OLD) days_old = 10;
				if (dateRestriction == DAY_30_OLD) days_old = 30;
				long diff =  now.getTime() - uploadTime.getTime();
				long days = diff/(24*60*60*1000);
				if (days <= days_old)
					return false;
			}
		}
		for (DirSelectionCriteria equation : selectionCriteria) {
			int key = equation.getPfhFieldKey();
			PacSatField field = pfh.getFieldById(key);
			if (field == null)
				return false;
			if (!equation.matches(field)) {
				// if any part does not match then we are done
				return false;
			}
		}
		return true; // all parts matched
	}
	
	public String toString() {
		String s = "";
		if (priority == 9)
			s = s + "N|";
		else
			s = s + priority + "|";
		int j = 0;
		for (DirSelectionCriteria crit : selectionCriteria) {
			if (j > 0) s = s + " AND ";
			s = s + crit;
			j++;
		}
		s = s + " |"+DATES_RESTRICTIONS[dateRestriction];
		
		return s;
	}
}
