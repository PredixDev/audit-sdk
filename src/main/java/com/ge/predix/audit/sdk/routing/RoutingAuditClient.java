package com.ge.predix.audit.sdk.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.AuditClientState;
import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.DirectMemoryMonitor;
import com.ge.predix.audit.sdk.FailReport;
import com.ge.predix.audit.sdk.config.AppNameConfig;
import com.ge.predix.audit.sdk.config.RoutingAuditConfiguration;
import com.ge.predix.audit.sdk.config.RoutingResourceConfig;
import com.ge.predix.audit.sdk.config.SystemConfig;
import com.ge.predix.audit.sdk.exception.RoutingAuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventsConverter;
import com.ge.predix.audit.sdk.routing.cache.ICache;
import com.ge.predix.audit.sdk.routing.cache.TenantCacheProxy;
import com.ge.predix.audit.sdk.routing.cache.impl.AsyncClientHolderICacheImpl;
import com.ge.predix.audit.sdk.routing.cache.impl.AuditAsyncClientHolder;
import com.ge.predix.audit.sdk.routing.cache.impl.CommonClientInterfaceICacheImpl;
import com.ge.predix.audit.sdk.routing.cache.management.AuditAsyncClientFactory;
import com.ge.predix.audit.sdk.routing.cache.management.AuditAsyncShutdownHandler;
import com.ge.predix.audit.sdk.routing.cache.management.AuditCacheRefresher;
import com.ge.predix.audit.sdk.routing.tms.*;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.ExceptionUtils;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;


/**
 * Created by 212584872
 * RoutingAuditClient to audit events by tenant to the right async {@link com.ge.predix.audit.sdk.AuditClient}
 */
public class RoutingAuditClient<T extends AuditEvent> {

    private static CustomLogger log = LoggerUtils.getLogger(RoutingAuditClient.class.getName());

    private final BlockingQueue<List<T>> eventsBlockingQ;
    private final ExecutorService executor;
    private final RoutingAuditPublisher<T> routingAuditPublisher;
    private final AtomicReference<AuditClientState> auditClientState;
    private final RoutingAuditCallback<T> callback;

    /**
     * Returns an Async audit client to publish audit messages.
     * @param configuration - auditConfiguration
     * @param callback - callback to be invoked for client's changes.
     * @throws RoutingAuditException - when fail to build shared audit client or failed to retrieve appName from TMS
     * */
    public RoutingAuditClient(RoutingAuditConfiguration configuration, RoutingAuditCallback<T> callback) throws RoutingAuditException {
        this.callback = callback;
        auditClientState = new AtomicReference<>(AuditClientState.CONNECTED);
        this.eventsBlockingQ = Queues.newArrayBlockingQueue(configuration.getRoutingResourceConfig().getMaxConcurrentAuditRequest());
        DirectMemoryMonitor monitor = DirectMemoryMonitor.getInstance();
        LoggerUtils.setLogLevelFromVcap();
        //because only prints in debug
        if(LoggerUtils.isDebugLogLevel()) {
            log.info("RoutingAuditConfiguration: %s", configuration);
            monitor.startMeasuringDirectMemory();
        }
        routingAuditPublisher = init(configuration, callback);
        executor = Executors.newSingleThreadExecutor();
        handleEvents();
    }

    RoutingAuditClient(BlockingQueue<List<T>> eventsBlockingQ, ExecutorService executor, RoutingAuditPublisher<T> routingAuditPublisher, AtomicReference<AuditClientState> auditClientState, RoutingAuditCallback<T> callback) {
        this.callback = callback;
        this.eventsBlockingQ = eventsBlockingQ;
        this.executor = executor;
        this.routingAuditPublisher = routingAuditPublisher;
        this.auditClientState = auditClientState;
        handleEvents();
    }

