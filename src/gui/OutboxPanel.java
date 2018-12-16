package gui;

import javax.swing.table.TableColumnModel;

public class OutboxPanel extends TablePanel {

	OutboxPanel() {	

		TableColumnModel tcm = directoryTable.getColumnModel();
		tcm.removeColumn( tcm.getColumn(7) );

	}
}
