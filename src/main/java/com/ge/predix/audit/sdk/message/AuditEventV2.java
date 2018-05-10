package com.ge.predix.audit.sdk.message;


import com.ge.predix.audit.sdk.message.validator.Uuid;
import com.ge.predix.audit.sdk.util.EnvUtils;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.UUID;

/**
 * @author Kobi {212584872} on 12/23/2016.
 * @author Igor {212579997}
 */

@Builder(builderClassName = "AuditEventV2Builder")
@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class AuditEventV2 implements AuditEvent {

    private static final String APPLICATION_NAME = EnvUtils.getEnvironmentVar(EnvUtils.APPLICATION_NAME);

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

    @Length(max = 100, message = "The field must be maximum 100 characters")
    private String appName;

    @ConstructorProperties({"messageId", "version", "timestamp", "classifier", "publisherType", "categoryType", "eventType", "payload", "correlationId", "tenantUuid", "appName"})
    public AuditEventV2(String messageId, int version, long timestamp, @NotNull AuditEnums.Classifier classifier, @NotNull AuditEnums.PublisherType publisherType, @NotNull AuditEnums.CategoryType categoryType, @NotNull AuditEnums.EventType eventType, String payload, String correlationId, String tenantUuid, String appName) {
        if (classifier == null) {
            throw new NullPointerException("classifier");
        } else if (publisherType == null) {
            throw new NullPointerException("publisherType");
        } else if (categoryType == null) {
            throw new NullPointerException("categoryType");
        } else if (eventType == null) {
            throw new NullPointerException("eventType");
        } else {
            if (messageId == null) {
                this.messageId = UUID.randomUUID().toString();
            } else {
                this.messageId = messageId;
            }
            if (timestamp > 0) {
                this.timestamp = timestamp;
            } else {
                this.timestamp = System.currentTimeMillis();
            }
            this.version = VERSION_2;

            this.classifier = classifier;
            this.publisherType = publisherType;
            this.categoryType = categoryType;
            this.eventType = eventType;
            this.payload = payload;
            this.correlationId = correlationId;
            this.tenantUuid = tenantUuid;
            this.appName = appName;
        }
    }

    @Override
    public AuditEvent clone() {
        return AuditEventV2.builder()
                .version(VERSION_2)
                .messageId(messageId)
                .timestamp(timestamp)
                .classifier(classifier)
                .publisherType(publisherType)
                .categoryType(categoryType)
                .eventType(eventType)
                .payload(payload)
                .correlationId(correlationId)
                .tenantUuid(tenantUuid)
                .appName(appName)
                .build();
    }

    public static class AuditEventV2Builder {
        private int version = VERSION_2;
        private long timestamp = System.currentTimeMillis();
        private String messageId = UUID.randomUUID().toString();
        private AuditEnums.Classifier classifier = AuditEnums.Classifier.SUCCESS;
        private String appName = APPLICATION_NAME;
    }
}