package com.ge.predix.audit.sdk.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.routing.tms.AppNameClient;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuditEventsConverterTest {

    private final static String APP_NAME = "test-application";
    private final static String SAMPLE_PAYLOAD = "ok";
    private final static String FAKE = "fake";

    private AppNameClient client = mock(AppNameClient.class);
    private AuditEventsConverter auditEventsConverter = new AuditEventsConverter(client);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        when(client.getAppName()).thenReturn(APP_NAME);
    }

    @Test
    public void v2Extend() throws JsonProcessingException {
        AuditEventV2 eventV2 = AuditEventV2.builder()
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .payload(SAMPLE_PAYLOAD)
                .tenantUuid(FAKE)
                .correlationId(FAKE)
                .build();

        AuditEvent extended = auditEventsConverter.extend(eventV2);
        assertThat(extended.getClass(), is(AuditEventV2.class));
        assertThat(((AuditEventV2)extended).getAppName(), is(APP_NAME));
        assertEquals(eventV2, extended);

    }

    @Test
    public void tracingExtend() {
        AuditTracingEvent tracingEvent = new AuditTracingEvent(FAKE, System.currentTimeMillis(), FAKE);
        assertEquals(tracingEvent, auditEventsConverter.extend(tracingEvent));
    }


    @Test (expected = IllegalArgumentException.class)
    public void extendIllegalClassThrows() {

        AuditEvent event = new AuditEvent() {
            @Override
            public int getVersion() { return -200; }

            @Override
            public String getMessageId() { return FAKE; }

            @Override
            public String getTenantUuid() { return FAKE; }

            @Override
            public AuditEvent clone() { return this; }
        };

        auditEventsConverter.extend(event);
    }
}