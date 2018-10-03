package com.ge.predix.audit.sdk;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DirectMemoryMonitorTest {

    private int numOfThreads = Thread.activeCount();

    @Test
    public void getInstanceNothingWasInitialized() {
        DirectMemoryMonitor.getInstance().shutdown();
        assertEquals(numOfThreads, Thread.activeCount());
    }

    @Test
    public void startMeasuringDirectMemory() throws InterruptedException {
        DirectMemoryMonitor.getInstance().startMeasuringDirectMemory();
        assertEquals(numOfThreads + 1, Thread.activeCount());
        DirectMemoryMonitor.getInstance().shutdown();
        Thread.sleep(100);
        assertEquals(numOfThreads, Thread.activeCount());
    }

    @Test
    public void startMeasuringDirectMemoryTwiceDoesNothing() throws InterruptedException {
        DirectMemoryMonitor.getInstance().startMeasuringDirectMemory();
        DirectMemoryMonitor.getInstance().startMeasuringDirectMemory();
        assertEquals(numOfThreads + 1, Thread.activeCount());
        DirectMemoryMonitor.getInstance().shutdown();
        Thread.sleep(100);
        assertEquals(numOfThreads, Thread.activeCount());
    }

}