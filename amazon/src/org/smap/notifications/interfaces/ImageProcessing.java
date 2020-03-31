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
 * Copyright Smap Consulting Pty Ltd
 * 
 ******************************************************************************/

/*
 * Manage access to AWS Rekognition service
 */
public class ImageProcessing extends AWSService {

	public ImageProcessing(String region) {
		super(region);	
	}

	/*
	 * Get labels
	 */
	public String getLabels(
			String basePath,
			String fileIdentifier, 	// How the file is identified in the bucket
			String format, 
			String mediaBucket) {
		
		String serverFilePath = basePath + fileIdentifier;
		StringBuffer labels = new StringBuffer("");
		
		// Put local files into remote default bucket			
		String bucketName = setBucket(mediaBucket, serverFilePath, fileIdentifier);	
			
		S3Object s3Object = new S3Object();
		s3Object.setBucket(bucketName);
		s3Object.setName(fileIdentifier);
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
		
		return labels.toString();
		
	}

}
