package org.smap.notifications.interfaces;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
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
 * Manage access to AWS Translate service
 */
public class TextProcessing extends AWSService {

	public TextProcessing(String region) {
		super(region);	
	}

	/*
	 * Get labels
	 */
	public String getTranslatian(
			String source,
			String sourceLanguage,
			String targetLanguage) {
			
		AmazonTranslate translate = AmazonTranslateClient.builder()
                .withCredentials(new ProfileCredentialsProvider())
                .withRegion(region)
                .build();
			
		String in = encodePlaceHolders(source);	// Add do not translates
		
		TranslateTextRequest request = new TranslateTextRequest()
                .withText(in)
                .withSourceLanguageCode(sourceLanguage)
                .withTargetLanguageCode(targetLanguage);
        TranslateTextResult result  = translate.translateText(request);
		
        String out = result.getTranslatedText();
		out = decodePlaceHolders(out);
		log.info("Translate to: " + out);
		return out;
		
	}
	
	// Mark placeholders as do not translate
	private String encodePlaceHolders(String in) {
		return in.replace("${", "#${#");  
	}
	
	// Decode placeholders as do not translate
	private String decodePlaceHolders(String in) {
		return in.replace("{ #", "{#")
				.replace("$ {", "${")
				.replace("# $", "#$")
				.replace("#${#", "${");
	}

}
