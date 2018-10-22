package com.ge.predix.audit.sdk.validator;


import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;

public class ValidatorServiceImpl {


    public static ValidatorServiceImpl instance = new ValidatorServiceImpl();

    private static CustomLogger log = LoggerUtils.getLogger(ValidatorServiceImpl.class.getName());

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