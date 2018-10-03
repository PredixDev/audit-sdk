package com.ge.predix.audit.sdk.routing.cache.impl;

import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.config.RoutingAuditConfiguration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class CommonClientInterfaceICacheImplTest {

    private static final String TENANT = "tenant";
    private CommonClientInterfaceICacheImpl commonClientInterfaceICache = new CommonClientInterfaceICacheImpl(500, 1);

    @Test
    public void getNotExistsTenantReturnEmpty () {
        assertTrue(commonClientInterfaceICache.getAll().isEmpty());
        assertFalse(commonClientInterfaceICache.get("tenant").isPresent());
    }

    @Test
    public void getExistsTenantReturnClient () {
        commonClientInterfaceICache.put(TENANT, mock(CommonClientInterface.class));
        assertTrue(commonClientInterfaceICache.get(TENANT).isPresent());
    }

    @Test
    public void getExistsTenantAndRecordExpiredReturnEmpty () throws InterruptedException {
        CommonClientInterface commonClientInterface = mock(CommonClientInterface.class);
        commonClientInterfaceICache.put(TENANT, commonClientInterface);
        assertTrue(commonClientInterfaceICache.get(TENANT).isPresent());
        Thread.sleep(501);
        assertTrue(commonClientInterfaceICache.getAll().isEmpty());
    }

    @Test
    public void deleteTest() {
        CommonClientInterface commonClientInterface = mock(CommonClientInterface.class);
        commonClientInterfaceICache.put(TENANT, commonClientInterface);
        assertTrue(commonClientInterfaceICache.get(TENANT).isPresent());
        commonClientInterfaceICache.delete(TENANT);
        assertFalse(commonClientInterfaceICache.get(TENANT).isPresent());
        assertTrue(commonClientInterfaceICache.getAll().isEmpty());
    }


    @Test
    public void shutdownAndGracefulShutDown() throws Exception {
        CommonClientInterface commonClientInterface = mock(CommonClientInterface.class);
        commonClientInterfaceICache.put(TENANT, commonClientInterface);
        commonClientInterfaceICache.gracefulShutdown();
        assertTrue(commonClientInterfaceICache.getAll().isEmpty());
    }
}