package com.ge.predix.audit.sdk.message.tracing;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Mockito.*;

/**
 * Created by 212584872 on 4/30/2017.
 */

public class TracingMessageSenderImplTest {
    private TracingMessageSenderImpl tracingMessageSender;
    private CloseableHttpClient closeableHttpClient;
    private String endpoint;
    private Checkpoint cp;

    @Before
    public void init() {
                 cp = Checkpoint.builder()
                .flowId("Flow ID")
                .state(LifeCycleEnum.CHECK)
                .payload("payload")
                .tenantId("tenant id")
                .build();
        endpoint = "http://127.0.0.1:80";
        closeableHttpClient = mock(CloseableHttpClient.class);
    }

    @Test //We wrote this test in order to test the changes in uriBuilder.
    public void TracingMessageURIBuilderBehaviorSenderTest() throws URISyntaxException {
        tracingMessageSender = new TracingMessageSenderImpl("1", "token");
    }


    @Test
    public void sendTracingMessageTest() throws URISyntaxException, IOException, InterruptedException {
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(closeableHttpClient.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        tracingMessageSender = new TracingMessageSenderImpl(endpoint, "token", closeableHttpClient);
        tracingMessageSender.sendTracingMessage(cp);
        Thread.sleep(5000);
        verify(closeableHttpClient).execute(any(HttpHost.class), any(HttpPost.class));
    }

    @Test
    public void sendTracingMessageWithIOExceptionTest() throws URISyntaxException, IOException, InterruptedException {
        when(closeableHttpClient.execute(any(HttpHost.class), any(HttpPost.class))).thenThrow(new IOException());
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        tracingMessageSender = new TracingMessageSenderImpl(endpoint, "token", closeableHttpClient);
        tracingMessageSender.sendTracingMessage(cp);
        Thread.sleep(5000);
    }
}