package com.ge.predix.audit.sdk.message;

import com.ge.predix.audit.sdk.AuditClientSyncImplTest;
import com.ge.predix.audit.sdk.exception.UnmodifiableFieldException;

import org.junit.Test;

import java.nio.charset.Charset;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.UUID;
import java.util.logging.Logger;

import com.ge.predix.audit.sdk.exception.UnmodifiableFieldException;

/**
 * Created by 212584872 on 1/12/2017.
 */
public class AuditEventV1Test {
    Logger log = Logger.getLogger(AuditEventV1Test.class.getName());

    String messageId = "MessageId";
    String actionType = "GET";
    String actor = "Me";
    String actorDiplayName = "Ploni";
    String actorUuid = UUID.randomUUID().toString();
    String batchUuid = UUID.randomUUID().toString();
    String resourceUuid = UUID.randomUUID().toString();
    String displayName = actorDiplayName;
    String description = "description";
    String resource = UUID.randomUUID().toString();
    String originator = "originator";
    String tenantUuid = "tenantUuid";
    long eventAt = System.currentTimeMillis();
    long eventsAt = eventAt +1;

    @Test
    public void auditMessagesConstructorTest() throws InterruptedException {
        AuditEventV1 auditA = AuditEventV1.builder()
                .messageId(messageId)
                .actionType(actionType)
                .actor(actor)
                .actorDisplayName(displayName)
                .actorUuid(actorUuid)
                .batchUuid(batchUuid)
                .description(description)
                .resource(resource)
                .resourceUuid(resourceUuid)
                .originator(originator)
                .tenantUuid(tenantUuid)
                .eventAt(eventAt)
                .eventsAt(eventsAt)
                .build();
        log.info("auditA is: "+auditA);
        AuditEventV1 auditB = AuditEventV1.builder()
                .messageId(messageId)
                .actionType(actionType)
                .actor(actor)
                .actorDisplayName(displayName)
                .actorUuid(actorUuid)
                .batchUuid(batchUuid)
                .description(description)
                .resource(resource)
                .resourceUuid(resourceUuid)
                .originator(originator)
                .tenantUuid(tenantUuid)
                .eventAt(eventAt)
                .eventsAt(eventsAt)
                .build();
        log.info("auditB is: "+auditB);

        assertThat(auditA.getMessageId().equals(auditB.getMessageId()) &&
                    auditA.getVersion() == auditB.getVersion() &&
                auditA.getActionType().equals(auditB.getActionType()) &&
                auditA.getTimestamp() <= auditB.getTimestamp() &&
                auditA.getActor().equals(auditB.getActor()) &&
                auditA.getActorDisplayName().equals(auditB.getActorDisplayName()) &&
                auditA.getActorUuid().equals(auditB.getActorUuid()) &&
                auditA.getBatchUuid().equals(auditA.getBatchUuid()) &&
                auditA.getDescription().equals(auditB.getDescription()) &&
                auditA.getResource().equals(auditB.getResource()) &&
                auditA.getResourceUuid().equals(auditB.getResourceUuid()) &&
                auditA.getOriginator().equals(auditB.getOriginator()) &&
                auditA.getTenantUuid().equals(auditB.getTenantUuid()) &&
                auditA.getEventAt() == auditB.getEventAt() &&
                auditA.getEventsAt() == auditB.getEventsAt()
                ,is(true));
    }

