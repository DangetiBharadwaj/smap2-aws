package org.smap.notifications.interfaces;

import java.util.logging.Logger;



/*****************************************************************************

This file is part of SMAP.

Copyright Smap Pty Ltd

 ******************************************************************************/

/*
 * Manage the table that stores details on the forwarding of data onto other systems
 */
public abstract class EmitSMS {
	
	protected static Logger log =
			 Logger.getLogger(EmitSMS.class.getName());
	
	public abstract String sendSMS(  
			String number, 
			String content) throws Exception;
	
	/*
	 * Validate an email
	 */
	protected boolean isValidPhoneNumber(String number) {
		boolean isValid = true;
		if(number == null) {
			isValid = false;
		} else if(number.trim().length() == 0) {
			isValid = false;
		}
		return isValid;
	}

}


