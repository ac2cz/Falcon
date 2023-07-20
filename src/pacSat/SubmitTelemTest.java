package pacSat;

import java.util.Date;

import org.junit.Test;

import ax25.Ax25Frame;
import ax25.KissFrame;
import pacSat.frames.FrameException;

class SubmitTelemTest {

	@Test
	void test() {
		KissFrame kiss = new KissFrame();
		int[] by = {0xC0,0x00,0x96,0x68,0x96,0x88,0xA4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x84,0xF0,0x00,0x06,0xC0};  // example I FRAME WITH DATA
		for (int b : by)
			kiss.add(b);
		try {
			Ax25Frame bf = new Ax25Frame(kiss);
			System.out.println(bf);
		} catch (FrameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(kiss.toByteString());
		Date now = new Date();
		SubmitTelem telem = new SubmitTelem("https://httpbin.org/post", 99718, "G0KLA", now, -73.5, 40.1);
		telem.setFrame(kiss);
		try {
			telem.send();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
