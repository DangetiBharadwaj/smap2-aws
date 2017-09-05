package org.smap.notifications.interfaces;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;

import model.DeviceTable;
import tools.AmazonSNSClientWrapper;
import tools.SampleMessageGenerator.Platform;

/*****************************************************************************
 * 
 * This file is part of SMAP.
 * 
 * SMAP is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SMAP is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * SMAP. If not, see <http://www.gnu.org/licenses/>.
 * 
 ******************************************************************************/

/*
 * Manage access to the Dynamo table that holds the connection between user and
 * device
 */
public class EmitDeviceNotification {

	private static Logger log = Logger.getLogger(EmitDeviceNotification.class.getName());

    private AmazonSNSClientWrapper snsClientWrapper;
	Properties properties = new Properties();
	String tableName = null;
	String region = null;
	String platformApplicationArn = null;
	AmazonSNS sns = null;

	public EmitDeviceNotification() {
		
		// get properties file
		try {
			properties.load(new FileInputStream("/smap_bin/resources/properties/aws.properties"));
			tableName = properties.getProperty("userDevices_table");
			region = properties.getProperty("userDevices_region");
			platformApplicationArn = properties.getProperty("fieldTask_platform");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error reading properties", e);
		}
		
		//create a new SNS client
		sns = AmazonSNSClient.builder()
				.withRegion(region)
				.withCredentials(new ProfileCredentialsProvider())
				.build();
	}

	/*
	 * Send a message to users registered with a server, name combo
	 */
	public void notify(String server, String user) {
		
		// For testing on local host - can leave in final code
		if(server.equals("smap")) {
			server = "dev.smap.com.au";
		}
		
		// Get the device registration ids associated with this user on this server
		DeviceTable deviceTable = new DeviceTable(region, tableName);
		ScanResult scanResult = deviceTable.getUserDevices(server, user);
		 
         // Process the results
		snsClientWrapper = new AmazonSNSClientWrapper(sns, deviceTable);
         List<Map<String, AttributeValue>> items = scanResult.getItems();
         if(items!= null && items.size() > 0) {
        	 	for(Map<String, AttributeValue> item : items) {
        	 		AttributeValue val = item.get("registrationId");
        	 		String token = val.getS();
        	 		
        	 		System.out.println("Token: " + token + " for " + server + ":" + user);
        	 		
        	        // Send the notification
        	 		Map<Platform, Map<String, MessageAttributeValue>> attrsMap = new HashMap<Platform, Map<String, MessageAttributeValue>> ();
        	 		snsClientWrapper.sendNotification(Platform.GCM, token, attrsMap, platformApplicationArn);
        	 		
        	 	}
         } else {
        	 	log.info("No token found for: " + server + ":" + user);
         }
		
	}

}
