package com.ge.predix.audit.sdk.routing.tms;

import com.ge.predix.audit.sdk.exception.TokenException;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class AppNameClientTest {
    private static final String APP_NAME = "application";
    private TokenClient tokenClient = mock(TokenClient.class);
    private String prefix = "stuf.app.";
    private AppNameClient client = new AppNameClient(tokenClient, prefix);

    private List<String> scopes = Lists.newArrayList(
            "predix-event-hub.zones.899aeff-adb6-4ce2-a89d-09fdc473ac3f.grpc.publish",
            "kuku",
            String.format("%s%s", prefix,APP_NAME));

    private Token token;
    @Before
    public void init () {

        StringBuilder allScopes = new StringBuilder("");
        scopes.forEach(s -> allScopes.append(String.format(" %s ", s)));
         token = new Token("kuku", "bearer",  1000, allScopes.toString(), "jti");
    }

    @Test
    public void getAppNameFirstTimeSuccess() {
        when(tokenClient.getToken(false)).thenReturn(token);

        String app = client.getAppName();

        assertEquals(APP_NAME, app);
    }

    @Test
    public void getAppNameMoreThanOneAppNameThrowsException() {
        token.setScope(String.format(" %s %skuku ", token.getScope(), prefix));
        when(tokenClient.getToken(anyBoolean())).thenReturn(token);

        assertThatThrownBy(() -> client.getAppName() ).isInstanceOf(TokenException.class);
        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verifyNoMoreInteractions(tokenClient);

    }

    @Test
    public void getAppNameEmptyAppNameThrowsException() {
        token.setScope(prefix);
        when(tokenClient.getToken(anyBoolean())).thenReturn(token);

        assertThatThrownBy(() -> client.getAppName() ).isInstanceOf(TokenException.class);
        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verifyNoMoreInteractions(tokenClient);
    }

    @Test
    public void getAppNameMoreThanOneAppNameTriesAgainWithSuccess() {
        token.setScope(String.format(" %s %skuku ", token.getScope(), prefix));
        when(tokenClient.getToken(eq(false))).thenReturn(token);
        when(tokenClient.getToken(eq(true))).thenReturn(new Token("kuku", "bearer",  1000, String.format("%s%s", prefix, APP_NAME), "jti"));

        assertEquals(APP_NAME, client.getAppName());
        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verifyNoMoreInteractions(tokenClient);
    }

    @Test
    public void getAppNameMoreThanOneAppNameIsEmptyTriesAgainWithSuccess() {
        token.setScope(prefix);
        when(tokenClient.getToken(eq(false))).thenReturn(token);
        when(tokenClient.getToken(eq(true))).thenReturn(new Token("kuku", "bearer",  1000, String.format("%s%s", prefix, APP_NAME), "jti"));

        assertEquals(APP_NAME, client.getAppName());
        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verifyNoMoreInteractions(tokenClient);
    }

    @Test
    public void getAppNameWithNullScopeThrowsException() {
        token.setScope(null);
        when(tokenClient.getToken(anyBoolean())).thenReturn(token);

        assertThatThrownBy(() -> client.getAppName() ).isInstanceOf(TokenException.class);

        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verifyNoMoreInteractions(tokenClient);
    }

    @Test
    public void getAppNameFirstTokenIsNullThrowsException() {
        when(tokenClient.getToken(false)).thenReturn(null);

        assertThatThrownBy(() -> client.getAppName() ).isInstanceOf(TokenException.class);
        verify(tokenClient).getToken(false);
        verify(tokenClient, never()).getToken(true);
    }

    @Test
    public void getAppNameFirstTimeThereAre2AppNamesAndInSecondTimeTokenIsNullThrowsException() {
        token.setScope(String.format(" %s %skuku ", token.getScope(), prefix));
        when(tokenClient.getToken(eq(false))).thenReturn(token);
        when(tokenClient.getToken(eq(true))).thenReturn(null);

        assertThatThrownBy(() -> client.getAppName() ).isInstanceOf(TokenException.class);

        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verifyNoMoreInteractions(tokenClient);
    }

    @Test
    public void getAppName0AppNameThrowsException() {
        token.setScope(scopes.get(0));
        when(tokenClient.getToken(anyBoolean())).thenReturn(token);

        assertThatThrownBy(() ->  client.getAppName()).isInstanceOf(TokenException.class);

        verify(tokenClient).getToken(false);
        verify(tokenClient).getToken(true);
        verifyNoMoreInteractions(tokenClient);
    }

    @Test
    public void getAppNameTokenClientThrowsExceptionThrowsException() {
        when(tokenClient.getToken(false)).thenThrow(new RuntimeException("kuku"));

        assertThatThrownBy(() -> client.getAppName() ).isInstanceOf(RuntimeException.class);

        verify(tokenClient).getToken(false);
        verify(tokenClient, never()).getToken(true);
        verifyNoMoreInteractions(tokenClient);
    }
}