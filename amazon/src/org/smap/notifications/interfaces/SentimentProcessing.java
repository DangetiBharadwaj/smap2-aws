package org.smap.notifications.interfaces;

import java.util.ArrayList;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClient;
import com.amazonaws.services.comprehend.model.DetectSentimentRequest;
import com.amazonaws.services.comprehend.model.DetectSentimentResult;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;

/*****************************************************************************
 * 
 * This file is part of SMAP.
 * Copyright Smap Consulting Pty Ltd
 * 
 ******************************************************************************/

/*
 * Manage access to AWS Comprehend service
 */
public class SentimentProcessing extends AWSService {
	
	public SentimentProcessing(String region, String basePath) {
		super(region, basePath);	
	}

	/*
	 * Get the Sentiment
	 */
	public String getSentiment(
			String source,
			String language) throws Exception {
			
		AmazonComprehend comprehend = AmazonComprehendClient.builder()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(region)
                .build();
		

		DetectSentimentRequest request = new DetectSentimentRequest()
				.withText(source)
				.withLanguageCode(language);
		DetectSentimentResult result  = comprehend.detectSentiment(request);
			
		String sentiment = result.getSentiment();
        
		log.info("Sentiment: " + sentiment);
		return sentiment;
		
	}
}