    /**
     * Logs an audit event asynchronously.
     * Result of this operation will be propagated through the AuditCallback.
     *
     * @param event - the event to log.
     * @throws RoutingAuditException - if an unexpected error occurred with auditing
     *         IllegalStateException - if client was already shut down
     */
    public void audit(T event) throws RoutingAuditException, IllegalStateException {
        if (event != null) {
            audit(Collections.singletonList(event));
        }
    }

    /**
     * Logs audit events asynchronously.
     * Result of this operation will be propagated through the AuditCallback.
     * @param events - the events to log
     * @throws RoutingAuditException - if an unexpected error occurred with auditing.
     *          IllegalStateException - if client was already shut down
     */
    public void audit(List<T> events) throws RoutingAuditException, IllegalStateException{
        throwIfShutDown();
        if(events != null && !events.isEmpty()) {
            if( eventsBlockingQ.offer(cloneEvents(events)) ){
                log.info("{%d} events was accepted", events.size());
            }
            else {
                throw new RoutingAuditException(String.format("Could not offer events, there are still {%d} requests in the pipe",
                        eventsBlockingQ.size()));
            }
        }
    }

    /**
     * Shuts-down this client, the callback will notify on failure for every remaining event.
     * this client cannot be restarted after it was shutdown
     * @throws RoutingAuditException - in case there was an error while closing the resources
     */
    public synchronized void shutdown() throws RoutingAuditException {
        try {
            if (auditClientState.get() != AuditClientState.SHUTDOWN) {
                this.auditClientState.set(AuditClientState.SHUTDOWN);
                getRemainingEvents().forEach(e -> callback.onFailure(e, FailReport.NO_MORE_RETRY, "Audit client is shut down!"));
                executor.shutdownNow();
                this.routingAuditPublisher.shutdown();
                DirectMemoryMonitor.getInstance().shutdown();
            }
        } catch (Exception e) {
            throw new RoutingAuditException(e);
        }

    }

    /**
     * Shuts-down this client and waiting for events in queue to be sent.
     * this client cannot be restarted after it was shutdown
     * @throws RoutingAuditException - in case there was an error closing resources or publish the remaining events.
     */
    public synchronized void gracefulShutdown() throws RoutingAuditException {
        if (auditClientState.get() != AuditClientState.SHUTDOWN) {
            try {
                auditClientState.set(AuditClientState.SHUTDOWN);
                executor.shutdownNow();
                publishEvents(getRemainingEvents());
                this.routingAuditPublisher.gracefulShutdown();
                DirectMemoryMonitor.getInstance().shutdown();
            } catch (Exception e) {
                throw new RoutingAuditException(e);
            }
        }
    }

    private List<T> getRemainingEvents() {
        List<List<T>> remainingEvents = new ArrayList<>();
        eventsBlockingQ.drainTo(remainingEvents);
        return remainingEvents.stream().flatMap(List::stream).collect(Collectors.toList());
    }

