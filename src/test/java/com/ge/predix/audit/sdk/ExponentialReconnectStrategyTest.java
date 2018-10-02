package com.ge.predix.audit.sdk;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Spy;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

/**
 * Created by 212582776 on 2/15/2018.
 */

public class ExponentialReconnectStrategyTest {


    public  class NotifyConnectedRunnable implements Runnable{



        @Override
        public void run() {
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.CONNECTED);
        }
    }

    public  class ThrowsRuntimeExceptionRunnable implements Runnable{

        @Override
        public void run() {
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.CONNECTING);
            throw new RuntimeException("exception");

        }
    }

    public  class ReconnectFailureRunnable implements Runnable{

        @Override
        public void run() {
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.CONNECTING);
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        }
    }

    private NotifyConnectedRunnable doNothingRunnable = spy(new NotifyConnectedRunnable());
    private ReconnectFailureRunnable reconnectFailureRunnable = spy(new ReconnectFailureRunnable());
    private ThrowsRuntimeExceptionRunnable throwsRuntimeExceptionRunnable = spy(new ThrowsRuntimeExceptionRunnable());


    @Spy
    ExponentialReconnectStrategy exponentialReconnectStrategy;
    private static final int[] backoffIntervals = new int[]{100,200,300,400};

    @Before
    public void init(){


    }
    private void setBackoff() throws NoSuchFieldException, IllegalAccessException {
        Field backoffIntervalsField = ExponentialReconnectStrategy.class.getDeclaredField("reconnectIntervalsMillis");
        backoffIntervalsField.setAccessible(true);
        backoffIntervalsField.set(exponentialReconnectStrategy, backoffIntervals);
    }

    @Test
    public void notifyStateChange_backoffNotStarted_backoffShouldStart() throws Exception {
        exponentialReconnectStrategy = spy(new ExponentialReconnectStrategy(doNothingRunnable));
        setBackoff();
        exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        Thread.sleep(500);
        verify(exponentialReconnectStrategy,times(2)).handleInterval();
    }


    @Test
    public void notifyStateChange_backoffIsStarted_reconnectShouldBeAttemptedTwice() throws Exception {
            exponentialReconnectStrategy = spy(new ExponentialReconnectStrategy(doNothingRunnable));
            setBackoff();
            //should start the flow. reconnect should happen after first interval - i.e after 100 ms
            //t0 - should schedule fot t1 (100). after t1 - always doing reconnect and reschedule.
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
            //t1 - 100ms first execution.
            Thread.sleep(150);
            //this should trigger another reconnect at time t2  - after another 200ms
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
            //t2 - 200ms since there was a failure between t1 and t2 - should try reconnect and schedule.
            //since t3 (another 300 ms) had passed with no notify, the backoff should be stopped and reset.
            Thread.sleep(1000);
            verify(exponentialReconnectStrategy,times(3)).handleInterval();
            verify(doNothingRunnable,times(2)).run();
   }

    @Test
    public void notifyStateChange_backoffResetAndStartsAgain_reconnectShouldBeAttemptedTwice() throws Exception {
        exponentialReconnectStrategy = spy(new ExponentialReconnectStrategy(doNothingRunnable));
        setBackoff();
        exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        Thread.sleep(1000);
        exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        Thread.sleep(500);
        verify(exponentialReconnectStrategy,times(4)).handleInterval();
        verify(doNothingRunnable,times(2)).run();
    }

    @Test
    public void notifyStateChange_stateIsNotifiedManyTimes_reconnectIsAttemptedOnce() throws Exception {
        exponentialReconnectStrategy = spy(new ExponentialReconnectStrategy(doNothingRunnable));
        setBackoff();
        for(int i = 0; i < 100; i++) {
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        }
        Thread.sleep(500);
        verify(exponentialReconnectStrategy,times(2)).handleInterval();
        verify(doNothingRunnable,times(1)).run();
    }


    @Test
    public void notifyStateChange_clientShutdown_reconnectIsNotAttempted() throws Exception {
        exponentialReconnectStrategy = spy(new ExponentialReconnectStrategy(doNothingRunnable));
        setBackoff();
        exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.SHUTDOWN);
        exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        Thread.sleep(500);
        verify(exponentialReconnectStrategy,never()).handleInterval();
        verify(doNothingRunnable,never()).run();
    }

    @Test
    public void notifyStateChange_actionThrowsException_reconnectContinues() throws Exception {
        exponentialReconnectStrategy = spy(new ExponentialReconnectStrategy(reconnectFailureRunnable));
        setBackoff();
        exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.DISCONNECTED);
        Thread.sleep(150);
        verify(exponentialReconnectStrategy,times(1)).handleInterval();
        verify(reconnectFailureRunnable,times(1)).run();
        Thread.sleep(200);
        verify(exponentialReconnectStrategy,times(2)).handleInterval();
        verify(reconnectFailureRunnable,times(2)).run();

        doAnswer(invocation -> {
            exponentialReconnectStrategy.notifyStateChanged(AuditCommonClientState.CONNECTED); return null;}).when(reconnectFailureRunnable).run();

        Thread.sleep(1000);
        verify(exponentialReconnectStrategy,times(4)).handleInterval();
        verify(reconnectFailureRunnable,times(3)).run();

    }
}