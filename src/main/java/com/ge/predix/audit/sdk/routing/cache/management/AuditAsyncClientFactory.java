package com.ge.predix.audit.sdk.routing.cache.management;

import com.ge.predix.audit.sdk.*;
import com.ge.predix.audit.sdk.config.*;
import com.ge.predix.audit.sdk.config.vcap.AuditServiceCredentials;
import com.ge.predix.audit.sdk.exception.RoutingAuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.routing.RoutingAuditCallback;
import com.ge.predix.audit.sdk.routing.cache.ICache;
import com.ge.predix.audit.sdk.routing.cache.impl.AuditAsyncClientHolder;
import com.ge.predix.audit.sdk.routing.tms.AuditTmsClient;
import com.ge.predix.audit.sdk.routing.tms.AuditTokenServiceClient;
import com.ge.predix.audit.sdk.routing.tms.TmsServiceInstance;
import com.ge.predix.audit.sdk.routing.tms.Token;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static com.ge.predix.audit.sdk.util.ExceptionUtils.swallowException;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AuditAsyncClientFactory<T extends AuditEvent>  {

    private static CustomLogger log = LoggerUtils.getLogger(AuditAsyncClientFactory.class.getName());

    private final ICache<String, CommonClientInterface<T>> sharedTenants;
    private final ICache<String, AuditAsyncClientHolder<T>> dedicatedClients;
    private final AuditTmsClient tmsClient;
    private final AuditTokenServiceClient tokenServiceClient;
    @Getter
    private final AuditClientAsyncImpl<T> sharedClient;
    private final RoutingAuditConfiguration configuration;
    private final RoutingAuditCallback<T> callback;

    public AuditAsyncClientFactory(RoutingAuditCallback<T> callback,
                                   RoutingAuditConfiguration configuration,
                                   AuditTmsClient tmsClient,
                                   AuditTokenServiceClient tokenServiceClient,
                                   ICache<String, CommonClientInterface<T>> sharedTenants,
                                   ICache<String, AuditAsyncClientHolder<T>> dedicatedClients) {

        this.sharedTenants = sharedTenants;
        this.dedicatedClients = dedicatedClients;
        this.tmsClient = tmsClient;
        this.tokenServiceClient = tokenServiceClient;
        this.configuration = configuration;
        this.callback = callback;
        this.sharedClient = buildSharedClient(configuration, callback);
    }

    public CommonClientInterface<T> createClient(String tenantUuid) {
        return tmsClient.fetchServiceInstance(tenantUuid)
                .map(instance -> getDedicatedClient(instance, tenantUuid))
                .orElseGet(()-> getSharedClient(tenantUuid));
    }

    private CommonClientInterface<T> getSharedClient(String tenantUuid) {
        log.warning("Update shared tenant cache that tenantUuid %s is shared", tenantUuid);
        return sharedTenants.put(tenantUuid, getSharedClient());
    }

    private CommonClientInterface<T> getDedicatedClient(TmsServiceInstance<AuditServiceCredentials> instance, String tenantUuid) {
        log.warning("Building audit client from TMS service instance %s and tenantUuid %s", instance, tenantUuid);
        Token token = tokenServiceClient.getToken(tenantUuid);
        AuditClientAsyncImpl<T> clientAsync = buildClient(instance.getCredentials(), configuration.getTenantAuditConfig(),
                token, tenantUuid, instance.getServiceInstanceUuid());
        return dedicatedClients.put(tenantUuid, new AuditAsyncClientHolder<T>(clientAsync, tenantUuid,
                instance.getServiceInstanceUuid(), token)).getClient();

    }

    private AuditClientAsyncImpl<T> buildSharedClient(RoutingAuditConfiguration configuration, RoutingAuditCallback<T> callback) {
        try {
            SharedAuditConfig sharedAuditConfig = configuration.getSharedAuditConfig();
            TenantAuditConfig tenantAuditConfig = configuration.getTenantAuditConfig();

            AuditConfiguration auditConfiguration = AuditConfiguration.builder()
                    .tracingInterval(tenantAuditConfig.getTracingInterval())
                    .ehubUrl(configuration.getSharedAuditConfig().getEhubUrl())
                    .auditZoneId(configuration.getSharedAuditConfig().getAuditZoneId())
                    .ehubZoneId(sharedAuditConfig.getEhubZoneId())
                    .tracingUrl(sharedAuditConfig.getTracingUrl())
                    .uaaClientId(sharedAuditConfig.getUaaClientId())
                    .uaaClientSecret(sharedAuditConfig.getUaaClientSecret())
                    .uaaUrl(sharedAuditConfig.getUaaUrl())
                    .bulkMode(tenantAuditConfig.getBulkMode())
                    .retryIntervalMillis(tenantAuditConfig.getRetryIntervalMillis())
                    .maxRetryCount(tenantAuditConfig.getMaxRetryCount())
                    .tracingToken(sharedAuditConfig.getTracingToken())
                    .maxNumberOfEventsInCache(tenantAuditConfig.getMaxNumberOfEventsInCachePerTenant())
                    .traceEnabled(tenantAuditConfig.isTraceEnabled())
                    .cfAppName(tenantAuditConfig.getCfAppName())
                    .auditServiceName(tenantAuditConfig.getAuditServiceName())
                    .reconnectMode(ReconnectMode.AUTOMATIC)
                    .spaceName(tenantAuditConfig.getSpaceName())
                    .build();

            return new AuditClientAsyncImpl<>(auditConfiguration, new AuditCallback<T>() {

                @Override
                public void onFailure(AuditAsyncResult<T> result) { callback.onFailure(result); }

                @Override
                public void onClientError(ClientErrorCode report, String description) { callback.onClientError(report, description, sharedAuditConfig.getAuditZoneId(), null); }

                @Override
                public void onSuccess(List<T> events) {
                    callback.onSuccess(events);
                }
            }, TracingHandlerFactory.newTracingHandler(auditConfiguration, "ROUTING"));
        } catch (Exception e) {
            throw new RoutingAuditException("Could not initialize shared audit client", e);
        }

    }

    private AuditClientAsyncImpl<T> buildClient(AuditServiceCredentials credentials, TenantAuditConfig tenantAuditConfig, Token token, String tenantUuid, String instanceId)  {
        log.info("Initializing dedicated audit client with zone %s for tenantUuid %s, available scopes: {%s}",
                instanceId, tenantUuid, token.getScope());
        try {
            AuditConfiguration auditConfiguration = AuditConfiguration.builderWithAuthToken()
                    .tracingUrl(credentials.getTracingUrl())
                    .tracingToken(credentials.getTracingToken())
                    .tracingInterval(credentials.getTracingInterval())
                    .auditServiceName(tenantAuditConfig.getAuditServiceName())
                    .ehubZoneId(credentials.getEventHubZoneId())
                    .ehubUrl(credentials.getEventHubUri())
                    .auditZoneId(instanceId)
                    .traceEnabled(tenantAuditConfig.isTraceEnabled())
                    .spaceName(tenantAuditConfig.getSpaceName())
                    .retryIntervalMillis(tenantAuditConfig.getRetryIntervalMillis())
                    .bulkMode(tenantAuditConfig.getBulkMode())
                    .cfAppName(tenantAuditConfig.getCfAppName())
                    .maxNumberOfEventsInCache(tenantAuditConfig.getMaxNumberOfEventsInCachePerTenant())
                    .maxRetryCount(tenantAuditConfig.getMaxRetryCount())
                    .reconnectMode(ReconnectMode.AUTOMATIC)
                    .bulkMode(tenantAuditConfig.getBulkMode())
                    .authToken(token.getAccessToken())
                    .build();

            return new AuditClientAsyncImpl<>(auditConfiguration, new AuditCallback<T>() {

                @Override
                public void onFailure(AuditAsyncResult<T> result) {
                    callback.onFailure(result);
                }

                @Override
                public void onClientError(ClientErrorCode report, String description) {
                    log.info("Got failure for audit zone %s and tenantUuid %s, failure: %s, description: %s", instanceId, tenantAuditConfig, report, description);
                    if (report == ClientErrorCode.AUTHENTICATION_FAILURE) {
                        dedicatedClients.get(tenantUuid)
                                .ifPresent((cacheObject) -> swallowException( () ->
                                                cacheObject.refreshNewToken(tokenServiceClient)
                                        , String.format("Audit client %s of tenant %s got Authentication failure, token renew failed as well", instanceId, tenantUuid)));
                    }
                    callback.onClientError(report, description, instanceId, tenantUuid);
                }

                @Override
                public void onSuccess(List<T> events) {
                    callback.onSuccess(events);
                }
            }, TracingHandlerFactory.newTracingHandler(auditConfiguration, "ROUTING"));
        } catch (Exception e) {
            throw new RoutingAuditException(String.format(
                    "Failed to initialize audit client for tenantUuid %s, audit zone id: %s"
                    , tenantUuid, instanceId), e);
        }

    }

}
