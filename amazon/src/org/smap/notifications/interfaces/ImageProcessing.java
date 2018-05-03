package org.smap.notifications.interfaces;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import tools.AmazonSNSClientWrapper;

/*****************************************************************************
 * 
 * This file is part of SMAP.
 * Copyright Smap Pty Ltd
 * 
 ******************************************************************************/

/*
 * Manage access to the Dynamo table that holds the connection between user and
 * device
 */
public class ImageProcessing {

	private static Logger log = Logger.getLogger(ImageProcessing.class.getName());

    private AmazonSNSClientWrapper snsClientWrapper;
	Properties properties = new Properties();
	String tableName = null;
	String region = null;
	String platformApplicationArn = null;
	String bucketName = "smap-rekognition";
	AmazonS3 s3 = null;

	public ImageProcessing() {
		
		// get properties file
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("/smap_bin/resources/properties/aws.properties");
			properties.load(fis);
			tableName = properties.getProperty("userDevices_table");
			region = properties.getProperty("userDevices_region");
			platformApplicationArn = properties.getProperty("fieldTask_platform");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error reading properties", e);
		} finally {
			try {fis.close();} catch (Exception e) {}
		}
		
		//create a new S3 client
		s3 = AmazonS3Client.builder()
				.withRegion(region)
				.withCredentials(new ProfileCredentialsProvider())
				.build();
	}

	/*
	 * Get labels
	 */
	public String getLabels(String server, String user, String path, String format) {
		
		StringBuffer labels = new StringBuffer("");
		// For testing on local host - can leave in final code
		if(server.equals("smap")) {
			server = "dev.smap.com.au";
		}
		
		File file = new File(path);
		
		if(file.exists()) {
			s3.putObject(new PutObjectRequest(bucketName, file.getName(), file));
			
			S3Object s3Object = new S3Object();
			s3Object.setBucket(bucketName);
			s3Object.setName(file.getName());
			Image image = new Image().withS3Object(s3Object);
			DetectLabelsRequest request = new DetectLabelsRequest();
			request.withImage(image)
	                .withMaxLabels(10)
	                .withMinConfidence(80F);
			
			AmazonRekognition rekognitionClient = AmazonRekognitionClient.builder()
					.withRegion(region)
					.withCredentials(new ProfileCredentialsProvider())
					.build();
			
			DetectLabelsResult result = rekognitionClient.detectLabels(request);
			
			if(format.equals("params")) {
				labels.append(result.toString());
			} else {
				for(Label l : result.getLabels()) {
					if(labels.length() > 0) {
						labels.append(", ");
					}
					labels.append(l.getName());
				}
			}
			
		} else {
			labels.append("Error: image not found");
		}
		// Upload the file to the image recognition bucket if it is not already there
		 
         // Get the labels
	
		// Apply results
		
		return labels.toString();
		
	}

}
