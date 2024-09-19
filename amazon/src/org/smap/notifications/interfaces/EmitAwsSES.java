package org.smap.notifications.interfaces;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

/*****************************************************************************

This file is part of SMAP.

Copyright Smap Consulting Pty Ltd

 ******************************************************************************/

/*
 * Manage the table that stores details on the forwarding of data onto other systems
 */
public class EmitAwsSES {
	
	/*
	 * Events
	 */
	public static int AWS_REGISTER_ORGANISATION = 0;
	
	private static Logger log =
			 Logger.getLogger(EmitAwsSES.class.getName());
	
	Properties properties = new Properties();
	
	public EmitAwsSES(String basePath) {
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(basePath + "_bin/resources/properties/aws.properties");
			properties.load(fis);
		}
		catch (Exception e) { 
			log.log(Level.SEVERE, "Error reading properties", e);
		} finally {
			try {fis.close();} catch (Exception e) {}
		}
	}
	
	// Send an sms
	public String sendSES() throws Exception  {
		
		String responseBody = null;
		ArrayList<String> recipients = new ArrayList<>();
		recipients.add("neilpenman@gmail.com");
		
		//create a new SNS client
		AmazonSimpleEmailService client = AmazonSimpleEmailServiceClient.builder()
				.withRegion("ap-southeast-2")
				.withCredentials(new DefaultAWSCredentialsProviderChain())
				.build();	
	
	       // The HTML body of the email.
        String bodyHTML = "<h1>Hello!</h1>"
                + "<p> See the list of customers.</p>";

        try {
            send(client, "auto@server.smap.com.au", recipients, "Hi there friend", bodyHTML);
            System.out.println("Done");

        } catch (Exception e) {
            e.getStackTrace();
        }
		
		return responseBody;
	}
	
	public static void send(AmazonSimpleEmailService client,
            String sender,
            Collection<String> recipients,
            String subject,
            String bodyHTML) throws Exception {

        Destination destination = new Destination();
        destination.setToAddresses(recipients);

        Content content = new Content(bodyHTML);

        Content sub = new Content(subject);

        Body body = new Body(content);

        Message msg = new Message();
        msg.setSubject(sub);
        msg.setBody(body);


        SendEmailRequest emailRequest = new SendEmailRequest();
        emailRequest.setDestination(destination);
        emailRequest.setMessage(msg);
        emailRequest.setSource(sender);

        try {
            System.out.println("Attempting to send an email through Amazon SES " 
            		+ "using the AWS SDK for Java...");
            client.sendEmail(emailRequest);

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

}


