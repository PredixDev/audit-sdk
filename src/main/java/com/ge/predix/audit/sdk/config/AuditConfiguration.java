package com.ge.predix.audit.sdk.config;


import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.ge.predix.audit.sdk.AuditClientType;
import com.ge.predix.audit.sdk.AuthenticationMethod;
import com.ge.predix.audit.sdk.config.vcap.VcapApplication;

import lombok.*;

/**
 * @author Kobi (212584872) on 1/10/2017.
 * @author Igor (212579997)
 */
//TODO add documentation for limitation
@Getter
@EqualsAndHashCode
@ToString
@Setter()
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditConfiguration {
    public static final int MAX_RETRY_COUNT = 5;
    public static final int DEFAULT_RETRY_COUNT = 2;
    public static final int MIN_RETRY_COUNT = 0;
    public static final long MAX_RETRY_INTERVAL_MILLIS = 20000;
    public static final long DEFAULT_RETRY_INTERVAL_MILLIS = 10000;
    public static final long MIN_RETRY_INTERVAL_MILLIS = 2000;
    public static final int DEFAULT_CACHE_SIZE = 50000;
    public static final int MIN_CACHE_SIZE = 1000;


    private String uaaUrl;
    private String uaaClientId;
    private String uaaClientSecret;
    private String ehubZoneId;
    private String ehubHost;
    private int ehubPort;
    private Boolean bulkMode;
    private String tracingUrl;
    private String tracingToken;
    private long tracingInterval;
    private String auditServiceName;
    private String appName;
    private String spaceName;
    @Max(MAX_RETRY_COUNT)
    @Min(MIN_RETRY_COUNT)
    private int maxRetryCount;
    @Max(MAX_RETRY_INTERVAL_MILLIS)
    @Min(MIN_RETRY_INTERVAL_MILLIS)
    private long retryIntervalMillis;
    @Min(MIN_CACHE_SIZE)
    private int maxNumberOfEventsInCache;
    private ReconnectMode reconnectMode;
    private boolean traceEnabled;
    private String authToken;
    @Deprecated
    private AuditClientType clientType;
    @Setter(AccessLevel.NONE) //this is not exposed to the user. for internal use only
    private AuthenticationMethod authenticationMethod;


    public void updateAppNameAndSpace(VcapApplication vcapApplication) {
        if (null != vcapApplication) {
            appName = vcapApplication.getAppName();
            spaceName = vcapApplication.getSpaceName();
        }
    }

    public static AuditConfigurationWithAuthTokenBuilder builderWithAuthToken(){
        return new AuditConfigurationWithAuthTokenBuilder();
    }

    public static AuditConfigurationBuilder builder(){
        return new AuditConfigurationBuilder();
    }

    public static AuditConfiguration fromConfiguration(AuditConfiguration auditConfiguration){
        return  new AuditConfiguration(auditConfiguration.uaaUrl,
                auditConfiguration.uaaClientId,
                auditConfiguration.uaaClientSecret,
                auditConfiguration.ehubZoneId,
                auditConfiguration.ehubHost,
                auditConfiguration.ehubPort,
                auditConfiguration.bulkMode,
                auditConfiguration.tracingUrl,
                auditConfiguration.tracingToken,
                auditConfiguration.tracingInterval,
                auditConfiguration.auditServiceName,
                auditConfiguration.appName,
                auditConfiguration.auditServiceName,
                auditConfiguration.maxRetryCount,
                auditConfiguration.retryIntervalMillis,
                auditConfiguration.maxNumberOfEventsInCache,
                auditConfiguration.reconnectMode,
                auditConfiguration.traceEnabled,
                auditConfiguration.authToken,
                auditConfiguration.clientType,
                auditConfiguration.authenticationMethod);
    }

    /****************************** BUILDERS **************************/

    static protected class BasicAuditBuilder {
        protected String ehubZoneId;
        protected String ehubHost;
        protected int ehubPort;
        protected String tracingUrl;
        protected String tracingToken;
        protected long tracingInterval;
        protected String auditServiceName;
        protected String appName;
        protected String spaceName;
        protected Boolean bulkMode = true;
        protected int maxRetryCount = DEFAULT_RETRY_COUNT;
        protected long retryIntervalMillis = DEFAULT_RETRY_INTERVAL_MILLIS;
        protected int maxNumberOfEventsInCache = DEFAULT_CACHE_SIZE;
        protected ReconnectMode reconnectMode = ReconnectMode.MANUAL;
        protected boolean traceEnabled = true;
        protected AuthenticationMethod authenticationMethod = AuthenticationMethod.UAA_USER_PASS;
        @Deprecated
        protected AuditClientType clientType;
        protected BasicAuditBuilder(){
        }

        public AuditConfiguration build(){
            return new AuditConfiguration(null,null,null,ehubZoneId,ehubHost,ehubPort,bulkMode,tracingUrl,tracingToken,tracingInterval,auditServiceName, appName, auditServiceName,maxRetryCount,retryIntervalMillis,maxNumberOfEventsInCache,reconnectMode,traceEnabled,null,clientType, authenticationMethod);
        }
    }

    public static class AuditConfigurationWithAuthTokenBuilder extends BasicAuditBuilder {

        private String authToken;

        public AuditConfigurationWithAuthTokenBuilder authToken(String authToken){
            this.authToken = authToken;
            return this;
        }

        public AuditConfigurationWithAuthTokenBuilder ehubHost(String ehubHost){
            this.ehubHost = ehubHost;
            return this;

        }
        public AuditConfigurationWithAuthTokenBuilder ehubZoneId(String ehubZoneId){
            this.ehubZoneId = ehubZoneId;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder ehubPort(int ehubPort){
            this.ehubPort = ehubPort;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder tracingUrl(String tracingUrl) {
            this.tracingUrl = tracingUrl;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder tracingToken(String tracingToken){
            this.tracingToken = tracingToken;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder tracingInterval(long tracingInterval){
            this.tracingInterval = tracingInterval;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder auditServiceName(String auditServiceName) {
            this.auditServiceName = auditServiceName;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder appName(String appName){
            this.appName = appName;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder spaceName(String spaceName){
            this.spaceName = spaceName;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder bulkMode(Boolean bulkMode){
            this.bulkMode = bulkMode;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder maxRetryCount(int maxRetryCount){
            this.maxRetryCount = maxRetryCount;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder retryIntervalMillis(long retryIntervalMillis){
            this.retryIntervalMillis = retryIntervalMillis;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder maxNumberOfEventsInCache(int maxNumberOfEventsInCache){
            this.maxNumberOfEventsInCache = maxNumberOfEventsInCache;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder reconnectMode(ReconnectMode reconnectMode){
            this.reconnectMode = reconnectMode;
            return this;
        }
        public AuditConfigurationWithAuthTokenBuilder traceEnabled(boolean traceEnabled){
            this.traceEnabled = traceEnabled;
            return this;
        }
        @Deprecated
        public AuditConfigurationWithAuthTokenBuilder clientType(AuditClientType clientType){
            this.clientType = clientType;
            return this;
        }

        public AuditConfiguration build(){
            AuditConfiguration auditConfiguration = super.build();
            auditConfiguration.authToken = this.authToken;
            auditConfiguration.authenticationMethod = AuthenticationMethod.AUTH_TOKEN;
            return auditConfiguration;

        }
    }

    public static class AuditConfigurationBuilder extends  BasicAuditBuilder {

        private String uaaUrl;
        private String uaaClientId;
        private String uaaClientSecret;

        public AuditConfigurationBuilder uaaUrl(String uaaUrl){
            this.uaaUrl = uaaUrl;
            return this;
        }

        public AuditConfigurationBuilder uaaClientId(String uaaClientId){
            this.uaaClientId = uaaClientId;
            return this;
        }

        public AuditConfigurationBuilder uaaClientSecret(String uaaClientSecret) {
            this.uaaClientSecret = uaaClientSecret;
            return this;
        }

        public AuditConfigurationBuilder ehubHost(String ehubHost){
            this.ehubHost = ehubHost;
            return this;

        }
        public AuditConfigurationBuilder ehubZoneId(String ehubZoneId){
            this.ehubZoneId = ehubZoneId;
            return this;
        }
        public AuditConfigurationBuilder ehubPort(int ehubPort){
            this.ehubPort = ehubPort;
            return this;
        }
        public AuditConfigurationBuilder tracingUrl(String tracingUrl) {
            this.tracingUrl = tracingUrl;
            return this;
        }
        public AuditConfigurationBuilder tracingToken(String tracingToken){
            this.tracingToken = tracingToken;
            return this;
        }
        public AuditConfigurationBuilder tracingInterval(long tracingInterval){
            this.tracingInterval = tracingInterval;
            return this;
        }
        public AuditConfigurationBuilder auditServiceName(String auditServiceName) {
            this.auditServiceName = auditServiceName;
            return this;
        }
        public AuditConfigurationBuilder appName(String appName){
            this.appName = appName;
            return this;
        }
        public AuditConfigurationBuilder spaceName(String spaceName){
            this.spaceName = spaceName;
            return this;
        }
        public AuditConfigurationBuilder bulkMode(Boolean bulkMode){
            this.bulkMode = bulkMode;
            return this;
        }
        public AuditConfigurationBuilder maxRetryCount(int maxRetryCount){
            this.maxRetryCount = maxRetryCount;
            return this;
        }
        public AuditConfigurationBuilder retryIntervalMillis(long retryIntervalMillis){
            this.retryIntervalMillis = retryIntervalMillis;
            return this;
        }
        public AuditConfigurationBuilder maxNumberOfEventsInCache(int maxNumberOfEventsInCache){
            this.maxNumberOfEventsInCache = maxNumberOfEventsInCache;
            return this;
        }
        public AuditConfigurationBuilder reconnectMode(ReconnectMode reconnectMode){
            this.reconnectMode = reconnectMode;
            return this;
        }
        public AuditConfigurationBuilder traceEnabled(boolean traceEnabled){
            this.traceEnabled = traceEnabled;
            return this;
        }
        @Deprecated
        public AuditConfigurationBuilder clientType(AuditClientType clientType){
            this.clientType = clientType;
            return this;
        }

        public AuditConfiguration build(){
            AuditConfiguration auditConfiguration = super.build();
            auditConfiguration.uaaUrl = this.uaaUrl;
            auditConfiguration.uaaClientId = this.uaaClientId;
            auditConfiguration.uaaClientSecret = this.uaaClientSecret;
            auditConfiguration.authenticationMethod = AuthenticationMethod.UAA_USER_PASS;
            return auditConfiguration;
        }

    }
}