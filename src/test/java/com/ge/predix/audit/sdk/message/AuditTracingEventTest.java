package com.ge.predix.audit.sdk.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Martin Saad on 4/27/2017.
 */
public class AuditTracingEventTest {

    Log log = LogFactory.getLog(AuditTracingEventTest.class);

    public static String eventhubZoneId = UUID.randomUUID().toString();
    @Test
    public void auditTracingMessagesIdDiffTest() {

        AuditTracingEvent event1 = new AuditTracingEvent(null,System.currentTimeMillis(),eventhubZoneId);
        AuditTracingEvent event2 = new AuditTracingEvent(null,0,eventhubZoneId);
        log.info(event1.toString());
        log.info(event2.toString());
        assertThat(event1.getMessageId().equals(event2.getMessageId()),is(false));
    }

    @Test
    public void auditTracingMessagesTimeStampDiffTest() throws InterruptedException {
        AuditTracingEvent event1 = new AuditTracingEvent("",0,eventhubZoneId);
        Thread.sleep(2);
        AuditTracingEvent event2 = new AuditTracingEvent("",0,eventhubZoneId);
        assertThat(event2.getTimestamp()>event1.getTimestamp(),is(true));
    }

    @Test
    public void auditTracingMessagesTimeStampTest() throws InterruptedException {
        long time = System.currentTimeMillis();
        AuditTracingEvent event1 = new AuditTracingEvent(null,2,eventhubZoneId);
        Thread.sleep(2);
        AuditTracingEvent event2 = new AuditTracingEvent(null,2,eventhubZoneId);
        assertThat(event1.getTimestamp(),is(event2.getTimestamp()));
    }

    @Test
    public void auditTracingMessagesVersionTest() throws InterruptedException {
        AuditTracingEvent event1 = new AuditTracingEvent(null,2,eventhubZoneId);
        assertThat(event1.getVersion(),is(-1));
    }

    @Test
    public void auditTracingMessagesSetVersionTest() throws InterruptedException {
        AuditTracingEvent event1 = new AuditTracingEvent(null,2,eventhubZoneId);
        event1.setVersion(3);
        assertThat(event1.getVersion(),is(-1));
    }

    @Test
    public void auditTracingMessagesCloneTest() throws InterruptedException {
        AuditTracingEvent event1 = new AuditTracingEvent(null,2,eventhubZoneId);;
        AuditEvent event2 = event1.clone();
        assertThat(event1,is(event2));
    }

}