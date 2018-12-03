package com.ge.predix.audit.examples.service;

import com.ge.predix.audit.sdk.AuditCallback;
import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.ClientErrorCode;
import com.ge.predix.audit.sdk.Result;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.VcapLoadException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.eventhub.EventHubClientException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

/**
 * Created by Martin Saad on 2/9/2017.
 */
@Service
public class PublisherServiceImpl implements PublisherService {

    private static final Log log = LogFactory.getLog(PublisherServiceImpl.class);

    private AuditClient<AuditEventV2> auditClient;
    private String responseFromCallback = "";

    /**
     * See below the steps to initialize Audit Client
     */
    @PostConstruct
    public void init() throws EventHubClientException, AuditException {
        //Step 1, get configuration from VCAP of predix-audit instance that is bound to the application
        AuditConfiguration auditConfiguration = getConfigFromVcap();

        //Step 2 instantiate Audit Callback
        AuditCallback<AuditEventV2> callback = buildAuditCallback();

        //Step 3, build Audit client.
        auditClient = getAuditClient(auditConfiguration, callback);
    }

    private AuditConfiguration getConfigFromVcap() {
        try {
            VcapLoaderServiceImpl vcapLoaderService = new VcapLoaderServiceImpl();
            return vcapLoaderService.getConfigFromVcap();
        } catch (VcapLoadException e) {
            log.error("Failed to load VCAP due to ", e);
            throw e;
        }
    }

    private AuditCallback<AuditEventV2> buildAuditCallback() {
        return new AuditCallback<AuditEventV2>() {
            @Override
            public void onFailure(Result<AuditEventV2> result) {
                responseFromCallback = result.toString();
            }

            @Override
            public void onClientError(ClientErrorCode clientErrorCode, String description) {
                responseFromCallback = "ClientError, probably something with connection or configuration, ClientErrorCode="+
                        clientErrorCode+" Descripion ="+description;
            }

            @Override
            public void onSuccess(List<AuditEventV2> list) {
                responseFromCallback = "The following Audit Events were sent: "+list.toString();
            }
        };
    }

    private AuditClient<AuditEventV2> getAuditClient(AuditConfiguration auditConfiguration, AuditCallback<AuditEventV2> callback)
            throws EventHubClientException, AuditException {
        try {
            return new AuditClient<>(auditConfiguration, callback);
        } catch (AuditException | EventHubClientException e) {
            log.error("Failed to instantiate Audit Client due to ", e);
            throw e;
        }
    }

    /**
     * See below the steps to use Audit Client
     * @return
     */
    @Override
    public String publishAsync() throws AuditException {
        //Step 1 - Build AuditEventV2
        AuditEventV2 eventV2 = AuditEventV2.builder()
                .payload("This is test payload")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .tenantUuid(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .build();


        //Step 2 - publish the event
        try {
            auditClient.audit(eventV2);
        } catch (AuditException e) {
            log.error("failed to publish Audit Event due to", e);
            throw e;
        }
        return "Sent message "+eventV2.getMessageId();
    }

    @Override
    public String getLastResponse() {
        return responseFromCallback;
    }
}
