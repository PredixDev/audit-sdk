package com.ge.predix.audit.sdk;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.util.ExceptionUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by 212554562 on 10/2/2018.
 */
@Data
@Builder
@AllArgsConstructor
public class AuditAsyncResult<T extends AuditEvent> {

    private List<AuditEventFailReport<T>> failReports;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Result report:\n");
        sb.append(getSummary());
        return sb.toString();
    }

    String getSummary() {

        if(failReports == null){
            return null;
        }

        Map<FailCode, List<AuditEventFailReport<T>>> failCodesMap = failReports.stream()
                .collect(Collectors.groupingBy(AuditEventFailReport::getFailureReason));

        StringBuilder sb = new StringBuilder();
        
        failCodesMap.entrySet().forEach((entry) -> {
        	switch (entry.getKey()) {
				case ADD_MESSAGE_ERROR:
					String message = "Failed to add the event with id %s to eventhub cache. Error description: %s";
					entry.getValue().forEach(fail -> sb.append(getLogDescription(message, fail.getAuditEvent().getMessageId(), ExceptionUtils.toString(fail.getThrowable()))));
					break;
				case BAD_ACK:
					message = "A back ACK was received for event with id %s. Error description: %s";
					entry.getValue().forEach(fail -> sb.append(getLogDescription(message, fail.getAuditEvent().getMessageId(), fail.getDescription())));
					break;
				case JSON_ERROR:
					message = "Failed to parse event with id %s. Error description: %s";
					entry.getValue().forEach(fail -> sb.append(getLogDescription(message, fail.getAuditEvent().getMessageId(), fail.getThrowable().getMessage())));
					break;
				case VALIDATION_ERROR:
					message = "Failed to validate event with id %s. Validation report: %s";
					entry.getValue().forEach(fail -> sb.append(getLogDescription(message, fail.getAuditEvent().getMessageId(), fail.getDescription())));
					break;
				case DUPLICATE_EVENT:
					message = "An event with id %s already exists in the cache";
					entry.getValue().forEach(fail -> sb.append(getLogDescription(message, fail.getAuditEvent().getMessageId())));
					break;
				case CACHE_IS_FULL:
					message = "%d events have been removed from audit cache because it is full";
					sb.append(getLogDescription(message, entry.getValue().size()));
					break;
				case CLIENT_INITIALIZATION_ERROR:
					message = "%d events could not be audited becuase the client was not properly initialized. Error description: %s";
					sb.append(getLogDescription(message, entry.getValue().size(), ExceptionUtils.toString(entry.getValue().get(0).getThrowable())));
					break;
				case NO_ACK:
					message = "%d events did not receive ACKs in a timely fashion";
					sb.append(getLogDescription(message, entry.getValue().size()));
					break;
				default:
					break;
        	}
        });
        
        return sb.toString();
    }
 
    private String getLogDescription(String message, Object... arguments) {
    	return String.format(message, arguments) + "\n";
    }
}
