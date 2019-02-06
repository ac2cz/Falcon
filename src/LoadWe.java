import java.io.IOException;

public class LoadWe {
	static final String usage = "LoadWe <dir-containing-we-files>\n";
	static String dirName;
	
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			dirName = args[0];
			} else {
				System.out.println(usage);
				System.exit(1);
			}
		// Have to run this on the server
		
		// Get all the files that start with WE
		
		// For each process the telem and write to the telem db
		
	}
}
