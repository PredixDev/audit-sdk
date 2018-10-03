package com.ge.predix.audit.sdk.message;

import com.ge.predix.audit.sdk.exception.UnmodifiableFieldException;
import com.ge.predix.audit.sdk.util.EnvUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

        assertThat(message1.getTimestamp() ==
                message2.getTimestamp(), is(false));
        assertThat(message1.getMessageId() ==
                message2.getMessageId(), is(false));

        assertThat(message1.getVersion()==
                message2.getVersion(),is(true));

        assertThat(message1.getVersion()==2,is(true));

        assertThat(message1.hashCode() != message2.hashCode(),is(true));
    }

    @Test(expected = NullPointerException.class)
    public void auditMessageMandatoryPublisherTypeFieldTest(){
        AuditEventV2.builder()
                .payload("There is no publisher")
                .classifier(AuditEnums.Classifier.FAILURE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.CUSTOM)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void auditMessageMandatoryCategoryTypeFieldTest(){
        AuditEventV2.builder()
                .payload("There is no category")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .eventType(AuditEnums.EventType.CUSTOM)
                .build();
    }

    @Test(expected = NullPointerException.class)
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
                .messageId(null)
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .build();

        assertThat(event.getMessageId() != null,is(true));
    }



    @Ignore
    @Test(expected = UnmodifiableFieldException.class)
    public void auditMessageSetVersionTest() throws UnmodifiableFieldException {
        AuditEventV2.builder().payload("Stam").version(5).build();
    }

    @Test(expected = NullPointerException.class)
    public void auditMessageMandatoryTypes() throws UnmodifiableFieldException {
        AuditEventV2.builder().payload("Stam").messageId("I'm trying to trick you!").build();
    }

    @Test
    public void auditMessageSetPayloadTest(){
        AuditEventV2 eventV2 = AuditEventV2.builder()
                .payload("GET: /v2/resources was unsuccessful")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .tenantUuid(TENANT)
                .correlationId(CORRELATION_ID)
                .messageId("This is automatically generated!")
                .build();

        String newPayload = "The is a new payload!";
        eventV2.setPayload(newPayload);
        assertThat(eventV2.getPayload().equals(newPayload),is(true));
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

    @Test(expected = NullPointerException.class)
    public void auditNoPublisherTest(){
        AuditEventV2 eventV1 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .payload("GET v2/apps T.O")
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void auditNoCategoryTest(){
        AuditEventV2 eventV1 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .payload("GET v2/apps T.O")
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void auditNoEventTypeTest(){
        AuditEventV2 eventV1 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .payload("GET v2/apps T.O")
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void auditNoClassifierConstructorTest(){
        AuditEventV2 event = new AuditEventV2("test",
        2,
        System.currentTimeMillis(),
        null,
        AuditEnums.PublisherType.APP_SERVICE,
        AuditEnums.CategoryType.API_CALLS,
        AuditEnums.EventType.CUSTOM,
        null,
        null,
        null);
    }

    @Test(expected = NullPointerException.class)
    public void auditNoPublisherConstructorTest(){
        AuditEventV2 eventV2 = new AuditEventV2("test",
                2,
                System.currentTimeMillis(),
                AuditEnums.Classifier.FAILURE,
                null,
                AuditEnums.CategoryType.API_CALLS,
                AuditEnums.EventType.CUSTOM,
                null,
                null,
                null);
    }

    @Test(expected = NullPointerException.class)
    public void auditNoCategoryConstructorTest(){
        AuditEventV2 eventV2 = new AuditEventV2("test",
                2,
                System.currentTimeMillis(),
                AuditEnums.Classifier.FAILURE,
                AuditEnums.PublisherType.OS,
                null,
                AuditEnums.EventType.CUSTOM,
                null,
                null,
                null);
    }

    @Test(expected = NullPointerException.class)
    public void auditNoEventTypeConstructorTest(){
        AuditEventV2 eventV2 = new AuditEventV2("test",
                2,
                System.currentTimeMillis(),
                AuditEnums.Classifier.FAILURE,
                AuditEnums.PublisherType.OS,
                AuditEnums.CategoryType.API_CALLS,
                null,
                null,
                null,
                null);
    }

    @Test(expected = NullPointerException.class)
    public void auditTimeStampIs0ConstructorTest(){
        AuditEventV2 eventV2 = new AuditEventV2("test",
                0,
                System.currentTimeMillis(),
                AuditEnums.Classifier.FAILURE,
                AuditEnums.PublisherType.OS,
                AuditEnums.CategoryType.API_CALLS,
                null,
                null,
                null,
                null);

        assertThat(eventV2.getTimestamp()>0,is(true));
    }

    @Test
    public void auditMessageIdIsNullConstructorTest(){
        AuditEventV2 eventV2 = new AuditEventV2("test",
                0,
                System.currentTimeMillis(),
                AuditEnums.Classifier.FAILURE,
                AuditEnums.PublisherType.OS,
                AuditEnums.CategoryType.API_CALLS,
                AuditEnums.EventType.CUSTOM,
                null,
                null,
                null);

        assertThat(eventV2.getMessageId()!= null,is(true));
    }

}
