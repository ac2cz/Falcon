package gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Amsat PacSat Ground
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * A simple panel with an image on it, that changes size when the panel is resized.
 * 
 *
 */
@SuppressWarnings("serial")
public class ImagePanel extends JPanel{

    protected BufferedImage image;
    boolean allowStretching = false;

    public ImagePanel() {
    	super();
    }
    
    public ImagePanel(String filePath) {
       setImage(filePath);
    }

    public void setImage( String filePath) {
    	try {                
            image = ImageIO.read(new File(filePath));
         } catch (IOException ex) {
              // handle exception...
         }
    }

    public void setBufferedImage( BufferedImage img) {
    	image = img;
    	this.repaint();
    }
    
    public void setBufferedImage( byte[] b) {
    	image = createImageFromBytes(b);
    	this.repaint();
    }

    public void allowStretching(boolean stretch) { allowStretching = stretch; }
    
    @Override
    protected void paintComponent(Graphics g) {
    	super.paintComponent(g);
    	if (image != null) {
    		BufferedImage display = image;
    		if (allowStretching || (this.getHeight() < image.getHeight() || this.getWidth() < image.getWidth())) {
    			double ratio = (double)this.getHeight()/(double)image.getHeight();
    			if (image.getWidth() * ratio > this.getWidth())
    				ratio = (double)this.getWidth()/(double)image.getWidth();
    			display = scale(image, ratio);
    		}
    		g.drawImage(display, 0, 0, null); // see javadoc for more info on the parameters
    	}
    }
    
    private BufferedImage createImageFromBytes(byte[] imageData) {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        try {
            return ImageIO.read(bais);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
	/**
	 * Utility function to scale an image
	 * @param source
	 * @param ratio
	 * @return
	 */
	public static BufferedImage scale(BufferedImage source,double ratio) {
		  int w = (int) (source.getWidth() * ratio);
		  int h = (int) (source.getHeight() * ratio);
		  BufferedImage bi = getCompatibleImage(w, h);
		  Graphics2D g2d = bi.createGraphics();
		  double xScale = (double) w / source.getWidth();
		  double yScale = (double) h / source.getHeight();
		  AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
		  g2d.drawRenderedImage(source, at);
		  g2d.dispose();
		  return bi;
		}

		private static BufferedImage getCompatibleImage(int w, int h) {
		  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		  GraphicsDevice gd = ge.getDefaultScreenDevice();
		  GraphicsConfiguration gc = gd.getDefaultConfiguration();
		  BufferedImage image = gc.createCompatibleImage(w, h);
		  return image;
		}

}