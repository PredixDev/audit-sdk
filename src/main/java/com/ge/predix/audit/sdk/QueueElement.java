package com.ge.predix.audit.sdk;

import lombok.*;

@Data
@Builder(builderClassName = "QueueElementBuilder")
@EqualsAndHashCode(of = "messageId")
public class QueueElement {
    private String messageId;
    private long timestamp;
    public static class QueueElementBuilder {
        private long timestamp = System.currentTimeMillis();
    }
}
