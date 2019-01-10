package gui;

import java.io.IOException;

import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.gui.DataRecordTableModel;
import com.g0kla.telem.gui.DisplayModule;
import com.g0kla.telem.gui.ModuleTab;

import common.Config;
import common.Log;
import common.Spacecraft;

public class WodTab extends ModuleTab {
	DataRecord record;
	
	public WodTab() {
		super(Config.layouts[0]);
	}
	
	public void updateTab(DataRecord record, boolean refreshTable) {
	
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.updateRtValues(record);
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.updateRtValues(record);
		}
		
		if (refreshTable)
			parseFrames();
		if (refreshTable) {
//			lblLive.setForeground(Config.AMSAT_RED);
//			lblLive.setText(LIVE);
		} else {
//			lblLive.setForeground(Color.BLACK);
//			lblLive.setText(DISPLAY);
		}
	}
	
	@Override
	public void parseFrames() {
		String[][] data = null;
		try {
			data = Config.db.getTableData(SAMPLES, 0, START_RESET, START_UPTIME, false, reverse, layout.name);
		} catch (NumberFormatException e) {
			Log.errorDialog("ERROR", "Issue parsing WOD telemetry: " + e.getMessage());
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Issue loading WOD telemetry: " + e.getMessage());
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Issue loaring layout for WOD telemetry: " + e.getMessage());
		} catch (DataLoadException e) {
			Log.errorDialog("ERROR", "Issue loading data for WOD telemetry: " + e.getMessage());
		}
		if (data != null && data.length > 0) {
			parseTelemetry(data);
			MainWindow.frame.repaint();
		}		
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("WODTab");
		running = true;
		done = false;
		boolean justStarted = true;
		while(running) {
			
			// Check if we have new data
			boolean updated = Config.db.getUpdated(Spacecraft.WOD_LAYOUT);
			if (updated) {
				 Config.db.setUpdated(Spacecraft.WOD_LAYOUT, false);
				 try {
					record = Config.db.getLatest(Spacecraft.WOD_LAYOUT);
					updateTab(record, true);
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (DataLoadException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: WODTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 
		}
		
	}

	@Override
	protected void displayRow(int fromRow, int row) {
		long reset_l = (long) table.getValueAt(row, DataRecordTableModel.RESET_COL);
    	long uptime = (long)table.getValueAt(row, DataRecordTableModel.UPTIME_COL);
    	//Log.println("RESET: " + reset);
    	//Log.println("UPTIME: " + uptime);
    	int reset = (int)reset_l;
    	try {
			record = Config.db.getLatest(0, reset, uptime, 0, Spacecraft.WOD_LAYOUT, false);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DataLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if (record != null)
    		updateTab(record, false);
    	if (fromRow == NO_ROW_SELECTED)
    		fromRow = row;
    	if (fromRow <= row)
    		table.setRowSelectionInterval(fromRow, row);
    	else
    		table.setRowSelectionInterval(row, fromRow);
	}


}
