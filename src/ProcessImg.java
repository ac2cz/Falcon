import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import fileStore.XcamImg;
import gui.ImagePanel;

public class ProcessImg {

	public static void main(String[] args) throws IOException {
		
//		XcamImg img = new XcamImg("C:\\Users\\chris\\Desktop\\LIVE\\PACSAT_MIR_SAT_1\\Mir-Sat-1\\0-010ED41F.img");
		XcamImg img = new XcamImg("C:\\Users\\chris\\Desktop\\LIVE\\PACSAT_MIR_SAT_1\\Mir-Sat-1\\1-010ED41F.img");
		
		JFrame win = new JFrame();
		win.setBounds(100, 100, 400, 300);
		win.setTitle("AMSAT Image Toolkit");
		win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		win.getContentPane().setLayout(new BorderLayout());
		ImagePanel pic = new ImagePanel();
		
		win.getContentPane().add(pic, BorderLayout.CENTER);

		pic.allowStretching(true);
		BufferedImage i = img.getRotatedImage();
		pic.setBufferedImage(i);

		img.export("C:\\Users\\chris\\Desktop\\LIVE\\PACSAT_MIR_SAT_1\\Mir-Sat-1\\1-010ED41F.raw");
		win.setVisible(true);
		
		img.saveAsPng("C:\\Users\\chris\\Desktop\\LIVE\\PACSAT_MIR_SAT_1\\Mir-Sat-1\\1-010ED41F.png");
	}
	
	class PicPanel extends JPanel {
		Image img;
		
		PicPanel() {
			
		}
		
		void setImage(Image i) {
			img = i;
		}
		
		public void paint (Graphics g) {
	        g.drawImage (img, 0, 0, this);
	    }
	}

}
