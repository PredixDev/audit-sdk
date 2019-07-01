package com.ge.predix.audit.sdk.config.vcap;

import com.ge.predix.audit.sdk.config.*;
import com.ge.predix.audit.sdk.exception.VcapLoadException;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.EnvUtils;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.ge.predix.audit.sdk.util.EnvUtils.mapExistingEnvironmentVar;

/**
 * Created by Igor on 20/11/2016.
 */
public class VcapLoaderServiceImpl implements VcapLoaderService {

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
    private static final String AUDIT_TRACE_ENABLED = "AUDIT_TRACE_ENABLED";

    private static CustomLogger log = LoggerUtils.getLogger(VcapLoaderServiceImpl.class.getName());

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

    @Setter
    private boolean traceEnabled;

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
        String traceFromEnv = System.getenv(AUDIT_TRACE_ENABLED);
        this.traceEnabled = traceFromEnv == null || !traceFromEnv.equalsIgnoreCase("false"); // default true
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
        log.warning("vcapServicesEnv: %s" , vcapServicesEnv);
        log.warning("vcapApplicationEnv: %s" , vcapApplicationEnv);
        deserializer.setAuditServiceName(auditServiceName);
        vcapServices = gson.fromJson(vcapServicesEnv, VcapServices.class);
        vcapApplication = gson.fromJson(vcapApplicationEnv, VcapApplication.class);
        validateVcap();
        return buildAuditConfiguration();
    }

    @Override
    public RoutingAuditConfiguration getRoutingConfigFromVcap() throws VcapLoadException {
        try {
            AuditConfiguration configuration = getConfigFromVcap();
            String systemUaaUrl = EnvUtils.getEnvironmentVar("AUDIT_SYSTEM_TRUSTED_ISSUER");
            return RoutingAuditConfiguration.builder()
                .appNameConfig(AppNameConfig.builder()
                        .clientId(EnvUtils.getEnvironmentVar("AUDIT_APP_NAME_CLIENT_ID"))
                        .clientSecret(EnvUtils.getEnvironmentVar("AUDIT_APP_NAME_CLIENT_SECRET"))
                        .uaaUrl(systemUaaUrl)
                        .build())
                .sharedAuditConfig(SharedAuditConfig.builder()
                        .ehubUrl(String.format("%s:%d", configuration.getEhubHost(), configuration.getEhubPort()))
                        .ehubZoneId(configuration.getEhubZoneId())
                        .auditZoneId(configuration.getAuditZoneId())
                        .uaaClientId(configuration.getUaaClientId())
                        .uaaClientSecret(configuration.getUaaClientSecret())
                        .uaaUrl(configuration.getUaaUrl())
                        .tracingToken(configuration.getTracingToken())
                        .tracingUrl(configuration.getTracingUrl())
                        .build())
                .systemConfig(SystemConfig.builder()
                        .clientId(EnvUtils.getEnvironmentVar("AUDIT_SYSTEM_CLIENT_ID"))
                        .clientSecret(EnvUtils.getEnvironmentVar("AUDIT_SYSTEM_CLIENT_SECRET"))
                        .tokenServiceUrl(EnvUtils.getEnvironmentVar("AUDIT_TOKEN_SERVICE_URL"))
                        .tmsUrl(EnvUtils.getEnvironmentVar("AUDIT_TMS_URL"))
                        .build())
                .tenantAuditConfig(TenantAuditConfig.builder()
                        .auditServiceName(configuration.getAuditServiceName())
                        .bulkMode(configuration.getBulkMode())
                        .maxNumberOfEventsInCachePerTenant(configuration.getMaxNumberOfEventsInCache())
                        .maxRetryCount(configuration.getMaxRetryCount())
                        .retryIntervalMillis(configuration.getRetryIntervalMillis())
                        .spaceName(configuration.getSpaceName())
                        .cfAppName(vcapApplication.getAppName())
                         .traceEnabled(configuration.isTraceEnabled())
                        .tracingInterval(configuration.getTracingInterval())
                        .build())
                .routingResourceConfig(getRoutingResourceConfig())
                .build();
            } catch (RuntimeException e) {
                throw new VcapLoadException(e);
            }
    }

    public RoutingResourceConfig getRoutingResourceConfig() {
        RoutingResourceConfig.RoutingResourceConfigBuilder builder = RoutingResourceConfig.builder();
        updateEnv("AUDIT_ROUTING_NUM_OF_CONNECTIONS", Integer::valueOf, builder::numOfConnections);
        updateEnv("AUDIT_ROUTING_SHARED_CACHE_SIZE", Integer::valueOf, builder::sharedTenantCacheSize);
        updateEnv("AUDIT_ROUTING_CACHE_REFRESH_PERIOD", Long::valueOf, builder::cacheRefreshPeriod);
        updateEnv("AUDIT_ROUTING_CONNECTION_LIFETIME", Long::valueOf, builder::connectionLifetime);
        updateEnv("AUDIT_ROUTING_MAX_CONCURRENT_REQUESTS", Integer::valueOf, builder::maxConcurrentAuditRequest);

        return builder.build();
    }

    private <T> void updateEnv(String parameter, Function<String, T> function, Consumer<T> consumer){
        consumeIfPresent(mapExistingEnvironmentVar(parameter, function), consumer);
    }

    private <T> void consumeIfPresent(@Nullable T value, Consumer<T> consumer) {
        if ( value != null ) {
            consumer.accept(value);
        }
    }

    @Override
    public VcapApplication getApplicationFromVcap() throws VcapLoadException{
        try {
            return gson.fromJson(vcapApplicationEnv, VcapApplication.class);
        }catch(RuntimeException e){
            throw new VcapLoadException(e);
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

            AuditConfiguration.AuditConfigurationBuilder auditConfigurationBuilder = AuditConfiguration.builder()
                    .ehubZoneId(auditServiceCredentials.getEventHubZoneId())
                    .uaaUrl(uaaUrl)
                    .uaaClientId(uaaClientId)
                    .uaaClientSecret(uaaClientSecret)
                    .ehubUrl(auditServiceCredentials.getEventHubUri())
                    .tracingUrl(auditServiceCredentials.getTracingUrl())
                    .tracingToken(auditServiceCredentials.getTracingToken())
                    .tracingInterval(auditServiceCredentials.getTracingInterval())
                    .traceEnabled(traceEnabled)
                    .cfAppName(vcapApplication.getName())
                    .auditServiceName(auditServiceName)
                    .auditZoneId(auditServiceCredentials.getAuditQueryApiScope().split("\\.")[2])
                    .spaceName(vcapApplication.getSpaceName());
            //handle optional params. if not set here, will be assigned with default values by the builder.
            if (maxRetries != null) {
                auditConfigurationBuilder.maxRetryCount(Integer.valueOf(maxRetries));
            }
            if (retryIntervalMillis != null) {
                auditConfigurationBuilder.retryIntervalMillis(Long.valueOf(retryIntervalMillis));
            }
            if (maxCachedEvents != null) {
                auditConfigurationBuilder.maxNumberOfEventsInCache(Integer.valueOf(maxCachedEvents));
            }
            if (reconnectPolicy != null) {
                auditConfigurationBuilder.reconnectMode(ReconnectMode.valueOf(reconnectPolicy));
            }
            return auditConfigurationBuilder.build();
        }catch(RuntimeException e){
            throw new VcapLoadException(e);
        }
    }


}