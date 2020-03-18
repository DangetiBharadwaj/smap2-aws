package org.smap.notifications.interfaces;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
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
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClientBuilder;
import com.amazonaws.services.transcribe.model.Media;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobResult;

import tools.AmazonSNSClientWrapper;

/*****************************************************************************
 * 
 * This file is part of SMAP.
 * Copyright Smap Pty Ltd
 * 
 ******************************************************************************/

/*
 * Manage access to AWS transcribe service
 */
public class AudioProcessing {

	private static Logger log = Logger.getLogger(AudioProcessing.class.getName());

	Properties properties = new Properties();
	String tableName = null;
	String region = null;
	String platformApplicationArn = null;
	String bucketName = "smap-rekognition";
	AmazonS3 s3 = null;

	public AudioProcessing() {
		
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
	public String getTranscript(String server, String user, String path, String format) {
		
		StringBuffer transcript = new StringBuffer("");
		// For testing on local host - can leave in final code
		if(server.equals("smap")) {
			server = "dev.smap.com.au";
		}
		
		File file = new File(path);
		
		if(file.exists()) {
			PutObjectResult x = s3.putObject(new PutObjectRequest(bucketName, file.getName(), file));
			
			S3Object s3Object = new S3Object();
			s3Object.setBucket(bucketName);
			s3Object.setName(file.getName());
			
			ClientConfiguration clientConfig = new ClientConfiguration();
	        clientConfig.setConnectionTimeout(60000);
	        clientConfig.setMaxConnections(100);
	        clientConfig.setSocketTimeout(60000);
	        
			AmazonTranscribe transcribeClient = AmazonTranscribeClientBuilder.standard()
					.withCredentials(new ProfileCredentialsProvider())
					.withRegion(region)
					.withClientConfiguration(clientConfig)
					.build();
			
			Media media=new Media().withMediaFileUri(s3.getUrl(bucketName, file.getName()).toString());
			StartTranscriptionJobRequest request = new StartTranscriptionJobRequest().withMedia(media);
		    StartTranscriptionJobResult result = transcribeClient.startTranscriptionJob(request);
		    log.info("Transcribe job status: " + result.getTranscriptionJob().getTranscriptionJobStatus());
			
			transcript.append(result.toString());
			
			
		} else {
			transcript.append("Error: audio not found");
		}
		
		return transcript.toString();
		
	}

}
