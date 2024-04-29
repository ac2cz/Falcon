package common;

import java.util.Date;

public class CommandParams {
	public static final int TIME_PARAM = 99099099;
	public static final String NONE = "NONE";
	public static final String MSB32BIT = "MSB32BIT";
	public static final String LIST = "LIST";
	public static final String NAME_SPACES = "namespaces";
	public String name;
	public int nameSpace;
	public int cmd;
	public int args[];
	public String argNames[];
	public boolean confirm;
	public boolean useResetUptime;
	public String description;
	
	
	public CommandParams(String name, int nameSpace, int cmd, int args[], String argNames[], boolean confirm, boolean useResetUptime, String description) {
		this.name = name;
		this.nameSpace = nameSpace;
		this.cmd = cmd;
		if (args.length > 4) throw new IllegalArgumentException("CommandParams: Max 4 arguments can be passed");
		this.args = args;
		if (argNames.length > 4) throw new IllegalArgumentException("CommandParams: Max 4 argument names can be passed");
		this.argNames = argNames;
		this.confirm = confirm;
		this.useResetUptime = useResetUptime;
		this.description = description;
	}
	
	public CommandParams(String[] items) {
		if (items.length < 14) throw new IllegalArgumentException("CommandParams: Too few items in initialization list");
		this.name = items[0];
		this.nameSpace = Integer.parseInt(items[1]);
		this.cmd = Integer.parseInt(items[2]);
		this.args = new int[4];
		this.args[0] = Integer.parseInt(items[3]);
		this.args[1] = Integer.parseInt(items[4]);
		this.args[2] = Integer.parseInt(items[5]);
		this.args[3] = Integer.parseInt(items[6]);
		this.argNames = new String[4];
		this.argNames[0] = items[7];
		this.argNames[1] = items[8];
		this.argNames[2] = items[9];
		this.argNames[3] = items[10];
		this.confirm = Boolean.parseBoolean(items[11]);
		this.useResetUptime = Boolean.parseBoolean(items[12]);
		this.description = items[13];
		
	}
	
	public String toString() {
		String s = "";
		s = name;
		return s;
	}
	
	
}
