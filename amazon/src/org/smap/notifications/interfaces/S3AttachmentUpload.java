package org.smap.notifications.interfaces;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

/*
 * Static class to upload attachments to S3 
 * Fast (hopefully)
 */
public class S3AttachmentUpload {

	private S3AttachmentUpload() {
		
	}
	
	private static AmazonS3 s3;
	private static String bucket;
	private static String region;
	private static boolean s3Enabled = true;
	
	static Logger log = Logger.getLogger(AWSService.class.getName());
	
	public static void put(String basePath, String filePath) {
		
		if(s3Enabled) {
			/*
			 * Initialise first time through
			 */
			if(s3 == null) {
				
				bucket = getSettingFromFile(basePath + "/settings/bucket");
				if(bucket == null) {
					s3Enabled = false;
				} else {
					region = getSettingFromFile(basePath + "/settings/region");
					
					s3 = AmazonS3Client.builder()
							.withRegion(region)
							.withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
							.build();
				}
			}
			
			/*
			 * Send the file
			 */
			File file = new File(filePath);	
			String s3Path = filePath.substring(basePath.length() + 1);
			if(file.exists()) {
				log.info("Using local file " + filePath + " to bucket " + bucket + " in region " + region);
				s3.putObject(new PutObjectRequest(bucket, s3Path, file));
			} else {
				log.info("Error uploading to S3: File not found: " + file.getAbsolutePath());
			}
		}
		
	}
	
	private static String getSettingFromFile(String filePath) {
		
		String setting = null;
		try {
			List<String> lines = Files.readAllLines(new File(filePath).toPath());
			if(lines.size() > 0) {
				setting = lines.get(0);
			}
		} catch (Exception e) {

		}

		return setting;
	}
}
