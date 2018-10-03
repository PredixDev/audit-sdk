package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import io.netty.util.internal.PlatformDependent;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Created by 212582776 on 2/13/2018.
 */
public class DirectMemoryMonitor {

    private static CustomLogger log = LoggerUtils.getLogger(DirectMemoryMonitor.class.getName());

    private static class DirectMemoryMonitorHolder {
        private final static DirectMemoryMonitor INSTANCE = new DirectMemoryMonitor();
    }

    public static DirectMemoryMonitor getInstance() {
        return DirectMemoryMonitorHolder.INSTANCE;
    }

    private static final String IO_NETTY_MAX_DIRECT_MEMORY_PATH = "io.netty.maxDirectMemory";
    private static final String MAX_DIRECT_MEMORY_SIZE_STR = "MAX_DIRECT_MEMORY_SIZE";

    private static long MAX_DIRECT_MEMORY_SIZE = 400; //400 MB
    private static final long MEGABYTE = 1024 * 1024;

    private LongSupplier directMemoryCounter = null;
    private ScheduledExecutorService executorService;
    private boolean initialized = false;

    private DirectMemoryMonitor(){
        setMaxDirectMemory();
    }

    public synchronized void startMeasuringDirectMemory() {
         try {
             if (!initialized) {
                 initialized = true;
                 directMemoryCounter = initDirectMemoryCounter();
                 executorService = Executors.newSingleThreadScheduledExecutor();
                 executorService.scheduleAtFixedRate(this::printDirectMemoryCount, 1, 10, TimeUnit.SECONDS);
             }
             else {
                log.warning("Direct memory monitor was already initialized.");
             }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize direct memory monitor", e);
        }
    }

    public synchronized void shutdown(){
        try {
            executorService.shutdownNow();
            initialized = false;
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
            log.severe("Failed to verify max memory set", e);
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
