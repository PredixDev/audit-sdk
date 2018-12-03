package com.ge.predix.audit.sdk;

import static com.ge.predix.audit.sdk.AbstractAuditClientImpl.printAck;

import org.junit.Before;
import org.junit.Test;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.tracing.Checkpoint;
import com.ge.predix.audit.sdk.message.tracing.LifeCycleEnum;
import com.ge.predix.eventhub.stub.Ack;
import com.ge.predix.eventhub.stub.AckStatus;

/**
 * Created by 212582776 on 3/4/2018.
 */
public class TracingHandlerImplTest {


    AuditConfiguration goodConfiguration = AuditConfiguration.builder()
            .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .tracingInterval(9000)
                .tracingUrl("http://localhost:443/tracing")
                .uaaUrl("http://localhost:443/uaa")
                .bulkMode(false)
                .traceEnabled(true)
                .retryIntervalMillis(2000)
                .build();
    TracingHandlerImpl tracingHandler;

    @Before
    public void init() throws AuditException {
        tracingHandler = new TracingHandlerImpl(goodConfiguration,"");
    }

    @Test
    public void testBuildMessage() {
        Checkpoint checkpoint = tracingHandler.buildTracingMessage(tracingHandler.auditTracingEvent.get(), LifeCycleEnum.CHECK, printAck(
                Ack.newBuilder()
                        .setId("123")
                        .setStatusCode(AckStatus.ACCEPTED)
                        .setDesc("ackDescription")
                        .build()));
        System.out.println(checkpoint);

    }

}