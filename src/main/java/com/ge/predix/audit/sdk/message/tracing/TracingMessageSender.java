package com.ge.predix.audit.sdk.message.tracing;

/**
 * Created by 212584872 on 4/30/2017.
 */

public interface TracingMessageSender {
    void sendTracingMessage(Checkpoint checkpoint);

    void shutdown();
}
