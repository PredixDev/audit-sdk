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

    private final static String JSON = "{\"messageId\":\"ab887c8f-2094-407a-88f7-8a686f41136c\"," +
                                        "\"version\":2,\"timestamp\":1532590835791,\"classifier\":\"SUCCESS\"," +
                                        "\"publisherType\":\"APP_SERVICE\",\"categoryType\":\"ADMINISTRATIONS\"," +
                                        "\"eventType\":\"ACTION\"}";

    @Before
    public void setUp(){
        om = new ObjectMapper();
    }

    @Test
    public void auditEventFromJsonTest() throws IOException {
        Object o = om.readValue(JSON, AuditEventV2.class);
        assertThat(om.readValue(JSON, AuditEventV2.class), is(notNullValue()));
    }
}