package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Created by 212582776 on 2/15/2018.
 */
public class ExponentialReconnectStrategy implements ReconnectStrategy {
    //TODO  add connected state tests
    private static CustomLogger log = LoggerUtils.getLogger(ExponentialReconnectStrategy.class.getName());

    /* Will try to reconnect for about 5 minutes  in a backoff delay manner.
    * After which if still unsuccessful, will try to reconnect every 5 minutes indefinitely.
    */
    protected static int[] reconnectIntervalsMillis = new int[]{2000,2000,2000,2000,2000,4000,4000,8000,8000,16000,16000,30000,30000,60000,120000,300000};

    ScheduledExecutorService threadExecutor;
    private Runnable actionToPerform;

    private volatile AtomicInteger curIndex;
    private volatile AtomicBoolean isBetweenIntervals;
    private volatile AtomicBoolean shouldReconnectNextInterval;
    private final String logPrefix;

    public ExponentialReconnectStrategy(Runnable actionToPerform, String logPrefix) {
        this.logPrefix = logPrefix;
        threadExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("AUDIT-ExponentialReconnectStrategy-%d").build());
        curIndex = new AtomicInteger(0);
        this.actionToPerform = actionToPerform;
        isBetweenIntervals = new AtomicBoolean(false);
        shouldReconnectNextInterval = new AtomicBoolean(false);
    }

    @Override
    public synchronized void notifyStateChanged(AuditCommonClientState auditCommonClientState)  {
        switch (auditCommonClientState){
            case DISCONNECTED:{
                if (!isBetweenIntervals.get()) {
                    isBetweenIntervals.set(true);
                    threadExecutor.schedule(this::handleInterval,reconnectIntervalsMillis[curIndex.get()], TimeUnit.MILLISECONDS);
                }
                shouldReconnectNextInterval.set(true);
                break;
            }
            case ACKED:{
                shouldReconnectNextInterval.set(false);
                break;
            }
            case SHUTDOWN:{
                shutdown();
                shouldReconnectNextInterval.set(false);
                break;
            }
            case CONNECTING:{
                shouldReconnectNextInterval.set(true);
                break;
            }
            case CONNECTED:{
                shouldReconnectNextInterval.set(false);
                break;
            }
            default:
                break;
        }
    }

    synchronized void handleInterval()  {
        log.logWithPrefix(Level.WARNING, logPrefix,"Running reconnect algo: shouldReconnect: %s, index: %s", shouldReconnectNextInterval, curIndex);
        if (shouldReconnectNextInterval.get()) {
            try {
                if(actionToPerform != null) {
                    actionToPerform.run();
                }
            } catch (Exception e) {
                log.logWithPrefix(Level.WARNING, e, logPrefix, "failed to perform reconnect");
            }
            if (curIndex.get() < reconnectIntervalsMillis.length) {
                curIndex.incrementAndGet();
            }
            threadExecutor.schedule(this::handleInterval, reconnectIntervalsMillis[curIndex.get()], TimeUnit.MILLISECONDS);
        } else {
            curIndex.set(0);
            isBetweenIntervals.set(false);

        }
    }

    private void shutdown() {
        this.threadExecutor.shutdownNow();
        this.actionToPerform = null;
    }


}
