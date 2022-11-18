package gui;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import common.Config;
import common.Log;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

public class OutboxPanel extends TablePanel {

	OutboxPanel(SpacecraftSettings spacecraftSettings, SpacecraftTab spacecraftTab) {	
		super(spacecraftSettings, spacecraftTab);
		TableColumnModel tcm = directoryTable.getColumnModel();
		tcm.removeColumn( tcm.getColumn(7) );
	}
	
	public void deleteRow(JTable table, int row) {
		int column = 10; // 10 vs 14 because some hidden
		if (Config.getBoolean(Config.SHOW_DIR_TIMES))
			column = 12; // some extra!
		String filename = (String) table.getValueAt(row, column); 
		File f = new File(spacecraftSettings.directory.dirFolder + File.separator + filename);
		if (f.exists()) {
			Object[] options = {"Yes",
			"No"};
			int n = JOptionPane.showOptionDialog(
					MainWindow.frame,
					"Do you want to delete this outbox file?",
					"Delete File?",
					JOptionPane.YES_NO_OPTION, 
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[1]);

			if (n == JOptionPane.NO_OPTION) {
				// don't exit
			} else {
				try {
					spacecraftSettings.outbox.delete(f);
					setDirectoryData(spacecraftSettings.outbox.getTableData());
				} catch (IOException e) {
					Log.errorDialog("ERROR", "Could not remove file: " + f.getPath());
				}	
			}
		}
	}
	
	public void setPriority(JTable table, int row, long id, int pri) {
//		Config.spacecraftSettings.directory.setPriority(id, pri);
//		setDirectoryData(Config.spacecraftSettings.directory.getTableData());
	}
	
	protected void displayRow(JTable table, int row) {
		int column = 10;
		if (Config.getBoolean(Config.SHOW_DIR_TIMES))
			column = 12;
		String filename = (String) table.getValueAt(row, column); // 10 vs 14 because some hidden
		//Log.println("Open file: " +id + ".act");
		//File f = new File("C:/Users/chris/Desktop/workspace/Falcon/" + id + ".act");
		File f = new File(spacecraftSettings.directory.dirFolder + File.separator + filename);
		PacSatFile psf;
		try {
			psf = new PacSatFile(spacecraftSettings, spacecraftSettings.directory.dirFolder + File.separator + filename);
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
				editor = new EditorFrame(spacecraftSettings, psf, true);
				editor.setVisible(true);
				setDirectoryData(spacecraftSettings.outbox.getTableData());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not open file: " + f + "\n" + e.getMessage());
			}
			//DesktopApi.edit(f);
			catch (MalformedPfhException e) {
				Log.errorDialog("ERROR", "Could not open file.  Pacsat File Header Corrupt: " + f + "\n" + e.getMessage());
			}
		}
	}
}
