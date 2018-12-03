package com.ge.predix.audit.sdk.config;


import javax.validation.constraints.Min;

import com.ge.predix.audit.sdk.AuthenticationMethod;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Kobi (212584872) on 1/10/2017.
 * @author Igor (212579997)
 */
//TODO add documentation for limitation
@Getter
@EqualsAndHashCode
@ToString
public class AuditConfiguration extends AbstractAuditConfiguration {

	private String uaaUrl;
	private String uaaClientId;
	private String uaaClientSecret;
	private String ehubZoneId;
	private String ehubHost;
	private int ehubPort;
	//   private String appName;
	@Min(MIN_CACHE_SIZE)
	private int maxNumberOfEventsInCache;
	private ReconnectMode reconnectMode;
	private String authToken;
	@Setter(AccessLevel.NONE) //this is not exposed to the user. for internal use only
	private AuthenticationMethod authenticationMethod;
	private String tracingUrl;
	private String tracingToken;
	private String auditZoneId;

	private AuditConfiguration(String uaaUrl, 				String uaaClientId,   String uaaClientSecret, 	String ehubZoneId,  
							   String ehubHost, 			int ehubPort, 		  boolean bulkMode, 		String tracingUrl, 
							   String tracingToken, 		long tracingInterval, String auditServiceName, 	String cfAppName, 
							   String spaceName, 			int maxRetryCount, 	  long retryIntervalMillis, int maxNumberOfEventsInCache,
							   ReconnectMode reconnectMode, boolean traceEnabled, String authToken, 		AuthenticationMethod authenticationMethod, 
							   String auditZoneId) {
		super(bulkMode, tracingInterval, auditServiceName, spaceName, maxRetryCount, retryIntervalMillis, traceEnabled, cfAppName);
		this.tracingUrl = tracingUrl;
		this.tracingToken = tracingToken;
		this.uaaUrl = uaaUrl;
		this.uaaClientId = uaaClientId;
		this.uaaClientSecret = uaaClientSecret;
		this.ehubZoneId = ehubZoneId;
		this.ehubHost = ehubHost;
		this.ehubPort = ehubPort;
		this.reconnectMode = reconnectMode;
		this.maxNumberOfEventsInCache = maxNumberOfEventsInCache;
		this.authToken = authToken;
		this.authenticationMethod = authenticationMethod;
		this.auditZoneId = auditZoneId;
	}
	
	@Builder(builderClassName = "AuditConfigurationBuilder")
	private AuditConfiguration(String uaaUrl, 			 String uaaClientId,   		  String uaaClientSecret, String ehubZoneId,
							   String ehubUrl, 			 String ehubHost, 			  int ehubPort, 		  boolean bulkMode,
							   String tracingUrl,		 String tracingToken, 		  long tracingInterval,	  String auditServiceName, 	
							   String cfAppName, 		 String spaceName, 			  int maxRetryCount, 	  int maxNumberOfEventsInCache,
							   long retryIntervalMillis, ReconnectMode reconnectMode, boolean traceEnabled,	  String auditZoneId) {
		this(uaaUrl, uaaClientId, uaaClientSecret, ehubZoneId, ehubHost, ehubPort, bulkMode, tracingUrl, tracingToken, tracingInterval, 
				auditServiceName, cfAppName, spaceName, maxRetryCount, retryIntervalMillis, maxNumberOfEventsInCache, reconnectMode, 
				traceEnabled, null, AuthenticationMethod.UAA_USER_PASS, auditZoneId);
		setEhubUrl(ehubUrl);
	}
	
	@Builder(builderClassName = "AuditConfigurationWithAuthTokenBuilder", builderMethodName="builderWithAuthToken")
	private AuditConfiguration(String ehubZoneId, 		 String ehubUrl, 			   String ehubHost, 			int ehubPort, 
							   boolean bulkMode, 		 String tracingUrl, 		   String tracingToken, 		long tracingInterval, 
							   String auditServiceName,  String cfAppName,  		   String spaceName, 			int maxRetryCount, 	  
							   long retryIntervalMillis, int maxNumberOfEventsInCache, ReconnectMode reconnectMode, boolean traceEnabled, 
							   String authToken,  		 String auditZoneId) {
		this(null, null, null, ehubZoneId, ehubHost, ehubPort, bulkMode, tracingUrl, tracingToken, tracingInterval, 
				auditServiceName, cfAppName, spaceName, maxRetryCount, retryIntervalMillis, maxNumberOfEventsInCache, reconnectMode, 
				traceEnabled, authToken, AuthenticationMethod.AUTH_TOKEN, auditZoneId);
		setEhubUrl(ehubUrl);
	}
	
	private void setEhubUrl(String ehubUrl) throws IllegalArgumentException {
		if (ehubUrl != null) {
			String[] eventHubHostandPort = ehubUrl.split(":");
			if ( eventHubHostandPort.length != 2 ) {
				throw new IllegalArgumentException(String.format("ehuburl {%s} is invalid, ehuburl should have the following format: {host.domain:port}", ehubUrl));
			}
			this.ehubHost = eventHubHostandPort[0];
			this.ehubPort = Integer.parseInt(eventHubHostandPort[1]);

		}
	}

	/****************************** BUILDERS **************************/
	// The following builders have the fields that are supposed to have default values
	// They cannot extend a mutual parent builder with these fields, because lombok doesn't consider them when building.

	public static class AuditConfigurationWithAuthTokenBuilder {
		protected Boolean bulkMode = true;
		protected int maxRetryCount = DEFAULT_RETRY_COUNT;
		protected long retryIntervalMillis = DEFAULT_RETRY_INTERVAL_MILLIS;
		protected int maxNumberOfEventsInCache = DEFAULT_CACHE_SIZE;
		protected ReconnectMode reconnectMode = ReconnectMode.AUTOMATIC;
		protected boolean traceEnabled = false;
	}

	public static class AuditConfigurationBuilder {
		protected Boolean bulkMode = true;
		protected int maxRetryCount = DEFAULT_RETRY_COUNT;
		protected long retryIntervalMillis = DEFAULT_RETRY_INTERVAL_MILLIS;
		protected int maxNumberOfEventsInCache = DEFAULT_CACHE_SIZE;
		protected ReconnectMode reconnectMode = ReconnectMode.AUTOMATIC;
		protected boolean traceEnabled = false;
	}
}