package gui;

import javax.swing.JTabbedPane;

import common.SpacecraftSettings;

public class SpacecraftTab extends JTabbedPane {
	public DirectoryPanel dirPanel;
	public OutboxPanel outbox;
	SpacecraftSettings spacecraftSettings;
	
	SpacecraftTab(SpacecraftSettings spacecraftSettings) {
		this.spacecraftSettings = spacecraftSettings;
	}
}
