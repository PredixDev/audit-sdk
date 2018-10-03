package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.config.vcap.AuditServiceCredentials;
import com.ge.predix.audit.sdk.exception.TmsClientException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AuditTmsClientTest {
    private final static String TARGET = "predix-audit";
    private final static String TENANT = "TENANT";

    private AuditTmsClient auditTmsClient;
    private TokenClient tokenClient = mock(TokenClient.class);;
    private CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
    private ObjectMapper om = spy(new ObjectMapper());


    @Before
    public void init () {
        TmsClient tmsClient = new TmsClient("http://tms.com", tokenClient, closeableHttpClient, om);
         auditTmsClient = new AuditTmsClient(tmsClient, TARGET);
    }

    @Test
    public void fetchInstanceFetchingAuditInstance() throws IOException {
        when(tokenClient.getToken(false)).thenReturn(new Token("1", "bearer", 1, "123", "jti"));

        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_OK, "OK"));

        HttpEntity entity = mock(HttpEntity.class);

        when(closeableHttpClient.execute(any()))
                .thenReturn(closeableHttpResponse);

        AuditServiceCredentials credentials = AuditServiceCredentials.builder()
                .auditPubClientScope(Collections.singletonList("scope"))
                .auditQueryApiScope("1")
                .auditQueryApiUrl("http://query.com")
                .eventHubUri("123")
                .eventHubZoneId("zoneId")
                .tracingInterval(1000)
                .tracingToken("1234=")
                .tracingUrl("http://tracing.com")
                .build();

        TmsServiceInstance serviceInstance = TmsServiceInstance.builder()
                .serviceInstanceName("predix-audit")
                .serviceInstanceUuid("12345")
                .canonicalServiceName(TARGET)
                .serviceName(TARGET)
                .createdBy("kuku")
                .createdOn(System.currentTimeMillis())
                .managed(false)
                .status("created")
                .credentials(credentials)
                .build();

        InputStream inputStream =  IOUtils.toInputStream(om.writeValueAsString(serviceInstance));
        when(entity.getContent()).thenReturn(inputStream);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        TmsServiceInstance fetched = auditTmsClient.fetchServiceInstance(TENANT).orElseThrow( () -> new NullPointerException("tms instance should be retrieved"));

        assertEquals(serviceInstance, fetched);
        verify(tokenClient, never()).getToken(true);
        verify(closeableHttpClient).execute(any());
    }

    @Test
    public void fetchInstanceOfNoneExistenceOrSharedTenantReturnEmpty() throws IOException {
        when(tokenClient.getToken(false)).thenReturn(new Token("1", "bearer", 1, "123", "jti"));
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_NOT_FOUND, "tenant not found"));
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream("this is mock!"));
        when(closeableHttpClient.execute(any()))
                .thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        assertFalse(auditTmsClient.fetchServiceInstance(TENANT).isPresent());
        verify(tokenClient).getToken(false);
        verify(tokenClient, never()).getToken(true);
        verify(closeableHttpClient).execute(any());
    }

    @Test
    public void fetchUnsuccessfullyInFirstTimeThrowsException () throws IOException {
        when(tokenClient.getToken(false)).thenReturn(new Token("1", "bearer", 1, "123", "jti"));
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_BAD_GATEWAY, "bad!"));
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream("this is mock!"));
        when(closeableHttpClient.execute(any()))
                .thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        assertThatThrownBy(() -> auditTmsClient.fetchServiceInstance(TENANT)).isInstanceOf(TmsClientException.class);
        verify(tokenClient).getToken(false);
        verify(tokenClient, never()).getToken(true);
    }

    @Test
    public void fetchUnsuccessfullyInSecondTimeThrowsException () throws IOException {
        Token token = new Token("1", "bearer", 1, "123", "jti");
        when(tokenClient.getToken(anyBoolean())).thenReturn(token);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_UNAUTHORIZED, "bad!"));
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream("this is mock!")).thenReturn(IOUtils.toInputStream("this is mock!"));
        when(closeableHttpClient.execute(any()))
                .thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        assertThatThrownBy(() -> auditTmsClient.fetchServiceInstance(TENANT)).isInstanceOf(TmsClientException.class);
        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verify(closeableHttpClient, times(2)).execute(any());
    }

    @Test
    public void fetchUnsuccessfullyDueTo401TokenTriesAgainForSharedTenant() throws IOException {
        Token token = new Token("1", "bearer", 1, "123", "jti");
        when(tokenClient.getToken(false)).thenReturn(token);
        when(tokenClient.getToken(true)).thenReturn(token);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_UNAUTHORIZED, "bad!"))
                .thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_NOT_FOUND, "bad!"));
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent())
                .thenReturn(IOUtils.toInputStream("Unauthorized")).thenReturn(IOUtils.toInputStream("this is mock!"));
        when(closeableHttpClient.execute(any()))
                .thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        Optional<TmsServiceInstance<AuditServiceCredentials>> tmsServiceInstance = auditTmsClient.fetchServiceInstance(TENANT);
        assertFalse(tmsServiceInstance.isPresent());

        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
    }

    @Test
    public void fetchUnsuccessfullyDueTo401TokenTriesAgainForDedicatedTenant() throws IOException {
        when(tokenClient.getToken(anyBoolean())).thenReturn(new Token("1", "bearer", 1, "123", "jti"));
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_UNAUTHORIZED, "bad!"))
                .thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_OK, "good!"));
        HttpEntity entity = mock(HttpEntity.class);

        TmsServiceInstance serviceInstance = TmsServiceInstance.builder()
                .serviceInstanceName("predix-audit")
                .serviceInstanceUuid("12345")
                .canonicalServiceName(TARGET)
                .serviceName(TARGET)
                .createdBy("kuku")
                .createdOn(System.currentTimeMillis())
                .managed(false)
                .status("created")
                .credentials(AuditServiceCredentials.builder()
                        .auditPubClientScope(Collections.singletonList("scope"))
                        .auditQueryApiScope("1")
                        .auditQueryApiUrl("http://query.com")
                        .eventHubUri("123")
                        .eventHubZoneId("zoneId")
                        .tracingInterval(1000)
                        .tracingToken("1234=")
                        .tracingUrl("http://tracing.com")
                        .build())
                .build();
        when(entity.getContent())
                .thenReturn(IOUtils.toInputStream("Unauthorized"))
                .thenReturn(IOUtils.toInputStream(om.writeValueAsString(serviceInstance)));
        when(closeableHttpClient.execute(any()))
                .thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        Optional<TmsServiceInstance<AuditServiceCredentials>> tmsServiceInstance = auditTmsClient.fetchServiceInstance(TENANT);
        assertTrue(tmsServiceInstance.isPresent());
        assertEquals(serviceInstance, tmsServiceInstance.get());
        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
    }

    @Test
    public void parseErrorThrowsException () throws IOException {
        when(tokenClient.getToken(false)).thenReturn(new Token("1", "bearer", 1, "123", "jti"));
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), HttpStatus.SC_OK, "bad!"));
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream("should not parsed to token"));
        when(closeableHttpClient.execute(any()))
                .thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        assertThatThrownBy(() -> auditTmsClient.fetchServiceInstance(TENANT)).isInstanceOf(TmsClientException.class);
        verify(tokenClient).getToken(false);
        verify(tokenClient, never()).getToken(true);
    }
}