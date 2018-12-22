package gui;

import java.io.File;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import common.Config;
import common.Log;
import fileStore.PacSatFile;
import fileStore.PacSatFileHeader;

public class DirectoryPanel extends TablePanel {

	DirectoryPanel(boolean userFiles) {	
		super(userFiles);
		TableColumnModel tcm = directoryTable.getColumnModel();
		tcm.removeColumn( tcm.getColumn(11) );

	}
	protected void displayRow(JTable table, int row) {
		String id = (String) table.getValueAt(row, 0);
		//Log.println("Open file: " +id + ".act");
		//File f = new File("C:/Users/chris/Desktop/workspace/Falcon/" + id + ".act");
		File f = new File(Config.spacecraft.directory.dirFolder + File.separator + id + ".act");
		PacSatFile psf = new PacSatFile(Config.spacecraft.directory.dirFolder, Long.decode("0x"+id));
		if (f.exists()) {
			EditorFrame editor = null;
			try {
				editor = new EditorFrame(psf, false);
				editor.setVisible(true);
				int state = psf.getPfh().getState();
				if (state == PacSatFileHeader.NEWMSG)
					psf.getPfh().setState(PacSatFileHeader.MSG);
				setDirectoryData(Config.spacecraft.directory.getTableData());
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not open file: " + f + "\n" + e.getMessage());
			}
			//DesktopApi.edit(f);
		}
	}
}
