package pacSat.frames;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.g0kla.telem.data.LayoutLoadException;

import ax25.Ax25Frame;
import common.Config;
import common.SpacecraftSettings;
import fileStore.MalformedPfhException;

class TlmMirSatFrameTest {

	@Test
	void test() throws MalformedPfhException, LayoutLoadException, IOException, FrameException {
		
		Config.init("Test");
		File current = new File(System.getProperty("user.dir"));
		
		Config.currentDir = current.getAbsolutePath();
		String logFileDir = "C:\\Users\\chris\\Desktop\\Test\\MIR-SAT-1_TEST";
		
		Config.logDirFromPassedParam = true;
		File log = new File(logFileDir);
		Config.set(Config.LOGFILE_DIR, logFileDir);
		Config.homeDir = log.getAbsolutePath();
		
		Config.simpleStart();
		SpacecraftSettings spacecraftSettings = Config.getSatSettingsByCallsign("3B8MIR");
		int[] ints = {
				0x66,0x84,0x70,0x9A,0xA4,0x86,0x60,0x66,0x84,0x70,0x9A,0x92,0xA4,0x61,0x03,0xF0,0x00,0x10,0x2B,0x2B,0x18,0x00,0x08,0x02,0xC1,0x44,0x01,0x7F,0x10,0x03,0x19,0x10,0x00,0x00,0x0B,0xA3,0xBF,0x00,0x00,0xD9,0x85,0x40,0xAA,0xAA,0x01,0x00,0x01,0x00,0x01,0x00,0x01,0x00,0x01,0x0C,0x01,0x0C,0x01,0x0C,0x01,0x0C,0x04,0x00,0x00,0x06,0xAA,0xB2,0x01,0x18,0x01,0x20,0x05,0xEC,0x00,0x06,0xAF,0x64,0x01,0x26,0x01,0x2C,0x06,0x64,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xE0,0x00,0x32,0xEF,0xFF,0xFF,0x90,0x30,0x50,0x00,0x33,0x82,0x50,0x00,0x00,0x00,0x00,0x00,0x33,0x8B,0xF0,0x00,0x00,0x00,0x00,0x00,0x33,0x92,0xB0,0x00,0x00,0x00,0x00,0x00,0x33,0xB8,0x3F,0xFF,0x50,0x31,0x00,0x00,0x33,0xDD,0xB0,0x00,0x70,0x33,0x60,0x00,0x34,0x03,0x30,0x00,0x50,0x33,0x10,0x00,0x34,0x28,0xB0,0x00,0x40,0x32,0xF0,0x00,0x34,0x4E,0x30,0x00,0x20,0x32,0xC0,0x00,0x34,0x73,0xBF,0xFE,0x00,0x2F,0x30,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x14,
		};
		int[] ints2 = {		
				0x66,0x84,0x70,0x9A,0xA4,0x86,0x60,0x66,0x84,0x70,0x9A,0x92,0xA4,0x61,0x03,0xF0,0x00,0x10,0x2C,0x2C,0x18,0x9C,0x03,0x02,0x58,0x2E,0xB7,0xAB,0x51,0x00,0x08,0x02,0x02,0x80,0x80,0x88,0x24,0x02,0x00,0x80,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x81,0x60,0x90,0x12,0x00,0x00,0x00,0x00,0x00,0x00,0x7F,0xE3,0xFF,0xE4,0x6A,0xB8,0x2E,0xB7,0xBA,0x81,0x41,0x20,0x24,0x01,0xDB,0xBF,0xFF,0x7F,0x59,0xFF,0x80,0x00,0x00,0x00,0x00,0x00,0x03,0x80,0x73,0xC9,0xFB,0xD1,0xFF,0xB2,0xBC,0xE7,0xE3,0x3F,0x74,0x7F,0xB5,0x55,0x7F,0x3B,0xF7,0x6C,0x59,0xB2,0xE7,0xDA,0xDF,0x3F,0xF6,0xAC,0x73,0x37,0x7B,0x7F,0x57,0x67,0xBD,0xFF,0x9B,0xF1,0x7B,0x05,0xB9,0x7F,0x6E,0xF9,0x47,0xFD,0x80,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x68,0xFB,0x00,0x00,0x00,0x00,0x80,0x00,0x00,0x00,0xC2,0x00,0xBA,0x00,0xBA,0x00,0x01,0x66,0xF0,0x00,0x00,0x00,0x18,0x18,0x6D,0xA8,0x07,0xFF,0x00,0x00,0x00,0x47,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
		};
		byte[] bytes = new byte[ints.length];
		for (int i=0; i< ints.length; i++)
			bytes[i] = (byte) (ints[i] & 0xff);
		byte[] bytes2 = new byte[ints2.length];
		for (int i=0; i< ints2.length; i++)
			bytes2[i] = (byte) (ints2[i] & 0xff);
		Ax25Frame frame = new Ax25Frame(bytes);
		System.out.println(frame);
		
		Ax25Frame frame2 = new Ax25Frame(bytes2);
		System.out.println(frame2);
		TlmMirSatFrame mirSatTlm = new TlmMirSatFrame(spacecraftSettings, frame);
		mirSatTlm.add2ndFrame(frame2);
		System.out.println(mirSatTlm);
	}

}