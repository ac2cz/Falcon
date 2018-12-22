package gui;

import java.io.File;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import common.Config;
import common.Log;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

public class OutboxPanel extends TablePanel {

	OutboxPanel(boolean user) {	
		super(user);
		TableColumnModel tcm = directoryTable.getColumnModel();
		tcm.removeColumn( tcm.getColumn(7) );
	}
	
	protected void displayRow(JTable table, int row) {
		String filename = (String) table.getValueAt(row, 10); // 10 vs 14 because some hidden
		//Log.println("Open file: " +id + ".act");
		//File f = new File("C:/Users/chris/Desktop/workspace/Falcon/" + id + ".act");
		File f = new File(Config.spacecraft.directory.dirFolder + File.separator + filename);
		PacSatFile psf;
		try {
			psf = new PacSatFile(Config.spacecraft.directory.dirFolder + File.separator + filename);
		} catch (MalformedPfhException e1) {
			Log.errorDialog("ERROR", "Could not open message " + e1.getMessage() );
			e1.printStackTrace(Log.getWriter());
			return;
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Could not open message " + e1.getMessage() );
			e1.printStackTrace(Log.getWriter());
			return;
		}
		if (f.exists()) {
			EditorFrame editor = null;
			try {
				editor = new EditorFrame(psf, true);
				editor.setVisible(true);
				setDirectoryData(Config.spacecraft.outbox.getTableData());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not open file: " + f + "\n" + e.getMessage());
			}
			//DesktopApi.edit(f);
		}
	}
}
