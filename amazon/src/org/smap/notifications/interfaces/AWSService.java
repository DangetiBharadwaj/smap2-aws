package org.smap.notifications.interfaces;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

/*****************************************************************************
 * 
 * This file is part of SMAP.
 * Copyright Smap Pty Ltd
 * 
 ******************************************************************************/

/*
 * Base class for calling AWS services
 */
public abstract class AWSService {

	static Logger log = Logger.getLogger(AWSService.class.getName());

	Properties properties = new Properties();
	String tableName = null;
	String platformApplicationArn = null;
	String defaultBucketName;	// Used if file is not already in an S3 bucket
	String region;
	AmazonS3 s3 = null;
	

	public AWSService(String r) {
		
		if(r != null) {
			this.region = r;
		} else {
			this.region = "us-east-1";
		}
		
		// get properties file		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("/smap_bin/resources/properties/aws.properties");
			properties.load(fis);
			tableName = properties.getProperty("userDevices_table");
			platformApplicationArn = properties.getProperty("fieldTask_platform");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error reading properties", e);
		} finally {
			try {fis.close();} catch (Exception e) {}
		}
		
		defaultBucketName = "smap-ai-" + region;
		// create a new S3 client
		log.info("Getting s3 client for regions: " + region);
		s3 = AmazonS3Client.builder()
				.withRegion(region)
				.withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
				.build();
        
	}
	
	public String  setBucket(String mediaBucket, String serverFilePath, String filePath) {
		String bucketName = null;
		if(mediaBucket == null) {
			bucketName = defaultBucketName;
			File file = new File(serverFilePath);				
			if(file.exists()) {
				log.info("Using local file " + filePath + " to bucket " + bucketName);
				s3.putObject(new PutObjectRequest(bucketName, filePath, file));
			} else {
				return("Error: Media File not found: " + file.getAbsolutePath());
			}
		} else {
			bucketName = mediaBucket;
		}
		return bucketName;
	}
}
