package com.ge.predix.audit.sdk.message.tracing;

import com.ge.predix.audit.sdk.AuditClientType;
import com.ge.predix.audit.sdk.message.AuditTracingEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.ge.predix.audit.sdk.AuditClientType;

/**
 * Created by Martin Saad on 4/27/2017.
 */
public class TracingMetaDataTest {
    Log log = LogFactory.getLog(TracingMetaData.class);

    @Test
    public void auditFieldsTest(){
        TracingMetaData tracingMetaData = TracingMetaData.builder()
                .uaaUrl("asdsad")
                .auditClientType(AuditClientType.ASYNC)
                .build();
        log.info(tracingMetaData);

    }



}