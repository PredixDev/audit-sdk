package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ValidationException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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
        validatorService = ValidatorServiceImpl.instance;
    }


    @Test
    public void createV2MessageInvalidTenantIdTest() throws ValidationException, NoSuchFieldException, IllegalAccessException {
        String longTenantId = "r3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI1F";
        String expectedErr = "The field must be maximum 36 characters";
        String expectedPathErr = "tenantUuid";

        assertThat(longTenantId.length(),is(37));

        AuditEventV2 audit = AuditEventV2.builder()
                .tenantUuid(UUID.randomUUID().toString())
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();

        Field tenantUuidField = audit.getClass().getDeclaredField("tenantUuid");
        tenantUuidField.setAccessible(true);
        tenantUuidField.set(audit, longTenantId);

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

        audit = AuditEventV2.builder()
                .tenantUuid(UUID.randomUUID().toString())
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();
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

        audit = AuditEventV2.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();
        reports = validatorService.validate(audit);
        assertTrue(reports.isEmpty());
    }

    @Test
    public void createV2MessageInvalidCorrelationIdTest() throws ValidationException, NoSuchFieldException, IllegalAccessException {
        String correlationId = "66abcdefghijklmnopqrstuvwxyzr3hXNg4a1NMW7CJUktU5tnpwZreVZgNDyDI112"; //66 chars
        AuditEventV2 audit = AuditEventV2.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();

        Field correlationIdField = audit.getClass().getDeclaredField("correlationId");
        correlationIdField.setAccessible(true);
        correlationIdField.set(audit, correlationId);

        List<ValidatorReport> reports = validatorService.validate(audit);
        assertTrue(!reports.isEmpty());
        assertThat(reports.get(0).getOriginalMessage(), containsString(CORRELATION_ID));
    }
}