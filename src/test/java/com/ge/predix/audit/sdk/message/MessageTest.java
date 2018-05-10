package com.ge.predix.audit.sdk.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Igor on 13/01/2017.
 */
public class MessageTest {

    private ObjectMapper om;

    private final static String JSON = "{\"tenantUuid\":\"407a73d6-ec39-4ffb-aab3-689b99544957\"}";

    @Before
    public void setUp(){
        om = new ObjectMapper();
    }

    @Test
    public void auditEventFromJsonTest() throws IOException {
        Object o = om.readValue(JSON, AuditEventV1.class);
        assertThat(om.readValue(JSON, AuditEventV1.class), is(notNullValue()));
//        assertThat(om.readValue(JSON, AuditEventV2.class), is(notNullValue()));
    }
}