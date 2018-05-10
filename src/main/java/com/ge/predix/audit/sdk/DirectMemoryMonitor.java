package com.ge.predix.audit.sdk;

import io.netty.util.internal.PlatformDependent;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by 212582776 on 2/13/2018.
 */
public class DirectMemoryMonitor {

	@Getter
	private static Logger log = Logger.getLogger(DirectMemoryMonitor.class.getName());

    private static final String IO_NETTY_MAX_DIRECT_MEMORY_PATH = "io.netty.maxDirectMemory";
    private static final String MAX_DIRECT_MEMORY_SIZE_STR = "MAX_DIRECT_MEMORY_SIZE";

    private static long MAX_DIRECT_MEMORY_SIZE = 400; //400 MB
    private static final long MEGABYTE = 1024 * 1024;

    private LongSupplier directMemoryCounter = null;
    private ScheduledExecutorService executorService;

    public DirectMemoryMonitor(){
        setMaxDirectMemory();
    }

    public void startMeasuringDirectMemory() {
         try {
            directMemoryCounter = initDirectMemoryCounter();
             executorService = Executors.newScheduledThreadPool(1);
             executorService.scheduleAtFixedRate(this::printDirectMemoryCount, 1, 10, TimeUnit.SECONDS);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Exception", e);
        }
    }

    public void shutdown(){
        try {
            executorService.shutdownNow();
        } catch (Exception e) {
            log.info("failed to shutdown DirectMemoryMonitor");
        }
    }
    private void printDirectMemoryCount() {
        log.info("Direct Memory size is: " + (directMemoryCounter.getAsLong() / MEGABYTE) + " MB");
    }

    private static LongSupplier initDirectMemoryCounter() throws NoSuchFieldException, IllegalAccessException {
        Field field = PlatformDependent.class.getDeclaredField("DIRECT_MEMORY_COUNTER");
        field.setAccessible(true);
        AtomicLong counter = (AtomicLong)field.get(null);
        return counter == null ? () -> -1 : counter::get;
    }

    private static void setMaxDirectMemory() {
        getDirectMemorySizeFromEnv();
        log.warning("setting " + IO_NETTY_MAX_DIRECT_MEMORY_PATH + " to " +
                MAX_DIRECT_MEMORY_SIZE + " MB");
        System.setProperty(IO_NETTY_MAX_DIRECT_MEMORY_PATH,
                String.valueOf(MAX_DIRECT_MEMORY_SIZE * MEGABYTE));
        try {
            verifyMaxMemorySet(MAX_DIRECT_MEMORY_SIZE * MEGABYTE);
        } catch (NoSuchFieldException | IllegalAccessException | AssertionError e) {
            log.log(Level.SEVERE, "Failed to verify max memory set", e);
        }
    }

    private static void verifyMaxMemorySet(long expectedMaxMem) throws NoSuchFieldException, IllegalAccessException {
        String memoryLimitFieldName = "DIRECT_MEMORY_LIMIT";
        Field field = PlatformDependent.class.getDeclaredField(memoryLimitFieldName);
        field.setAccessible(true);
        long maxMem = field.getLong(null);
        log.warning( memoryLimitFieldName + " is " + maxMem);
        if (maxMem != expectedMaxMem) {
            throw new AssertionError("Failed to set netty max memory. Expected " +
                    expectedMaxMem + " actual " + maxMem);
        }
    }

    private static void getDirectMemorySizeFromEnv() {
        String maxMem = System.getenv(MAX_DIRECT_MEMORY_SIZE_STR);
        if(null != maxMem){
            MAX_DIRECT_MEMORY_SIZE = Integer.valueOf(maxMem);
        }
    }
}
