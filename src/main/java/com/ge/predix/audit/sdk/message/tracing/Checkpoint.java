package com.ge.predix.audit.sdk.message.tracing;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Martin Saad on 4/27/2017.
 */
@Builder
@Data
public class Checkpoint implements Serializable {

    public static final String AUDIT_SDK = "Audit SDK";

    private String tenantId;
    private String flowId;
    private final String checkpoint = AUDIT_SDK;
    private LifeCycleEnum state;
    private String payload;

}
