package org.smap.notifications.interfaces;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.quicksight.AmazonQuickSight;
import com.amazonaws.services.quicksight.AmazonQuickSightClientBuilder;
import com.amazonaws.services.quicksight.model.GetDashboardEmbedUrlRequest;
import com.amazonaws.services.quicksight.model.GetDashboardEmbedUrlResult;
import com.amazonaws.services.quicksight.model.IdentityType;

/*****************************************************************************
 * 
 * This file is part of SMAP.
 * Copyright Smap Consulting Pty Ltd
 * 
 ******************************************************************************/

/*
 * Manage access to AWS transcribe service
 */
public class QuickSight extends AWSService {

	AmazonQuickSight quicksightClient = null;

	public QuickSight(String r) {
		
		super(r);
		
		// create a new transcribe client
		ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setConnectionTimeout(60000);
        clientConfig.setMaxConnections(100);
        clientConfig.setSocketTimeout(60000);
        
        
		quicksightClient = AmazonQuickSightClientBuilder.standard()
				.withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
				.withRegion(region)
				.withClientConfiguration(clientConfig)
				.build();
	}
	
	public String getDashboardUrl() {
		
		final String dashboardId = "3c0205d9-c84c-49bd-8112-20e81c16f619";
		final String awsAccountId = "439804189189";
		
		final GetDashboardEmbedUrlResult dashboardEmbedUrlResult =
				quicksightClient.getDashboardEmbedUrl(new GetDashboardEmbedUrlRequest()
		            .withDashboardId(dashboardId)
		            .withAwsAccountId(awsAccountId)
		            .withIdentityType(IdentityType.IAM)
		            .withResetDisabled(true)
		            .withSessionLifetimeInMinutes(100l)
		            .withUndoRedoDisabled(false));
		
		return dashboardEmbedUrlResult.getEmbedUrl();
	}

}
