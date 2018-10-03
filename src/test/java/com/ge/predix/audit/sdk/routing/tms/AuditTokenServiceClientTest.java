package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.exception.TokenException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class AuditTokenServiceClientTest {

    private AuditTokenServiceClient auditTokenServiceClient;
    private ObjectMapper om = new ObjectMapper();
    private CloseableHttpClient client = mock(CloseableHttpClient.class);

    @Before
    public void init() throws Exception {
        auditTokenServiceClient = new AuditTokenServiceClient("predix-audit",
                new TokenServiceClient(client, "http://tokenService.com/oauth/token", om, "client", "secret"));
    }

    @Test
    public void getTokenReturnToken() throws Exception {
        Token token = new Token("access_token", "bearer", 10000, "scopes", "jti");
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 200, "OK"));
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream(om.writeValueAsString(token)));
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        Token fetchedToken = auditTokenServiceClient.getToken( "tnt");

        assertEquals(token.getAccessToken(), fetchedToken.getAccessToken());
        assertEquals(token.getScope(), fetchedToken.getScope());
        assertEquals(token.getTokenType(), fetchedToken.getTokenType());
        assertEquals(token.getSecondsToExpire(), fetchedToken.getSecondsToExpire());
        verify(client).execute(any(HttpHost.class), any(HttpPost.class));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void fetchTokenWrongCredentialsThrowsTokenException() throws Exception {
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream("kuku"));
        when(closeableHttpResponse.getEntity()).thenReturn(entity);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 401, "Unauthorized"));
        assertThatThrownBy(() -> auditTokenServiceClient.getToken("tnt")).isInstanceOf(TokenException.class);
    }

    @Test
    public void fetchTokenParseErrorThrowsTokenException() throws Exception {
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream("kuku"));
        when(closeableHttpResponse.getEntity()).thenReturn(entity);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), HttpStatus.SC_OK, "OK"));

        assertThatThrownBy(() -> auditTokenServiceClient.getToken("tnt")).isInstanceOf(TokenException.class);
    }

}