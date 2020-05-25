package org.smap.notifications.interfaces;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClientBuilder;
import com.amazonaws.services.transcribe.model.GetMedicalTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetMedicalTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.Media;
import com.amazonaws.services.transcribe.model.MedicalTranscript;
import com.amazonaws.services.transcribe.model.MedicalTranscriptionJob;
import com.amazonaws.services.transcribe.model.Specialty;
import com.amazonaws.services.transcribe.model.StartMedicalTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.StartMedicalTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.Transcript;
import com.amazonaws.services.transcribe.model.TranscriptionJob;
import com.amazonaws.services.transcribe.model.Type;

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
public class S3 extends AWSService {

	AmazonTranscribe transcribeClient = null;
	AmazonS3URI s3uri = null;
	
	public S3(String r, String uri) {		
		super(r);
		s3uri = new AmazonS3URI(uri);
	}
	
	public String get() throws Exception {
		 
		StringBuilder sb = new StringBuilder();
		if(s3uri != null) {
			BufferedReader bufferedReader = null;
			try {
				
				bufferedReader = new BufferedReader(new InputStreamReader(s3.getObject(new GetObjectRequest(s3uri.getBucket(), s3uri.getKey())).getObjectContent()));
				
				String line;
				while ( (line = bufferedReader.readLine()) != null ) {
					sb.append(line);
				}
	
			} finally {
				if(bufferedReader != null) try{bufferedReader.close();} catch(Exception e) {}
			}
		} else {
			throw new Exception("S3 object not found");
		}
		return sb.toString();
	}
	
	public void rm() throws IOException {
		 
		s3.deleteObject(s3uri.getBucket(), s3uri.getKey());
		s3uri = null;
	}

}
