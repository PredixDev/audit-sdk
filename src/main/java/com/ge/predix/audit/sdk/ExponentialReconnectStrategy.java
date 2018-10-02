package com.ge.predix.audit.sdk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 212582776 on 2/15/2018.
 */
public class ExponentialReconnectStrategy implements ReconnectStrategy {
    //TODO  add connected state tests
    private static Log log = LogFactory.getLog(ExponentialReconnectStrategy.class);

    /* Will try to reconnect for about 5 minutes  in a backoff delay manner.
    * After which if still unsuccessful, will try to reconnect every 5 minutes indefinitely.
    */
    protected static int[] reconnectIntervalsMillis = new int[]{2000,2000,2000,2000,2000,4000,4000,8000,8000,16000,16000,30000,30000,60000,120000,300000};

    ScheduledExecutorService threadExecutor;
    private Runnable actionToPerform;

    private volatile AtomicInteger curIndex;
    private volatile AtomicBoolean isBetweenIntervals;
    private volatile AtomicBoolean shouldReconnectNextInterval;
    private volatile AuditCommonClientState auditCommonClientState;

    public ExponentialReconnectStrategy(Runnable actionToPerform) {
        threadExecutor = Executors.newSingleThreadScheduledExecutor();
        curIndex = new AtomicInteger(0);
        this.actionToPerform = actionToPerform;
        isBetweenIntervals = new AtomicBoolean(false);
        shouldReconnectNextInterval = new AtomicBoolean(false);
    }

    @Override
    public synchronized void notifyStateChanged(AuditCommonClientState auditCommonClientState)  {
        this.auditCommonClientState = auditCommonClientState;
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
                shouldReconnectNextInterval.set(false);
                shutdown();
            }
            case CONNECTING:{
                shouldReconnectNextInterval.set(true);
            }
            case CONNECTED:{
                shouldReconnectNextInterval.set(false);
            }
            default:
                break;
        }
    }

    synchronized void handleInterval()  {
         log.info("Running reconnect algo: shouldReconnect: "+shouldReconnectNextInterval+" index: "+curIndex);
         if (shouldReconnectNextInterval.get()) {
            try {
                if(actionToPerform != null) {
                    actionToPerform.run();
                }
               // if (!)
            } catch (Exception e) {
                log.warn("failed to perform reconnect: "+e.getMessage());
            }
            if (curIndex.get() < reconnectIntervalsMillis.length) {
                curIndex.incrementAndGet();
            }
            //shouldReconnectNextInterval.set(false);
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
