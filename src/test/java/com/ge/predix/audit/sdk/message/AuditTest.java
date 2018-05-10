package com.ge.predix.audit.sdk.message;

import com.ge.predix.audit.sdk.exception.UnmodifiableFieldException;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by 212584872 on 1/9/2017.
 */
public class AuditTest {

    @Test
    public void auditMessagesDiffTest() throws InterruptedException {
        AuditEventV2 message1 = AuditEventV2.builder()
                        .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                        .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                        .eventType(AuditEnums.EventType.ACTION)
                        .payload("test1")
                        .build();
        Thread.sleep(1);
        AuditEventV2 message2 = AuditEventV2.builder()
                        .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                        .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                        .eventType(AuditEnums.EventType.ACTION)
                        .payload("test2")
                        .build();

        assertThat(message1.getTimestamp() ==
                message2.getTimestamp(), is(false));

        assertThat(message1.getMessageId() ==
                message2.getMessageId(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void auditMessageMandatoryFieldsTest(){
        AuditEventV2.builder().payload("Stam").build();
    }

    @Ignore
    @Test(expected = UnmodifiableFieldException.class)
    public void auditMessageSetVersionTest() throws  UnmodifiableFieldException {
        AuditEventV2.builder().payload("Stam").version(5).build();
    }

}