    @Test
    public void auditMessagesCloneTest() throws InterruptedException {
        long eventAt = System.currentTimeMillis();
        long eventsAt = eventAt +1;
        AuditEventV1 auditA = AuditEventV1.builder()
                .messageId(messageId)
                .actionType(actionType)
                .actor(actor)
                .actorDisplayName(displayName)
                .actorUuid(actorUuid)
                .batchUuid(batchUuid)
                .description(description)
                .resource(resource)
                .resourceUuid(resourceUuid)
                .originator(originator)
                .tenantUuid(tenantUuid)
                .eventAt(eventAt)
                .eventsAt(eventsAt)
                .build();

        AuditEventV1 auditB = (AuditEventV1) auditA.clone();
        assertThat(auditA.getMessageId().equals(auditB.getMessageId()) &&
                        auditA.getVersion() == auditB.getVersion() &&
                        auditA.getActionType().equals(auditB.getActionType()) &&
                        auditA.getTimestamp() <= auditB.getTimestamp() &&
                        auditA.getActor().equals(auditB.getActor()) &&
                        auditA.getActorDisplayName().equals(auditB.getActorDisplayName()) &&
                        auditA.getActorUuid().equals(auditB.getActorUuid()) &&
                        auditA.getBatchUuid().equals(auditA.getBatchUuid()) &&
                        auditA.getDescription().equals(auditB.getDescription()) &&
                        auditA.getResource().equals(auditB.getResource()) &&
                        auditA.getResourceUuid().equals(auditB.getResourceUuid()) &&
                        auditA.getOriginator().equals(auditB.getOriginator()) &&
                        auditA.getTenantUuid().equals(auditB.getTenantUuid()) &&
                        auditA.getEventAt() == auditB.getEventAt() &&
                        auditA.getEventsAt() == auditB.getEventsAt()
                ,is(true));
    }

    @Test
    public void auditMessagesV1ConstructorTest() throws InterruptedException {
        AuditEventV1 auditA = AuditEventV1.builder()
                .messageId(messageId)
                .actionType(actionType)
                .actor(actor)
                .actorDisplayName(displayName)
                .actorUuid(actorUuid)
                .batchUuid(batchUuid)
                .description(description)
                .resource(resource)
                .resourceUuid(resourceUuid)
                .originator(originator)
                .tenantUuid(tenantUuid)
                .eventAt(eventAt)
                .eventsAt(eventsAt)
                .build();

        log.info(auditA.toString());

        log.info("audita: "+auditA);
        assertThat(
                    auditA.getVersion() == 1 &&
                    auditA.getMessageId().equals(messageId) &&
                    auditA.getActionType().equals(actionType) &&
                    auditA.getActor().equals(actor) &&
                    auditA.getActorDisplayName().equals(displayName) &&
                    auditA.getActorUuid().equals(actorUuid) &&
                    auditA.getBatchUuid().equals(batchUuid) &&
                    auditA.getDescription().equals(description) &&
                    auditA.getResource().equals(resource) &&
                    auditA.getResourceUuid().equals(resourceUuid) &&
                    auditA.getOriginator().equals(originator) &&
                    auditA.getTenantUuid().equals(tenantUuid) &&
                    auditA.getTimestamp() <= System.currentTimeMillis() &&
                    auditA.getEventAt() == eventAt &&
                    auditA.getEventsAt() == eventsAt
                ,is(true));
    }




    @Test
    public void auditMessagesDiffTest() throws InterruptedException {
        AuditEventV1 auditA = AuditEventV1.builder()
                .build();
        log.info("auditA is: "+auditA);

        Thread.sleep(1);

        AuditEventV1 auditB = AuditEventV1.builder()
                .build();
        log.info("auditB is: "+auditB);

        assertThat(auditA.getTimestamp() ==
            auditB.getTimestamp(), is(false));

        assertThat(auditA.getMessageId()
                .equals(auditB.getMessageId()),
                is(false));

        assertThat(auditA.hashCode()!= auditB.hashCode(),
                is(true));
    }

    @Test
    public void auditMessagesVersionDiffTest() throws InterruptedException {
        AuditEvent auditA = AuditEventV1.builder()
                .build();
        log.info("auditA is: "+auditA);

        AuditEvent auditB = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .build();
        assertThat(auditA.getVersion() ==1 &&
                auditB.getVersion() ==2,
                is(true));
    }

    @Test()
    public void auditMessageMandatoryMessageIdFieldTest(){
        String id = AuditEventV1.builder()
                .build().getMessageId();

        assertThat(id == null ||
                    id.equals(""),
                    is(false));
    }

    @Test(expected = UnmodifiableFieldException.class)
    public void auditMessageSetVersionTest() throws UnmodifiableFieldException {
        AuditEventV1.builder().
                version(5).
                build();
    }
}