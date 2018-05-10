package com.ge.predix.audit.sdk.config.vcap;

import java.util.logging.Logger;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.ReconnectMode;
import com.ge.predix.audit.sdk.exception.VcapLoadException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by Igor on 20/11/2016.
 */
public class VcapLoaderServiceImpl implements VcapLoaderService{

    private static final String VCAP_SERVICES = "VCAP_SERVICES";
    private static final String AUDIT_SERVICE_NAME = "AUDIT_SERVICE_NAME";
    private static final String VCAP_APPLICATION = "VCAP_APPLICATION";
    private static final String AUDIT_UAA_URL = "AUDIT_UAA_URL";
    private static final String AUDIT_UAA_CLIENT_ID = "AUDIT_UAA_CLIENT_ID";
    private static final String AUDIT_UAA_CLIENT_SECRET = "AUDIT_UAA_CLIENT_SECRET";
    private static final String AUDIT_MAX_RETRY_COUNT = "AUDIT_MAX_RETRY_COUNT";
    private static final String AUDIT_RETRY_INTERVAL_MILLIS = "AUDIT_RETRY_INTERVAL_MILLIS";
    private static final String AUDIT_MAX_CACHED_EVENTS = "AUDIT_MAX_CACHED_EVENTS";
    private static final String AUDIT_RECONNECT_POLICY = "AUDIT_RECONNECT_POLICY";

    @Getter
    private static Logger log = Logger.getLogger(VcapLoaderServiceImpl.class.getName());

    @Setter
    private String vcapServicesEnv;

    @Setter
    private String vcapApplicationEnv;

    @Setter
    private String auditServiceName;

    @Setter
    private String uaaUrl;

    @Setter
    private String uaaClientId;

    @Setter
    private String uaaClientSecret;

    @Setter
    private VcapServicesDeserializer deserializer;

    @Getter
    private VcapServices vcapServices;

    @Getter
    private VcapApplication vcapApplication;

    private Gson gson;

    @Setter
    private String maxRetries;

    @Setter
    private String retryIntervalMillis;

    @Setter
    private String maxCachedEvents;

    @Setter
    private String reconnectPolicy;

    public VcapLoaderServiceImpl() {
        this.vcapServicesEnv = System.getenv(VCAP_SERVICES);
        this.auditServiceName = System.getenv(AUDIT_SERVICE_NAME);
        init();
    }

    public VcapLoaderServiceImpl(String auditServiceName, String vcapApplicationEnv) {
        this.auditServiceName = auditServiceName;
        this.vcapServicesEnv = vcapApplicationEnv;
        init();
    }

    private void init() {
        this.vcapApplicationEnv = System.getenv(VCAP_APPLICATION);
        this.uaaUrl = System.getenv(AUDIT_UAA_URL);
        this.uaaClientId = System.getenv(AUDIT_UAA_CLIENT_ID);
        this.uaaClientSecret = System.getenv(AUDIT_UAA_CLIENT_SECRET);
        this.maxRetries = System.getenv(AUDIT_MAX_RETRY_COUNT);
        this.retryIntervalMillis = System.getenv(AUDIT_RETRY_INTERVAL_MILLIS);
        this.maxCachedEvents = System.getenv(AUDIT_MAX_CACHED_EVENTS);
        this.reconnectPolicy = System.getenv(AUDIT_RECONNECT_POLICY);
        this.deserializer = new VcapServicesDeserializer();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.disableHtmlEscaping()
                .setPrettyPrinting()
                .registerTypeAdapter(VcapServices.class, deserializer)
                .serializeNulls()
                .create();
        gson = gsonBuilder.create();
    }

    @Override
    public AuditConfiguration getConfigFromVcap() throws VcapLoadException {
        log.warning("vcapServicesEnv:" + vcapServicesEnv);
        log.warning("vcapApplicationEnv:" + vcapApplicationEnv);
        deserializer.setAuditServiceName(auditServiceName);
        vcapServices = gson.fromJson(vcapServicesEnv, VcapServices.class);
        vcapApplication = gson.fromJson(vcapApplicationEnv, VcapApplication.class);
        validateVcap();
        return buildAuditConfiguration();
    }

    @Override
    public VcapApplication getApplicationFromVcap() throws VcapLoadException {
        try {
            return gson.fromJson(vcapApplicationEnv, VcapApplication.class);
        }catch (Throwable throwable){
            throw new VcapLoadException(throwable.getMessage());
        }
    }

    private void validateVcap() throws VcapLoadException{
        if ((null != vcapServices) &&
            (null != vcapApplication) &&
            (vcapServices.getAuditService().size() == 1)) {
            log.warning(vcapServices.toString());
        } else {
            throw new VcapLoadException("VCAP not configured well, there are few logical reasons such " +
                    "as no AuditService/multiple AuditService were found, or the values were incorrect");
        }
    }

    private AuditConfiguration buildAuditConfiguration() throws VcapLoadException {
        try {
            AuditService service = vcapServices.getAuditService().get(0);
            AuditServiceCredentials auditServiceCredentials = service.getCredentials();

            String ehubAddr = auditServiceCredentials.getEventHubUri();
            String[] eventHubHostandPort = ehubAddr.split(":");
            String eventHubHost = eventHubHostandPort[0];
            int eventHubPort = Integer.parseInt(eventHubHostandPort[1]);

            AuditConfiguration.AuditConfigurationBuilder auditConfigurationBuilder = AuditConfiguration.builder()
                    .ehubZoneId(auditServiceCredentials.getEventHubZoneId())
                    .uaaUrl(uaaUrl)
                    .uaaClientId(uaaClientId)
                    .uaaClientSecret(uaaClientSecret)
                    .ehubHost(eventHubHost)
                    .ehubPort(eventHubPort)
                    .tracingUrl(auditServiceCredentials.getTracingUrl())
                    .tracingToken(auditServiceCredentials.getTracingToken())
                    .tracingInterval(auditServiceCredentials.getTracingInterval())
                    .appName(vcapApplication.getName())
                    .auditServiceName(auditServiceName)
                    .spaceName(vcapApplication.getSpaceName());
            //handle optional params. if not set here, will be assigned with default values by the builder.
            if(maxRetries != null) {
                auditConfigurationBuilder.maxRetryCount(Integer.valueOf(maxRetries));
            }
            if(retryIntervalMillis != null) {
                auditConfigurationBuilder.retryIntervalMillis(Long.valueOf(retryIntervalMillis));
            }
            if(maxCachedEvents != null){
                auditConfigurationBuilder.maxNumberOfEventsInCache(Integer.valueOf(maxCachedEvents));
            }
            if(reconnectPolicy != null){
                auditConfigurationBuilder.reconnectMode(ReconnectMode.valueOf(reconnectPolicy));
            }
            return auditConfigurationBuilder.build();
        }catch (Throwable t){
            throw new VcapLoadException(t.getMessage());
        }
    }
}