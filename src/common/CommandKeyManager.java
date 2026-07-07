package common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 *
 * Amsat PacSat Ground
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2026 amsat.org
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
 * Loads a 32 byte command key from a password protected PKCS12 (.p12)
 * container.  The container holds a single SecretKeyEntry; which key file to
 * use is entirely the operator's choice via the spacecraft settings.  Loaded
 * keys are cached for the life of the process, so the passphrase is asked
 * for at most once per key file per session.
 *
 * Shows modal Swing dialogs; call getKey() from the EDT only.
 */
public final class CommandKeyManager {

	private static final int KEY_LEN = 32;

	/** raw key bytes, keyed by .p12 path; lives until process exit */
	private static final Map<String, byte[]> sessionKeys = new HashMap<String, byte[]>();
	/** what the container says it holds, for display only; never gated against */
	private static final Map<String, String> sessionLabels = new HashMap<String, String>();

	private CommandKeyManager() {}

	/** True if the key for this container path is already cached this session. */
	public static synchronized boolean isLoaded(String p12Path) {
		return p12Path != null && sessionKeys.containsKey(p12Path);
	}

	/**
	 * Display label for a loaded key, taken from the container's manifest
	 * (its alias, e.g. "astropi key 0").  Informational only - the operator
	 * judges whether it is the right key; nothing is enforced against it.
	 * Returns null if this path has no key loaded.
	 */
	public static synchronized String loadedLabel(String p12Path) {
		return sessionLabels.get(p12Path);
	}

	/**
	 * Return the raw 32 byte command key from the given .p12 file, prompting
	 * for the passphrase if it is not already cached this session.
	 *
	 * Returns null if the operator cancels or the key cannot be loaded; a
	 * dialog explaining why has already been shown in that case.
	 */
	public static synchronized byte[] getKey(String p12Path, Component parent) {
		byte[] cached = (p12Path == null) ? null : sessionKeys.get(p12Path);
		if (cached != null) return cached;

		if (p12Path == null || p12Path.trim().length() == 0) {
			JOptionPane.showMessageDialog(parent,
					"No command key file is configured.\n"
					+ "Set the path to the .p12 file in the spacecraft settings.",
					"Command Key", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		while (true) {
			// Removable media is a normal case, so a missing file offers Retry
			File f = new File(p12Path);
			if (!f.isFile()) {
				Object[] options = { "Retry", "Cancel" };
				int n = JOptionPane.showOptionDialog(parent,
						"Command key file not found:\n" + p12Path + "\n\n"
						+ "If your key is on removable media, insert it and press Retry.\n"
						+ "Otherwise correct the path in the spacecraft settings.",
						"Command Key",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[0]);
				if (n == JOptionPane.OK_OPTION) continue;
				return null;
			}

			char[] password = promptPassword(parent, f.getName());
			if (password == null) return null; // cancelled

			try {
				KeyStore ks = KeyStore.getInstance("PKCS12");
				try {
					FileInputStream fis = new FileInputStream(f);
					try {
						ks.load(fis, password);
					} finally {
						fis.close();
					}
				} catch (IOException e) {
					// Wrong passphrase and a corrupted container both land here
					// (the PKCS12 integrity MAC fails).  Re-prompt.
					JOptionPane.showMessageDialog(parent,
							"Incorrect passphrase (or the file is corrupt).\nTry again.",
							"Command Key", JOptionPane.ERROR_MESSAGE);
					continue;
				}

				// take the single key entry, whatever its alias
				Enumeration<String> aliases = ks.aliases();
				if (!aliases.hasMoreElements()) {
					JOptionPane.showMessageDialog(parent,
							"This file contains no key:\n" + p12Path,
							"Command Key", JOptionPane.ERROR_MESSAGE);
					return null;
				}
				String alias = aliases.nextElement();

				SecretKey key = (SecretKey) ks.getKey(alias, password);
				byte[] raw = (key == null) ? null : key.getEncoded();
				if (raw == null || raw.length != KEY_LEN) {
					if (raw != null) Arrays.fill(raw, (byte) 0);
					JOptionPane.showMessageDialog(parent,
							"This file does not contain a valid " + KEY_LEN
							+ " byte command key:\n" + p12Path,
							"Command Key", JOptionPane.ERROR_MESSAGE);
					return null;
				}

				String label = labelFromAlias(alias);
				sessionKeys.put(p12Path, raw);
				sessionLabels.put(p12Path, label);
				Log.println("Loaded command key from " + f.getName() + " (" + label + ")");
				return raw;

			} catch (GeneralSecurityException e) {
				JOptionPane.showMessageDialog(parent,
						"Could not read the key file:\n" + e.toString(),
						"Command Key", JOptionPane.ERROR_MESSAGE);
				return null;
			} finally {
				Arrays.fill(password, '\0');
			}
		}
	}

	/**
	 * Turn a container alias into a human readable label.  Containers minted
	 * by IorsKeyProvision use {sat}-cmd-key-{index}, which reads back as
	 * "astropi key 0".  Anything else is shown as-is.
	 */
	private static String labelFromAlias(String alias) {
		int sep = alias.indexOf("-cmd-key-");
		if (sep > 0) {
			String sat = alias.substring(0, sep);
			String idx = alias.substring(sep + "-cmd-key-".length());
			return sat + " key " + idx;
		}
		return alias;
	}

	/** Modal passphrase prompt.  Returns null on cancel. */
	private static char[] promptPassword(Component parent, String fileName) {
		JPasswordField pf = new JPasswordField(24);
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.add(new JLabel("Passphrase for command key file " + fileName + ":"),
				BorderLayout.NORTH);
		panel.add(pf, BorderLayout.CENTER);

		JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = pane.createDialog(parent, "Command Key");
		// JOptionPane hands focus to its default button after showing; take it
		// back for the password field once the window actually has focus
		dialog.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				pf.requestFocusInWindow();
			}
		});
		dialog.setVisible(true);
		dialog.dispose();

		Object result = pane.getValue();
		if (result == null || !result.equals(Integer.valueOf(JOptionPane.OK_OPTION)))
			return null;
		return pf.getPassword();
	}
}