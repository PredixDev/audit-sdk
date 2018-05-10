package com.ge.predix.audit.sdk.validator;


import com.ge.predix.audit.sdk.exception.VersioningException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV1;
import com.ge.predix.audit.sdk.message.AuditEventV2;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ValidatorServiceImpl implements ValidatorService {
	@Getter
    private static Logger log = Logger.getLogger(ValidatorServiceImpl.class.getName());

    @Getter
    @Setter
    private PolicyFactory policy;

    @Getter
    @Setter
    private Validator validator;

    private static List<Function<AuditEventV1, ValidatorReport>> sanitizersV1;
    private static List<Function<AuditEventV2, ValidatorReport>> sanitizersV2;
    private static Map<Integer, Function<AuditEvent, List<ValidatorReport>>> audits;

    public ValidatorServiceImpl(){
        policy =  new HtmlPolicyBuilder().allowStandardUrlProtocols().toFactory();
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        sanitizersV1 = Lists.newArrayList(
                (event) -> sanitize(event.getOriginator(), event::setOriginator),
                (event) -> sanitize(event.getDescription(), event::setDescription),
                (event) -> sanitize(event.getActor(), event::setActor),
                (event) -> sanitize(event.getActionType(), event::setActionType),
                (event) -> sanitize(event.getActorDisplayName(), event::setActorDisplayName),
                (event) -> sanitize(event.getResource(), event::setResource),
                (event) -> sanitize(event.getParam1(), event::setParam1),
                (event) -> sanitize(event.getParam2(), event::setParam2),
                (event) -> sanitize(event.getParam3(), event::setParam3),
                (event) -> sanitize(event.getParam4(), event::setParam4),
                (event) -> sanitize(event.getParam5(), event::setParam5),
                (event) -> sanitize(event.getParam6(), event::setParam6),
                (event) -> sanitize(event.getParam7(), event::setParam7),
                (event) -> sanitize(event.getParam8(), event::setParam8));

        sanitizersV2 = Lists.newArrayList(
                (event) -> sanitize(event.getPayload(), event::setPayload));

        audits = Maps.newConcurrentMap();
        audits.put(1, (event) -> sanitize((AuditEventV1) event));
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

    private List<ValidatorReport> sanitize(AuditEventV1 event) {
        return sanitizersV1.stream().map(consumer ->
                consumer.apply(event)).filter(Objects::nonNull).collect(Collectors.toList());
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
            log.severe(String.format("Invalid {%s}", reports.toString()));
            result = false;
        }
        return result;
    }
}