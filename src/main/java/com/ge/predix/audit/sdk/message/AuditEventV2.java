package com.ge.predix.audit.sdk.message;


import java.beans.ConstructorProperties;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.ge.predix.audit.sdk.exception.AuditValidationException;
import com.ge.predix.audit.sdk.message.validator.Uuid;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

    @Min(value = 0, message = "Timestamp field should be a valid timestamp")
    private long timestamp;

    @NotNull
    private AuditEnums.Classifier classifier;

    @NotNull
    private AuditEnums.PublisherType publisherType;

    @NotNull
    private AuditEnums.CategoryType categoryType;

    @NotNull
    private AuditEnums.EventType eventType;

    @Length(max = 2048, message = "The field must be maximum 2048 characters")
    private String payload;

    @Length(max = 64, message = "The field must be maximum 64 characters")
    private String correlationId;

    @Length(max = 36, message = "The field must be maximum 36 characters")
    private String tenantUuid;

    @Setter(value=AccessLevel.PACKAGE)
    @Length(max = 100, message = "appName (origin) from TMS token must be maximum length of 100 characters")
    private String appName;

    @Length(max = 36, message = "The field must be maximum 36 characters")
    private String ownerTenant;

    @Length(max = 36, message = "The field must be maximum 36 characters")
    private String operatorTenant;

    @Builder(builderClassName = "AuditEventV2Builder")
    @ConstructorProperties({ "messageId", "timestamp", "classifier", "publisherType", "categoryType", "eventType", "payload", "correlationId", "tenantUuid", "ownerTenant", "operatorTenant"})
    AuditEventV2(@NotNull String messageId, long timestamp, @NotNull AuditEnums.Classifier classifier, @NotNull AuditEnums.PublisherType publisherType, @NotNull AuditEnums.CategoryType categoryType, @NotNull AuditEnums.EventType eventType, String payload, String correlationId, String tenantUuid, String ownerTenant, String operatorTenant) throws AuditValidationException {
        this(timestamp, classifier, publisherType, categoryType, eventType, payload, correlationId, tenantUuid, ownerTenant, operatorTenant, messageId, null);
    }

    //We are keeping this ctor to avoid appName injection.
    protected AuditEventV2(long timestamp, AuditEnums.Classifier classifier, AuditEnums.PublisherType publisherType, AuditEnums.CategoryType categoryType,
    		AuditEnums.EventType eventType, String payload, String correlationId, String tenantUuid, String ownerTenant, String operatorTenant, String messageId, String appName) {
    	this.timestamp = timestamp;
        this.classifier = classifier;
        this.publisherType = publisherType;
        this.categoryType = categoryType;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.tenantUuid = tenantUuid;
        this.ownerTenant = ownerTenant;
        this.operatorTenant = operatorTenant;
        this.messageId = messageId;
        this.appName = appName;
    }

    @Override
    public AuditEventV2 clone() {
        return new AuditEventV2(timestamp, classifier, publisherType, categoryType, eventType, payload, correlationId, tenantUuid, ownerTenant, operatorTenant, messageId, appName);
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
        private String ownerTenant;
        private String operatorTenant;

        public AuditEventV2 build() throws AuditValidationException {
            messageId = (messageId == null)? UUID.randomUUID().toString() : messageId;
            timestamp = (timestamp <= 0) ? timestamp = System.currentTimeMillis() : timestamp;
            classifier = (classifier == null) ? AuditEnums.Classifier.SUCCESS : classifier;
            
            AuditEventV2 auditEventV2 = new AuditEventV2(timestamp, classifier, publisherType, categoryType, eventType, payload, 
            		correlationId, tenantUuid, ownerTenant, operatorTenant, messageId, null);
            
            List<ValidatorReport> report = ValidatorServiceImpl.instance.validate(auditEventV2);
            if (!report.isEmpty()) {
                throw new AuditValidationException(report);
            }
            
            return auditEventV2;
        }
    }
}