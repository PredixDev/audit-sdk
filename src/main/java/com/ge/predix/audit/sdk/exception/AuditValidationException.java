package com.ge.predix.audit.sdk.exception;

import com.ge.predix.audit.sdk.validator.ValidatorReport;
import lombok.Getter;

import java.util.List;

/**
 * Created by 212554562 on 8/29/2018.
 */
public class AuditValidationException extends RuntimeException {

    @Getter
    private List<ValidatorReport> validationReport;

    public AuditValidationException(List<ValidatorReport> validationReport) {
        super();
        this.validationReport = validationReport;
    }
}
