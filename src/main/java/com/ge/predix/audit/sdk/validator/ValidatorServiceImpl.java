package com.ge.predix.audit.sdk.validator;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Validation;
import javax.validation.Validator;

import com.ge.predix.audit.sdk.message.AuditEvent;

public class ValidatorServiceImpl {
	
    public static ValidatorServiceImpl instance = new ValidatorServiceImpl();

    private Validator validator;

    private ValidatorServiceImpl(){
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    public List<ValidatorReport> validate(AuditEvent event) {
        return validator.validate(event).stream().map(constraint ->
                ValidatorReport.builder().originalMessage(constraint.toString()).build())
                .collect(Collectors.toList());
    }
}