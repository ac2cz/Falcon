package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import common.Config;
import common.DesktopApi;
import common.Log;

/**
 * 
 * Pacsat Ground
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
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
 */
@SuppressWarnings("serial")
public class HelpAbout extends JDialog implements ActionListener {

	private JPanel contentPane;
	private final String AMSAT = "http://www.amsat.org";
	private final String FOX = "http://ww2.amsat.org/?page_id=1113";
	public final static String MANUAL = "pacsat_ground_manual.pdf";
	public final static String LEADERBOARD = "http://www.amsat.org/tlm/";
	public final static String SOFTWARE = "http://www.g0kla.com/pacsat";
	JButton btnClose;
	
	/**
	 * Create the frame.
	 */
	public HelpAbout(JFrame owner, boolean modal) {
		super(owner, modal);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(MainWindow.frame.getBounds().x+25, MainWindow.frame.getBounds().y+25, 500, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel northPanel = new JPanel();
		panel.add(northPanel, BorderLayout.NORTH);
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));
		
		JPanel northApanel = new JPanel();
		JPanel northBpanel = new JPanel();
		northPanel.add(northApanel);
		northPanel.add(northBpanel);
		northApanel.setLayout(new BoxLayout(northApanel, BoxLayout.Y_AXIS));
		
		JPanel eastPanel = new JPanel();
		panel.add(eastPanel, BorderLayout.EAST);
		eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));

//		JPanel centerPanelWrapper = new JPanel();
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
//		panel.add(centerPanel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		panel.add(southPanel, BorderLayout.SOUTH);

		JLabel lblAmsatFoxaTelemetry = new JLabel("<html><h2>AMSAT PacSat Ground Station</h2></html>");
		lblAmsatFoxaTelemetry.setForeground(Color.BLUE);
		northApanel.add(lblAmsatFoxaTelemetry);

		addLine("Version " + Config.VERSION, northApanel);
		
		
//		addLine("<html>Written by <b>Chris Thompson G0KLA/AC2CZ</b><br><br></html>", northApanel);
		addUrl("Written by ", "www.g0kla.com", "<b>Chris Thompson</b>", " G0KLA / AC2CZ", northApanel);
		addUrl("You can browse ", MANUAL, "the manual", " for help", northApanel);
		
		addUrl("Please consider ", AMSAT, "donating", " to this and future AMSAT missions", northApanel);
		addLine(" ", northApanel);
		addUrl("\nThis program is distributed in the hope that it will be useful, "
				+ "but WITHOUT ANY WARRANTY; without even the implied warranty of "
				+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the ",
				"http://www.gnu.org/licenses/gpl-3.0.en.html", "<b>GNU General Public License</b>", " for more details. ", northApanel);
		addLine(" ", northApanel);
		addLine("This software also includes:", northApanel);
		addUrl("- Java Predict Port by ", "https://github.com/badgersoftdotcom/predict4java", "<b>G4DPZ</b>", ", released under GPL", northApanel);
		addUrl("- Predict is by", "http://www.qsl.net/kd2bd/predict.html", "<b>KD2BD</b>", ", released under GPL", northApanel);
		
		JScrollPane scrollPane = new JScrollPane (centerPanel, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//centerPanelWrapper.add(scrollPane);
		panel.add(scrollPane, BorderLayout.CENTER);
		
		addLine("<html><br><b><u>Pacsat</b></u></html>", centerPanel);
		addLine("<html><table style='mso-cellspacing: 0in' cellspacing='0' cellpadding='2' >"
				+"<tr><tbody><td><b>tbc AB0CD</b></td><td>Description</td>"
				+ "</tbody></table></html>", centerPanel);
		
		BufferedImage wPic = null;
		try {
			wPic = ImageIO.read(this.getClass().getResource("/images/pacsat_sm.jpg"));
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}
		if (wPic != null) {
			JLabel wIcon = new JLabel(new ImageIcon(wPic));
			northBpanel.add(wIcon);
		}
		btnClose = new JButton("Close");
		btnClose.addActionListener(this);
		southPanel.add(btnClose);

	}
	 
	private void addUrl(String pre, final String url, String text, String post, JPanel panel) {
		JLabel website = new JLabel();
		//website.setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
		website.setForeground(Color.BLACK);
		panel.add(website);

	        website.setText("<html>"+pre+"<a href=\"\">"+text+"</a>"+post+"</html>");
	        website.setCursor(new Cursor(Cursor.HAND_CURSOR));
	        website.addMouseListener(new MouseAdapter() {
	            @Override
	            public void mouseClicked(MouseEvent e) {
	                    try {
	                            DesktopApi.browse(new URI(url));
	                    } catch (URISyntaxException ex) {
	                            //It looks like there's a problem
	                    	ex.printStackTrace();
	                    }
	            }
	        });
	    }
	private void addLine(String text, JPanel panel) {
		JLabel lblVersion = new JLabel(text);
		//lblVersion.setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
		lblVersion.setForeground(Color.BLACK);
		panel.add(lblVersion);

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {		
		if (e.getSource() == btnClose) {
			this.dispose();
		}

		
	}

}
