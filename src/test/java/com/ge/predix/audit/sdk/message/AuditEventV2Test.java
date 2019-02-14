package com.ge.predix.audit.sdk.message;

import com.ge.predix.audit.sdk.exception.AuditValidationException;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * Created by 212584872 on 1/9/2017.
 */

public class AuditEventV2Test {

    Logger log = Logger.getLogger(AuditEventV2Test.class.getName());

    private static final String TENANT = UUID.nameUUIDFromBytes(
            "TENANT_ID".getBytes(Charset.defaultCharset())).toString();

    private static final String CORRELATION_ID = UUID.nameUUIDFromBytes(
            "CORRELATION_ID".getBytes(Charset.defaultCharset())).toString();


    @Test
    public void auditMessagesDiffTest() throws InterruptedException {
        AuditEventV2 message1 = AuditEventV2.builder()
                        .messageId(TENANT)
                        .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                        .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                        .eventType(AuditEnums.EventType.ACTION)
                        .payload("test1")
                        .build();

        log.info("message1: " + message1.toString());
        Thread.sleep(1);
        AuditEventV2 message2 = AuditEventV2.builder()
                        .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                        .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                        .eventType(AuditEnums.EventType.ACTION)
                        .payload("test2")
                        .build();

        log.info("message2: "+message2.toString());

        assertEquals(TENANT, message1.getMessageId());
        assertThat(message1.getTimestamp() ==
                message2.getTimestamp(), is(false));
        assertThat(message1.getMessageId() ==
                message2.getMessageId(), is(false));

        assertThat(message1.getVersion()==
                message2.getVersion(),is(true));

        assertThat(message1.getVersion()==2,is(true));

        assertThat(message1.hashCode() != message2.hashCode(),is(true));
    }

    @Test(expected = AuditValidationException.class)
    public void auditMessageMandatoryPublisherTypeFieldTest(){
        AuditEventV2.builder()
                .payload("There is no publisher")
                .classifier(AuditEnums.Classifier.FAILURE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.CUSTOM)
                .build();
    }

    @Test(expected = AuditValidationException.class)
    public void auditMessageMandatoryCategoryTypeFieldTest(){
        AuditEventV2.builder()
                .payload("There is no category")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .eventType(AuditEnums.EventType.CUSTOM)
                .build();
    }

    @Test(expected = AuditValidationException.class)
    public void auditMessageMandatoryEventTypeFieldTest(){
        AuditEventV2.builder()
                .payload("There is no category")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .build();
    }

    @Test
    public void auditMessageMandatoryAuditServiceIdFieldTest(){
        AuditEventV2 event =  AuditEventV2.builder()
                .payload("There is auditServiceId")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .build();

        assertThat(event.getMessageId() != null,is(true));
    }

    public void auditMessageMandatoryAuditServiceIdIsNullFieldTest(){
        AuditEventV2 event =  AuditEventV2.builder()
                .payload("There is auditServiceId")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .build();

        assertThat(event.getMessageId() != null,is(true));
    }

    @Test
    public void auditMessageEqualsAndHashCodeTest(){
        AuditEventV2 eventV2 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .build();

        AuditEventV2 eventV1 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .payload("GET v2/apps T.O")
                .build();

        AuditEventV2 eventV3 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.CUSTOM)
                .payload("GET v2/apps T.O")
                .build();

        assertThat(eventV2.equals(eventV2),is(true));
        assertThat(eventV2.hashCode() == eventV1.hashCode(),is(false));
    }

    @Test
    public void auditNoClassifierTest(){
        AuditEventV2 event1 = AuditEventV2.builder()
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .payload("GET v2/apps T.O")
                .build();

        AuditEventV2 cloned = (AuditEventV2) event1.clone();
        assertThat(cloned.getClassifier() == AuditEnums.Classifier.SUCCESS,is(true));
    }

    @Test(expected = AuditValidationException.class)
    public void auditNoPublisherTest(){
        AuditEventV2 eventV1 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .payload("GET v2/apps T.O")
                .build();
    }

    @Test(expected = AuditValidationException.class)
    public void auditNoCategoryTest(){
        AuditEventV2 eventV1 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .payload("GET v2/apps T.O")
                .build();
    }

    @Test(expected = AuditValidationException.class)
    public void auditNoEventTypeTest(){
        AuditEventV2 eventV1 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .payload("GET v2/apps T.O")
                .build();
    }


    @Test
    public void auditTimeStampIs0Test(){
        AuditEventV2 eventV2 = AuditEventV2.builder()
                .timestamp(0)
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .payload("GET v2/apps T.O")
                .build();

        assertThat(eventV2.getTimestamp()>0,is(true));
    }


    @Test
    public void validateEventsBuilderMissingCategoryTypeFailureTest() {

        try {
            AuditEventV2.builder()
                    .eventType(AuditEnums.EventType.STARTUP_EVENT)
                    .classifier(AuditEnums.Classifier.FAILURE)
                    .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                    .build();

        }catch(AuditValidationException e){
            assertFalse(e.getValidationReport().isEmpty());
            assertTrue(e.getValidationReport().get(0).getOriginalMessage().contains("interpolatedMessage='may not be null', propertyPath=categoryType"));
            return;
        }

        fail();
    }


    @Test
    public void validateEventsBuilderInvalidTenantIdFailureTest() {

        try {
            AuditEventV2.builder()
                    .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                    .eventType(AuditEnums.EventType.STARTUP_EVENT)
                    .classifier(AuditEnums.Classifier.FAILURE)
                    .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                    .tenantUuid("a very verrrrrryyyyy looooooonngggg striiinnnnggg")
                    .build();
        }catch(AuditValidationException e){
            assertFalse(e.getValidationReport().isEmpty());
            assertTrue(e.getValidationReport().get(0).getOriginalMessage().contains("interpolatedMessage='The field must be maximum 36 characters', propertyPath=tenantUuid"));
            return;
        }

        fail();
    }
}
