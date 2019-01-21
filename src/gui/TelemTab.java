package gui;

import java.io.IOException;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.gui.DataRecordTableModel;
import com.g0kla.telem.gui.DisplayModule;
import com.g0kla.telem.gui.ModuleTab;
import com.g0kla.telem.segDb.SatTelemStore;
import com.g0kla.telem.segDb.Spacecraft;

import common.Config;
import common.Log;

public class TelemTab extends ModuleTab {
	DataRecord record;
	
	public TelemTab(ByteArrayLayout layout, Spacecraft sat, SatTelemStore db) {
		super(layout, sat, db);
	}
	
	public void updateTab(DataRecord record, boolean refreshTable) {
	
		for (DisplayModule mod : topModules) {
			if (mod != null)
				try {
					mod.updateValues(record);
				} catch (NumberFormatException e) {
					Log.errorDialog("ERROR", "ERROR");
					e.printStackTrace();
				} catch (IOException e) {
					Log.errorDialog("ERROR", "ERROR");
					e.printStackTrace();
				} catch (DataLoadException e) {
					Log.errorDialog("ERROR", "ERROR");
					e.printStackTrace();
				}
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
				try {
					mod.updateValues(record);
				} catch (NumberFormatException e) {
					Log.errorDialog("ERROR", "ERROR");
					e.printStackTrace();
				} catch (IOException e) {
					Log.errorDialog("ERROR", "ERROR");
					e.printStackTrace();
				} catch (DataLoadException e) {
					Log.errorDialog("ERROR", "ERROR");
					e.printStackTrace();
				}
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
			Log.errorDialog("ERROR", "Issue parsing telemetry: " + e.getMessage());
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Issue loading telemetry: " + e.getMessage());
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Issue loaring layout for telemetry: " + e.getMessage());
		} catch (DataLoadException e) {
			Log.errorDialog("ERROR", "Issue loading data for telemetry: " + e.getMessage());
		}
		if (data != null && data.length > 0) {
			parseTelemetry(data);
			MainWindow.frame.repaint();
		}		
	}
	
	@Override
	public void run() {
		running = true;
		done = false;
		boolean justStarted = true;
		while(running) {
			
			// Check if we have new data
			if (Config.db != null) { // make sure we have initialized
				boolean updated = Config.db.getUpdated(layout.name);
				if (updated) {
					Config.db.setUpdated(layout.name, false);
					try {
						record = Config.db.getLatest(layout.name);
						if (record != null)
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
			record = Config.db.getLatest(0, reset, uptime, 0, layout.name, false);
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
