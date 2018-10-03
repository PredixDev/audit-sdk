package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.exception.TokenException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TokenClientTest {

    private TokenClient tokenClient;
    private ObjectMapper om = new ObjectMapper();
    private CloseableHttpClient client = mock(CloseableHttpClient.class);

    @Before
    public void init() throws Exception {
        tokenClient = new TokenClient("http://uaa.com/oauth/token", "client", "secret", client, om);
    }

    @Test
    public void getTokenAndTokenIsCachedReturnToken() throws Exception {
        Token token = new Token("access_token", "bearer", 10000, "scopes", "jti");
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 200, "OK"));
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream(om.writeValueAsString(token)));
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        Token fetchedToken = tokenClient.getToken(false);
        Token cachedToken = tokenClient.getToken(false);

        assertEquals(token.getAccessToken(), fetchedToken.getAccessToken());
        assertEquals(token.getScope(), fetchedToken.getScope());
        assertEquals(token.getTokenType(), fetchedToken.getTokenType());
        assertEquals(token.getSecondsToExpire(), fetchedToken.getSecondsToExpire());
        assertEquals(fetchedToken, cachedToken);
        verify(client).execute(any(HttpHost.class), any(HttpPost.class));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void getTokenForceAndTokenIsCachedReturnNewToken() throws Exception {
        Token token = new Token("access_token", "bearer", 10000, "scopes", "jti");
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 200, "OK"));
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        HttpEntity entity = mock(HttpEntity.class);
        String tokenContent = om.writeValueAsString(token);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream(tokenContent)).thenReturn(IOUtils.toInputStream(tokenContent));
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        Token cached = tokenClient.getToken(false);
        Thread.sleep(1000);
        Token fetched = tokenClient.getToken(true);

        Lists.newArrayList(cached, fetched).forEach(t-> {
            assertEquals(token.getAccessToken(), t.getAccessToken());
            assertEquals(token.getScope(), t.getScope());
            assertEquals(token.getTokenType(), t.getTokenType());
            assertEquals(token.getSecondsToExpire(), t.getSecondsToExpire());
        });
        assertNotEquals(cached, fetched);
        verify(client, times(2)).execute(any(HttpHost.class), any(HttpPost.class));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void getTokenAndCachedTokenExpiredFetchingNewToken() throws Exception {
        Token expired = new Token("access_token", "bearer", 0, "scopes", "jti");
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 200, "OK"));
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream(om.writeValueAsString(expired)))
        .thenReturn(IOUtils.toInputStream(om.writeValueAsString(expired)));
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        tokenClient.getToken(false);
        Thread.sleep(1000);
        tokenClient.getToken(false);

        verify(client, times(2)).execute(any(HttpHost.class), any(HttpPost.class));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void fetchTokenWrongCredentialsThrowsTokenException() throws Exception {
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 401, "Unauthorized"));

        assertThatThrownBy(() -> tokenClient.getToken(false)).isInstanceOf(TokenException.class);
    }

    @Test
    public void fetchTokenParsingErrorThrowsTokenException() throws Exception {
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(client.execute(any(HttpHost.class), any(HttpPost.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 200, "OK"));
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(IOUtils.toInputStream("kuku"));
        when(closeableHttpResponse.getEntity()).thenReturn(entity);

        assertThatThrownBy(() -> tokenClient.getToken(false)).isInstanceOf(TokenException.class);
    }
}