package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.exception.VersioningException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV1;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.owasp.html.HtmlPolicyBuilder;

import javax.validation.ValidationException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ValidatorServiceTest {
    public static final String CORRELATION_ID = "correlationId";
    Logger log = Logger.getLogger(ValidatorServiceTest.class.getName());


    private ValidatorServiceImpl validatorService;

    private static final String GOOD_ONE = "good one";

    private static final String[] FISHY_TAGS = {"<<<", "=", "'", "\""};

    private static final String[] FISHY_STRINGS =
            {   GOOD_ONE + "<style>",
                    GOOD_ONE + "<img src=x onerror=alert(1)>",
                    GOOD_ONE + "<script type=\"text/javascript\">\n" +
                            "    window.setTimeout(function() {\n" +
                            "        document.body.className = document.body.className.replace('loading', '');\n" +
                            "      }, 10); </script>"
            };

    @Before
    public void setUp(){
        validatorService = new ValidatorServiceImpl();
        validatorService.setPolicy(new HtmlPolicyBuilder().allowStandardUrlProtocols().toFactory());
    }

    @Test
    public void extractAuditEventV2AndSanitizeGreenTest() throws IOException, ValidationException {
        AuditEventV2 audit = AuditEventV2.builder()
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .eventType(AuditEnums.EventType.ACTION)
                .payload("Hello world")
                .build();
        List<ValidatorReport> list = validatorService.sanitize(audit);
        assertThat(validatorService.sanitize(audit).size(), is(0));
    }

    @Test
    public void extractAuditEventAndSanitizeGreenTest() throws IOException, ValidationException {
        AuditEvent sampleEvent = AuditEventV1.builder()
                .actionType("Security")
                .actor("application")
                .actorDisplayName("appA")
                .actorUuid(UUID.randomUUID().toString())
                .actionType("Add role")
                .description("Application addRole event")
                .eventsAt(System.currentTimeMillis())
                .tenantUuid(UUID.randomUUID().toString())
                .build();
        assertThat(validatorService.sanitize(sampleEvent).size(), is(0));
    }

    @Test
    public void extractAuditEventAndWithEmptyFieldsGreenTest() throws IOException, ValidationException {
        AuditEvent sampleEvent = AuditEventV1.builder()
                .build();
        assertThat(validatorService.sanitize(sampleEvent).size(), is(0));
    }

    @Test
    public void extractAuditEventV2AndWithEmptyPayloadSanitizeGreenTest() throws IOException, ValidationException {
        AuditEventV2 audit = AuditEventV2.builder()
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .eventType(AuditEnums.EventType.ACTION)
                .build();
        assertThat(validatorService.sanitize(audit).size(), is(0));
    }

    @Test(expected = VersioningException.class)
    public void unSupportedVersionAuditEventV2Test() throws IOException, ValidationException {
        AuditEventV2 audit = new AuditEventV2();
        audit.setVersion(3);
        log.info(audit.toString());
        validatorService.sanitize(audit);
    }

    @Test
    public void extractAuditEventV2AndSanitizeFishyTagsMessageTest() throws IOException, ValidationException {
        for(String fishy: FISHY_TAGS){
            log.info("Trying to test fishy tag: " + fishy);
            createMessageAndTestFishy(fishy);
        }
    }

    @Test
    public void extractAuditEventAndSanitizeFishyTagsMessageTest() throws IOException, ValidationException {
        for(String fishy: FISHY_TAGS){
            createFishyAuditEventAndTestSanitize(fishy, UUID.randomUUID());
        }
    }

    @Test
    public void extractAuditEventV2AndSanitizeFishyStringMessageTest() throws IOException, ValidationException {
        for(String fishy: FISHY_STRINGS){
            log.info("Trying to test fishy string: " + fishy);
            createMessageAndTestFishy(fishy);
        }
    }

    @Test
    public void extractAuditEventAndSanitizeFishyStringsMessageTest() throws IOException, ValidationException {
        for(String fishy: FISHY_STRINGS){
            createFishyAuditEventAndTestSanitize(fishy, UUID.randomUUID());
        }
    }

    private void createFishyAuditEventAndTestSanitize(String fishy, UUID uuid) throws ValidationException {
        AuditEvent sampleEvent = AuditEventV1.builder()
                .actor(fishy)
                .actorUuid(uuid.toString())
                .actorDisplayName(fishy)
                .actionType(fishy)
                .description(fishy)
                .eventsAt(System.currentTimeMillis())
                .tenantUuid(uuid.toString())
                .originator(fishy)
                .resource(fishy)
                .param1(fishy)
                .param2(fishy)
                .param3(fishy)
                .param4(fishy)
                .param5(fishy)
                .param6(fishy)
                .param7(fishy)
                .param8(fishy)
                .build();
        assertThat(validatorService.sanitize(sampleEvent).size(), is(14));
    }

    private void createMessageAndTestFishy(String fishy) throws ValidationException {
        AuditEventV2 audit = AuditEventV2.builder()
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .payload(fishy)
                .build();
        assertThat(validatorService.sanitize(audit).size(), is(1));
    }

    @Test
    public void createV2MessageInvalidTenantIdTest() throws ValidationException {
        String longTenantId = "r3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI1F";
        String expectedErr = "The field must be maximum 36 characters";
        String expectedPathErr = "tenantUuid";

        assertThat(longTenantId.length(),is(37));

        AuditEventV2 audit = AuditEventV2.builder()
                .tenantUuid(longTenantId)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();
        List<ValidatorReport> reports = validatorService.validate(audit);
        assertThat(reports.size(),is(1));

        ValidatorReport report = reports.get(0);
        assertThat(report.toString().contains(expectedErr),is(true));
        assertThat(report.toString().contains(expectedPathErr),is(true));

    }

    @Test
    public void createV1MessageInvalidTenantIdTest() throws ValidationException {
        String longTenantId = "r3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI1F";
        String expectedErr = "The field must be maximum 36 characters";
        String expectedPathErr = "tenantUuid";

        assertThat(longTenantId.length(),is(37));

        AuditEventV1 audit = AuditEventV1.builder()
                .tenantUuid(longTenantId)
                .build();
        List<ValidatorReport> reports = validatorService.validate(audit);
        assertThat(reports.size(),is(1));

        ValidatorReport report = reports.get(0);
        assertThat(report.toString().contains(expectedErr),is(true));
        assertThat(report.toString().contains(expectedPathErr),is(true));
    }

    @Test
    public void createV2MessageValidTenantIdTest() throws ValidationException {
        String tenantId = "r3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI1";
        assertThat(tenantId.length(),is(36));

        AuditEventV2 audit = AuditEventV2.builder()
                .tenantUuid(tenantId)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();
        List<ValidatorReport> reports = validatorService.validate(audit);
        assertThat(reports.isEmpty(),is(true));

        audit.setTenantUuid(UUID.randomUUID().toString());
        reports = validatorService.validate(audit);
        assertThat(reports.isEmpty(),is(true));
    }

    @Test
    public void createV1MessageValidTenantIdTest() throws ValidationException {
        String tenantId = "r3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI1";
        assertThat(tenantId.length(),is(36));

        AuditEventV1 audit = AuditEventV1.builder()
                .tenantUuid(tenantId)
                .build();
        List<ValidatorReport> reports = validatorService.validate(audit);
        assertThat(reports.isEmpty(),is(true));

        audit.setTenantUuid(UUID.randomUUID().toString());
        reports = validatorService.validate(audit);
        assertThat(reports.isEmpty(),is(true));
    }


    @Test
    public void createV2MessageValidCorrelationIdTest() throws ValidationException {
        String correlationId = "abcdefghijklmnopqrstuvwxyzr3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI112";
        assertThat(correlationId.length(),is(64));

        AuditEventV2 audit = AuditEventV2.builder()
                .correlationId(correlationId)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();
        List<ValidatorReport> reports = validatorService.validate(audit);
        assertTrue(reports.isEmpty());

        audit.setCorrelationId(UUID.randomUUID().toString());
        reports = validatorService.validate(audit);
        assertTrue(reports.isEmpty());
    }

    @Test
    public void createV2MessageInvalidCorrelationIdTest() throws ValidationException {
        String correlationId = "66abcdefghijklmnopqrstuvwxyzr3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI112"; //66 chars
        AuditEventV2 audit = AuditEventV2.builder()
                .correlationId(correlationId)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();
        List<ValidatorReport> reports = validatorService.validate(audit);
        assertTrue(!reports.isEmpty());
        assertThat(reports.get(0).getOriginalMessage(), containsString(CORRELATION_ID));
    }
}