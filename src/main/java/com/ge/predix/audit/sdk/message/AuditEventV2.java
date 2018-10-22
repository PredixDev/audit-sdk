package com.ge.predix.audit.sdk.message;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ge.predix.audit.sdk.exception.AuditValidationException;
import com.ge.predix.audit.sdk.message.validator.Uuid;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.UUID;

/**
 * @author Kobi {212584872} on 12/23/2016.
 * @author Igor {212579997}
 */

@ToString
@EqualsAndHashCode
@Getter
public class AuditEventV2 implements AuditEvent {

    private static final int VERSION_2 = 2;

    @NotNull
    @Length(max = 36, message = "The field must be maximum 36 characters")
    @Uuid
    private String messageId;


    private int version = VERSION_2;

    private long timestamp;

    @NotNull
    private AuditEnums.Classifier classifier;

    @NotNull
    private AuditEnums.PublisherType publisherType;

    @NotNull
    private AuditEnums.CategoryType categoryType;

    @NotNull
    private AuditEnums.EventType eventType;

    @Length(max = 1024, message = "The field must be maximum 1024 characters")
    private String payload;

    @Length(max = 64, message = "The field must be maximum 64 characters")
    private String correlationId;

    @Length(max = 36, message = "The field must be maximum 36 characters")
    private String tenantUuid;

    @Setter(value=AccessLevel.PACKAGE)
    @Length(max = 100, message = "appName (origin) from TMS token must be maximum length of 100 characters")
    private String appName;

    @Builder(builderClassName = "AuditEventV2Builder")
    @ConstructorProperties({"timestamp", "classifier", "publisherType", "categoryType", "eventType", "payload", "correlationId", "tenantUuid"})
    AuditEventV2(long timestamp, @NotNull AuditEnums.Classifier classifier, @NotNull AuditEnums.PublisherType publisherType, @NotNull AuditEnums.CategoryType categoryType, @NotNull AuditEnums.EventType eventType, String payload, String correlationId, String tenantUuid) throws AuditValidationException {

        this.timestamp = timestamp;
        this.classifier = classifier;
        this.publisherType = publisherType;
        this.categoryType = categoryType;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.tenantUuid = tenantUuid;

    }

    @Override
    public AuditEventV2 clone() {
        AuditEventV2 build = AuditEventV2.builder()
                .timestamp(timestamp)
                .classifier(classifier)
                .publisherType(publisherType)
                .categoryType(categoryType)
                .eventType(eventType)
                .payload(payload)
                .correlationId(correlationId)
                .tenantUuid(tenantUuid)
                .build();
        build.messageId = this.messageId;
        build.appName = this.appName;
        return build;
    }


    public static class AuditEventV2Builder {

        private long timestamp;
        private AuditEnums.Classifier classifier;
        private String messageId;
        private AuditEnums.PublisherType publisherType;
        private AuditEnums.CategoryType categoryType;
        private AuditEnums.EventType eventType;
        private String payload;
        private String correlationId;
        private String tenantUuid;

        public AuditEventV2 build() throws AuditValidationException {

            AuditEventV2 auditEventV2 = new AuditEventV2(timestamp, classifier, publisherType, categoryType, eventType, payload, correlationId, tenantUuid );
            auditEventV2.messageId = UUID.randomUUID().toString();
            if (auditEventV2.timestamp <= 0) {
                auditEventV2.timestamp = System.currentTimeMillis();
            }
            if(auditEventV2.classifier == null){
                auditEventV2.classifier = AuditEnums.Classifier.SUCCESS;
            }

            List<ValidatorReport> report = ValidatorServiceImpl.instance.validate(auditEventV2);
            if (!report.isEmpty()) {
                throw new AuditValidationException(report);
            }
            return auditEventV2;
        }
    }
}