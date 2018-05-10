package com.ge.predix.audit.sdk.message.tracing;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

/**
 * Created by 212584872 on 4/30/2017.
 */

public class TracingMessageSenderImpl implements TracingMessageSender {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC = "Basic ";

    @Getter
    private static Logger log = Logger.getLogger(TracingMessageSenderImpl.class.getName());
    private final CloseableHttpClient httpClient;
    private final String token;

    private ObjectMapper objectMapper;
    private URIBuilder uriBuilder;
    private Executor executor;

    public TracingMessageSenderImpl(String endpoint, String token)
            throws URISyntaxException {
        this(endpoint, token,  HttpClients.createDefault());
    }

    protected TracingMessageSenderImpl(String destination, String token, CloseableHttpClient closeableHttpClient)
            throws URISyntaxException {
        this.token = token;
        this.objectMapper = new ObjectMapper();
        this.uriBuilder = new URIBuilder(destination);
        this.executor = Executors.newFixedThreadPool(1);
        this.httpClient = closeableHttpClient;
    }

    @Override
    public void sendTracingMessage(final  Checkpoint checkpoint) {
        this.executor.execute(() -> {
            try{
                log.warning("Sending the following checkpoint: "+ checkpoint.toString());
                HttpPost request = buildRequest(checkpoint);
                HttpHost target = new HttpHost(uriBuilder.getHost(), uriBuilder.getPort(), uriBuilder.getScheme());
                CloseableHttpResponse response = httpClient.execute(target, request);
                try{
                    log.info("Response from tracing url: " + response.getEntity().toString());
                } finally {
                    response.close();
                }
            } catch (Throwable e){
                log.warning("exception from sending to tracing: " + e.toString());
            }
        });
    }

    private HttpPost buildRequest(Checkpoint checkpoint)
            throws JsonProcessingException, UnsupportedEncodingException {
        HttpPost request = new HttpPost(uriBuilder.getPath());
        StringEntity input = new StringEntity(objectMapper.writeValueAsString(checkpoint));
        input.setContentType("application/json");
        request.setEntity(input);
        request.setHeader(AUTHORIZATION, BASIC + token);
        return request;
    }
}
