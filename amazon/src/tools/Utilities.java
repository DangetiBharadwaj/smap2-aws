package tools;

import java.util.logging.Logger;

import org.smap.notifications.interfaces.AudioProcessing;

public class Utilities {
	
	private static Logger log = Logger.getLogger(AudioProcessing.class.getName());
	
	/*
	 * Create media to another format
	 */
	public static boolean convertMedia(String in, String out) {

		boolean status = false;
		
		String cmd = "/smap_bin/convertMedia.sh " + in + " " + out
				+ " >> /var/log/subscribers/convert.log 2>&1";
		log.info("Exec: " + cmd);
		try {

			Process proc = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", cmd });

			int code = proc.waitFor();
			log.info("Convrt media processing finished with status:" + code);
			if (code != 0) {
				log.info("Error: Convert processing failed");
			} else {
				status = true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return status;
	}

}
