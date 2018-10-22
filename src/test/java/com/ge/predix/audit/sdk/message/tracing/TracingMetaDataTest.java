package com.ge.predix.audit.sdk.message.tracing;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;


/**
 * Created by Martin Saad on 4/27/2017.
 */
public class TracingMetaDataTest {
    Log log = LogFactory.getLog(TracingMetaData.class);

    @Test
    public void auditFieldsTest(){
        TracingMetaData tracingMetaData = TracingMetaData.builder()
                .uaaUrl("asdsad")
                .build();
        log.info(tracingMetaData);

    }



}