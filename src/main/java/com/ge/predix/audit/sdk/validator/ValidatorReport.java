package com.ge.predix.audit.sdk.validator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidatorReport {
    private String sanitizedMessage;
    private String originalMessage;
}
