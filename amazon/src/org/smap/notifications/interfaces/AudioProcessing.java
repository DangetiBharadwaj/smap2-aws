package org.smap.notifications.interfaces;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.ResponseMetadata;
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
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClientBuilder;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.Media;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.Transcript;
import com.amazonaws.services.transcribe.model.TranscriptionJob;

import tools.AmazonSNSClientWrapper;
import tools.Utilities;

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
	String platformApplicationArn = null;
	String defaultBucketName;	// Used if file is not already in an S3 bucket
	AmazonS3 s3 = null;
	AmazonTranscribe transcribeClient = null;

	public AudioProcessing(String region) {
		
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
		s3 = AmazonS3Client.builder()
				.withRegion(region)
				.withCredentials(new ProfileCredentialsProvider())
				.build();
		
		// create a new transcribe client
		ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setConnectionTimeout(60000);
        clientConfig.setMaxConnections(100);
        clientConfig.setSocketTimeout(60000);
        
		transcribeClient = AmazonTranscribeClientBuilder.standard()
				.withCredentials(new ProfileCredentialsProvider())
				.withRegion(region)
				.withClientConfiguration(clientConfig)
				.build();
	}

	/*
	 * Submit an audio job
	 */
	public String submitJob(ResourceBundle localisation, 
			String server, 
			String basePath, 
			String filePath, 
			String format, 
			String job,
			String mediaBucket) {
		
		StringBuffer response = new StringBuffer("");
		boolean awsSupported = false;
		boolean convert = false;
		String ext = "";
		String serverFilePath = null;
		String bucketName;	// The bucket we end up using. It will be either the default bucket or the media bucket
		
		// For testing on local host - can leave in final code
		if(server.equals("smap")) {
			server = "dev.smap.com.au";
		}
		
		// Check to see if the file type is supported
		int idx = filePath.lastIndexOf('.');
		if(idx > 0) {
			ext = filePath.substring(idx+1).toLowerCase();
		}
		if(ext.equals("mp3") || ext.equals("mp4") || ext.equals("wav")) {
			awsSupported = true;
			log.info("Extension " + ext + " is nativly supported by AWS");
		} else if(ext.equals("amr")) {
			log.info("Extension " + ext + " will be converted to mp3");
			convert = true;
		}
		
		if(awsSupported || convert) {
			
			// Conversion
			if(convert) {
				String tempFilePath = null;
				String convertedTempFilePath = null;
				if(mediaBucket != null) {
					// Get the file onto local disk to convert it
					String fBase = "/smap/temp/" + UUID.randomUUID().toString() + ".";
					tempFilePath = fBase + ext;
					convertedTempFilePath = fBase + "mp3";
					
					File tempFile = new File(tempFilePath);
					log.info("Getting media file from s3 bucket: " + mediaBucket + " to : " + tempFilePath);
					s3.getObject(new GetObjectRequest(mediaBucket, filePath, null), tempFile);
				} else {
					tempFilePath = basePath + filePath;
					convertedTempFilePath = "/smap/temp/" + UUID.randomUUID().toString() + ".mp3";					
				}
				
				// Convert the file and update file path to the converted file
				log.info("Converting: " + tempFilePath + " to " + convertedTempFilePath);
				if(!Utilities.convertMedia(tempFilePath, convertedTempFilePath)) {
					String msg = localisation.getString("aws_t_conv_erro");
					msg = msg.replace("%s1", tempFilePath);
					return(msg);
				}
				serverFilePath = convertedTempFilePath;
			} else if(mediaBucket == null) {
				serverFilePath = basePath + filePath;
			}
			
			// Put local files into remote default bucket
			if(convert || mediaBucket == null) {
				bucketName = defaultBucketName;
				File file = new File(serverFilePath);				
				if(file.exists()) {
					log.info("Using local file " + filePath + " to bucket " + bucketName);
					s3.putObject(new PutObjectRequest(bucketName, filePath, file));
				} else {
					return("Error: Audio File not found: " + file.getAbsolutePath());
				}
			} else {
				bucketName = mediaBucket;
			}
				
			// Generate the transcript
			String status = null;
			try {
				log.info("Generating transcript for file: " + bucketName + filePath);
				Media media=new Media().withMediaFileUri(s3.getUrl(bucketName, filePath).toString());
				StartTranscriptionJobRequest request = new StartTranscriptionJobRequest()
						.withMedia(media)
						.withLanguageCode("en-US")
						.withTranscriptionJobName(job);
					
				StartTranscriptionJobResult result = transcribeClient.startTranscriptionJob(request);
				status = result.getTranscriptionJob().getTranscriptionJobStatus();
				log.info("Transcribe job status: " + status);	
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				return e.getMessage();			
			}
			    
			response.append(status);
			
		} else {
			String msg = localisation.getString("aws_t_ns");
			msg = msg.replace("%s1", ext);
			response.append(msg);
		}

		
		return response.toString();
		
	}
	
	/*
	 * Get the transcript
	 */
	public String getTranscriptUri(String job) {
		String uri = null;
		
		GetTranscriptionJobRequest request = new GetTranscriptionJobRequest()
				.withTranscriptionJobName(job);		
		GetTranscriptionJobResult result = transcribeClient.getTranscriptionJob(request);
		if(result != null) {
			TranscriptionJob tj = result.getTranscriptionJob();
			String status = tj.getTranscriptionJobStatus();
			
			if(status != null && status.equals("COMPLETED")) {
				Transcript t = tj.getTranscript();
				uri = t.getTranscriptFileUri();
			}
			
		}
	
		return uri;
	}

}
