package com.ge.predix.audit.sdk.validator;


import com.ge.predix.audit.sdk.exception.VersioningException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ValidatorServiceImpl implements ValidatorService {

    private static CustomLogger log = LoggerUtils.getLogger(ValidatorServiceImpl.class.getName());

    @Getter
    @Setter
    private PolicyFactory policy;

    @Getter
    @Setter
    private Validator validator;

    private static List<Function<AuditEventV2, ValidatorReport>> sanitizersV2;
    private static Map<Integer, Function<AuditEvent, List<ValidatorReport>>> audits;

    public ValidatorServiceImpl(){
        policy =  new HtmlPolicyBuilder().allowStandardUrlProtocols().toFactory();
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        sanitizersV2 = Lists.newArrayList(
                (event) -> sanitize(event.getPayload(), event::setPayload));

        audits = Maps.newConcurrentMap();
        audits.put(2, (event) -> sanitize((AuditEventV2) event));
    }

    private ValidatorReport sanitize(String original, Consumer<String> consumer){
        String sanitized = null;
        ValidatorReport report = null;
        if(null != original){
            sanitized = policy.sanitize(original);
        }
        if(original != null && !original.equals(sanitized)){
            consumer.accept(sanitized);
            report = ValidatorReport.builder()
                    .originalMessage(original)
                    .sanitizedMessage(sanitized)
                    .build();
        }
        return report;
    }

    @Override
    public List<ValidatorReport> sanitize(AuditEvent event) throws VersioningException {
        return audits.getOrDefault(event.getVersion(), this::throwSome).apply(event);
    }

    private List<ValidatorReport> throwSome(AuditEvent event) throws ValidationException {
        throw new VersioningException(String.format("Version {%d} is not supported", event.getVersion()));
    }

    private List<ValidatorReport> sanitize(AuditEventV2 event) {
        return sanitizersV2.stream().map(consumer ->
                consumer.apply(event)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<ValidatorReport> validate(AuditEvent event) {
        return validator.validate(event).stream().map(constraint ->
                ValidatorReport.builder().originalMessage(constraint.toString()).build())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValid(AuditEvent event) throws VersioningException {
        boolean result = true;
        List<ValidatorReport> reports  = validate(event);
        if(reports.size() > 0){
            log.severe("Invalid {%s}", reports.toString());
            result = false;
        }
        return result;
    }
}