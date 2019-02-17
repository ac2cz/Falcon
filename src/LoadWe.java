import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.segDb.DataTable;

import common.Config;
import common.Log;
import fileStore.Directory;
import fileStore.MalformedPfhException;
import fileStore.telem.LogFileWE;

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
		Config.init("PacSatServer.properties");
		File current = new File(System.getProperty("user.dir"));
		Config.currentDir = current.getAbsolutePath();
		Config.set(Config.LOGFILE_DIR, Config.currentDir);
		Config.homeDir = Config.currentDir;
		
		Config.load();
		
		Log.init(Config.get(Config.LOGFILE_DIR) + File.separator + "PacSatServer");
		Log.showGuiDialogs = false;
		try {
			Config.simpleStart();
		} catch (com.g0kla.telem.data.LayoutLoadException e) {
			Log.println("ERROR - Could not load the layout, telem DB has not been initiailized.  No telemetry can be stored.\n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.println("ERROR - Could not load the layout, telem DB has not been initiailized.  No telemetry can be stored.\n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
		
		importWE(dirName);
	}
	
	/**
	 * Get a list of all the files in the dir and import them
	 */
	private static void importWE(String stpDir ) {
		//PayloadDbStore payload = initPayloadDB(u,p,db);
		String dir = stpDir;
		int n = 0;
		Log.println("IMPORT WE files from " + dir);
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && listOfFiles[i].getName().startsWith("we") ) {
					Log.println("Loading data from: " + listOfFiles[i].getName());
					File destFile = new File(Config.spacecraftSettings.directory.dirFolder + File.separator + listOfFiles[i].getName());
					try {
						DataTable.copyFile(listOfFiles[i], destFile);
						RandomAccessFile saveFile = new RandomAccessFile(destFile, "rw");
						LogFileWE we;

						we = new LogFileWE(destFile.getPath());
						if (we.records != null)
							for (DataRecord d : we.records) {
								Config.db.add(d);
							}
						n++;
					} catch (MalformedPfhException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (LayoutLoadException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (DataLoadException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (i%100 == 0)
						Log.println("Loaded: " + Math.round(100.0*i/listOfFiles.length) +"%");
				}
			}
		}
		Log.println("Files Processed: " + n);
	}
}