    private RoutingAuditPublisher<T> init(RoutingAuditConfiguration configuration, RoutingAuditCallback<T> cb) {
        try {
            //Build httpClient
            ObjectMapper objectMapper = new ObjectMapper();
            CloseableHttpClient httpClient = HttpClients.createDefault();

            //Build TMS and tokenService and APPName clients
            AppNameConfig appNameConfig = configuration.getAppNameConfig();
            TokenClient tokenClient = new TokenClient(appNameConfig.getUaaUrl(), appNameConfig.getClientId(),
                    appNameConfig.getClientSecret(), httpClient, objectMapper);

            SystemConfig systemConfig = configuration.getSystemConfig();
            AuditTmsClient tmsClient = new AuditTmsClient(new TmsClient(systemConfig.getTmsUrl(),
                    tokenClient, httpClient, objectMapper), systemConfig.getCanonicalServiceName());
            AuditTokenServiceClient tokenServiceClient = new AuditTokenServiceClient(systemConfig.getCanonicalServiceName(),
                    new TokenServiceClient(httpClient, systemConfig.getTokenServiceUrl(), objectMapper, systemConfig.getClientId(), systemConfig.getClientSecret()));

            AuditEventsConverter converter = new AuditEventsConverter(new AppNameClient(tokenClient, appNameConfig.getAppNamePrefix()));

            RoutingResourceConfig routingResourceConfig = configuration.getRoutingResourceConfig();
            //shutdown client
            AuditAsyncShutdownHandler shutdownClient = new AuditAsyncShutdownHandler(configuration.getTenantAuditConfig(), tokenServiceClient,
                    Executors.newFixedThreadPool(routingResourceConfig.getNumOfConnections()));

            //Dedicated tenants cache
           ICache<String, AuditAsyncClientHolder> dedicatedClients = new AsyncClientHolderICacheImpl(shutdownClient,
                   new AuditCacheRefresher(tmsClient, tokenServiceClient),
                   routingResourceConfig.getConnectionLifetime(),
                   routingResourceConfig.getNumOfConnections(),
                   routingResourceConfig.getCacheRefreshPeriod());

           ICache<String, CommonClientInterface> sharedTenants = new CommonClientInterfaceICacheImpl(routingResourceConfig.getCacheRefreshPeriod(),
                   routingResourceConfig.getSharedTenantCacheSize());

            //Client factory
           AuditAsyncClientFactory<T> clientFactory = new AuditAsyncClientFactory<>(cb, configuration, tmsClient, tokenServiceClient, sharedTenants, dedicatedClients);

           TenantCacheProxy cache = new TenantCacheProxy(dedicatedClients,
                    sharedTenants,
                    tokenServiceClient,
                    clientFactory,
                   routingResourceConfig.getCacheRefreshPeriod());

           return new RoutingAuditPublisher<>(cb, cache, converter);
        } catch (Exception e) {
            throw new RoutingAuditException(e);
        }
    }

    private void handleEvents(){
        executor.execute(() -> {
            while ( auditClientState.get() != AuditClientState.SHUTDOWN ) {
                ExceptionUtils.swallowException(()-> publishEvents(takeEvents()), "error during publish events");
            }
        });
    }

    private void publishEvents(List<T> events) {
        if (events != null && !events.isEmpty()) {
            Map<Boolean, List<T>> eventMap = events.stream().collect(Collectors.groupingBy(AuditEvent::hasTenantUuid));
            auditEventsWithTenantId(eventMap.get(Boolean.TRUE));
            auditEventsWithoutTenantId(eventMap.get(Boolean.FALSE));
        }
    }

    private void auditEventsWithTenantId(List<T> events) {
        if (events != null && !events.isEmpty()) {
            Map<String, List<T>> eventsByTenant = events.stream().collect(Collectors.groupingBy(AuditEvent::getTenantUuid));
            eventsByTenant.forEach((routingAuditPublisher::auditToTenant));
        }
    }

    private void auditEventsWithoutTenantId(List<T> events) {
        if (events != null && !events.isEmpty()) {
            routingAuditPublisher.auditToShared(events.stream()
                    .filter(e -> !e.hasTenantUuid())
                    .collect(Collectors.toList()));
        }
    }

    private List<T> takeEvents() {
        try {
            return eventsBlockingQ.take();
        } catch (InterruptedException e) {
            log.log(Level.INFO, e,"Got InterruptedException, probably AuditRoutingClient was shut down");
            return Lists.newArrayList();
        }
    }

    //To avoid abusing of the Q and retry
    @SuppressWarnings("unchecked")
    private List<T> cloneEvents(List<T> events) {
        return events.stream()
                .filter(Objects::nonNull) //None null events
                .map(e-> (T)e.clone()) //Clone and collect to list
                .collect(Collectors.toList());
    }

    private void throwIfShutDown() {
        if (this.auditClientState.get() == AuditClientState.SHUTDOWN) {
            throw new IllegalStateException("Audit tenant routing client was shut down!");
        }
    }

}
