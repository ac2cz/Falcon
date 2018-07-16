package common;

import java.util.ArrayList;

public class SatManager {

	public SatManager() {
		
	}
	
	public ArrayList<Spacecraft> getSpacecraftList() {
		ArrayList<Spacecraft> sats = new ArrayList<Spacecraft>();
		sats.add(Config.spacecraft);
		return sats;
	}
}
