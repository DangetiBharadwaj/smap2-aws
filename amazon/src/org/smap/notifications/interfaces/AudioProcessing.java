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
	String defaultBucketName = "smap-rekognition";	// Used if file is not already in an S3 bucket
	AmazonS3 s3 = null;
	AmazonTranscribe transcribeClient = null;

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
		String bucketName;	// The bucket we end up using. It will be either the default bucket or the media bucket

		log.info("xxxxxxxxxxx Media Bucket: " + mediaBucket);
		
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
		} else if(ext.equals("amr")) {
			convert = true;
		}
		
		if(awsSupported || convert) {
			
			// Conversion
			if(convert) {
				if(mediaBucket != null) {
					// TODO get the file onto local disk to convert it
					String tempFileName = "/smap/temp/" + UUID.randomUUID().toString() + "." + ext;
					File tempFile = new File(tempFileName);
					s3.getObject(new GetObjectRequest(mediaBucket, filePath, null), tempFile);
				}
				// TODO convert the file and update file path to the converted file
			}
			
			// Put local files into remote default bucket
			if(convert || mediaBucket == null) {
				bucketName = defaultBucketName;
				File file = new File(basePath + filePath);				
				if(file.exists()) {				
					s3.putObject(new PutObjectRequest(bucketName, file.getName(), file));
				} else {
					return("Error: Audio File not found: " + file.getAbsolutePath());
				}
			} else {
				bucketName = mediaBucket;
			}
				
			// Generate the transcript	
			Media media=new Media().withMediaFileUri(s3.getUrl(bucketName, filePath).toString());
			StartTranscriptionJobRequest request = new StartTranscriptionJobRequest()
					.withMedia(media)
					.withLanguageCode("en-US")
					.withTranscriptionJobName(job);
				
			StartTranscriptionJobResult result = transcribeClient.startTranscriptionJob(request);
			String status = result.getTranscriptionJob().getTranscriptionJobStatus();
			log.info("Transcribe job status: " + status);			
			    
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
