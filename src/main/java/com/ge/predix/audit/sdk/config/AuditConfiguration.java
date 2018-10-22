package com.ge.predix.audit.sdk.config;


import com.ge.predix.audit.sdk.AuthenticationMethod;
import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;

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

    private AuditConfiguration(String uaaUrl, String uaaClientId, String uaaClientSecret,
                               String ehubZoneId,  String ehubHost, int ehubPort, boolean bulkMode,
                               String tracingUrl, String tracingToken, long tracingInterval,
                               String auditServiceName, String cfAppName, String spaceName,
                               int maxRetryCount, long retryIntervalMillis, int maxNumberOfEventsInCache,
                               ReconnectMode reconnectMode, boolean traceEnabled, String authToken,
                               AuthenticationMethod authenticationMethod, String auditZoneId
    ) {
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

    public static AuditConfigurationWithAuthTokenBuilder builderWithAuthToken(){
        return new AuditConfigurationWithAuthTokenBuilder();
    }

    public static AuditConfigurationBuilder builder(){
        return new AuditConfigurationBuilder();
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
        protected String cfAppName;
        protected String spaceName;
        protected Boolean bulkMode = true;
        protected int maxRetryCount = DEFAULT_RETRY_COUNT;
        protected long retryIntervalMillis = DEFAULT_RETRY_INTERVAL_MILLIS;
        protected int maxNumberOfEventsInCache = DEFAULT_CACHE_SIZE;
        protected ReconnectMode reconnectMode = ReconnectMode.AUTOMATIC;
        protected boolean traceEnabled = false;
        protected AuthenticationMethod authenticationMethod = AuthenticationMethod.UAA_USER_PASS;
        protected String auditZoneId;
        protected BasicAuditBuilder(){
        }

        public AuditConfiguration build(){
            return new AuditConfiguration(null,null,null,ehubZoneId,ehubHost,ehubPort,bulkMode,tracingUrl,tracingToken,tracingInterval,auditServiceName, cfAppName, spaceName,maxRetryCount,retryIntervalMillis,maxNumberOfEventsInCache,reconnectMode,traceEnabled,null, authenticationMethod, auditZoneId);
        }

        public BasicAuditBuilder ehubUrl(String ehubUrl) throws IllegalArgumentException {
            if (ehubUrl != null) {
                String[] eventHubHostandPort = ehubUrl.split(":");
                if ( eventHubHostandPort.length != 2 ) {
                    throw new IllegalArgumentException(String.format("ehuburl {%s} is invalid, ehuburl should have the following format: {host.domain:port}", ehubUrl));
                }
                this.ehubHost = eventHubHostandPort[0];
                this.ehubPort = Integer.parseInt(eventHubHostandPort[1]);

            }
            return this;
        }
    }

    @Accessors(chain = true, fluent = true) @Getter @Setter
    public static class AuditConfigurationWithAuthTokenBuilder extends BasicAuditBuilder {

        private String authToken;

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
        public AuditConfigurationWithAuthTokenBuilder cfAppName(String cfAppName){
            this.cfAppName = cfAppName;
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
        public AuditConfigurationWithAuthTokenBuilder ehubUrl(String ehubUrl) {
            super.ehubUrl(ehubUrl);
            return this;
        }

        public AuditConfigurationWithAuthTokenBuilder auditZoneId(String auditZoneId) {
            this.auditZoneId = auditZoneId;
            return this;
        }


        public AuditConfiguration build(){
            AuditConfiguration auditConfiguration = super.build();
            auditConfiguration.authToken = this.authToken;
            auditConfiguration.authenticationMethod = AuthenticationMethod.AUTH_TOKEN;
            return auditConfiguration;

        }
    }

    @Accessors(chain = true, fluent = true) @Getter @Setter
    public static class AuditConfigurationBuilder extends  BasicAuditBuilder {

        private String uaaUrl;
        private String uaaClientId;
        private String uaaClientSecret;

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
        public AuditConfigurationBuilder cfAppName(String cfAppName){
            this.cfAppName = cfAppName;
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
        public AuditConfigurationBuilder ehubUrl(String ehubUrl) {
            super.ehubUrl(ehubUrl);
            return this;
        }

        public AuditConfigurationBuilder auditZoneId(String auditZoneId) {
            this.auditZoneId = auditZoneId;
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