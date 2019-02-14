package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.exception.TokenException;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.ge.predix.audit.sdk.util.RestUtils.*;

/**
 * Http client that is able to fetch and cache Token from trusted Issuer.
 */
public class TokenClient {
    private static CustomLogger log = LoggerUtils.getLogger(TokenClient.class.getName());
    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

    private final String clientId;
    private final String clientSecret;
    private final CloseableHttpClient client;
    private final URIBuilder uaaUrl;
    private final ObjectMapper om;
    private Token token;

    /**
     * Instantiate the TokenClient.
     * @param uaaUrl the url of the trusted issuer in this format: http://issuer....com/oauth/token
     * @param clientId the client id to fetch the token
     * @param clientSecret the client secret to fetch the token
     * @param closeableHttpClient the http that will make the rest call
     * @param objectMapper to parse the token client to {@link Token} response
     * @throws URISyntaxException in case that the url of the trusted issuer is invalid.
     */
    public TokenClient(String uaaUrl, String clientId, String clientSecret, CloseableHttpClient closeableHttpClient, ObjectMapper objectMapper) throws URISyntaxException {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.uaaUrl = new URIBuilder(uaaUrl);
        this.client = closeableHttpClient;
        this.om = objectMapper;
        this.token = null;
    }

    /**
     * @param force if set to true then a new token will be retrieved
     * @return cached {@link Token} in case that the token was not fetched before, ot the token is going to be expired in 1 minute
     * @throws TokenException when response from the trusted issuer ended with response code that is different then 200OK
     */
    public synchronized Token getToken(boolean force) throws TokenException {
        if (expired() || force) {
            token = fetchToken();
        }
        return token;
    }

    private Token fetchToken() {
        try (CloseableHttpResponse httpResponse = executeRequest();
             BufferedReader bufferedReader = new BufferedReader(
                     new InputStreamReader(httpResponse.getEntity().getContent()))) {
            return om.readValue(IOUtils.toString(bufferedReader), Token.class);
        } catch (Exception e) {
            throw new TokenException(e, String.format("TokenClient failed to fetch token due to: %s", e.getMessage()));
        }
    }

    private boolean expired() {
        return (null == token || token.shouldExpire(ONE_MINUTE));
    }

    private CloseableHttpResponse executeRequest() throws IOException {
        HttpPost request = buildRequest();
        HttpHost target = new HttpHost(uaaUrl.getHost(), uaaUrl.getPort(), uaaUrl.getScheme());
        CloseableHttpResponse httpResponse =  client.execute(target, request);
        if(! (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)) {
            log.info("OAuth token request {%s} to target {%s} ended with the following response {%s} ", request, target, httpResponse);
            throw new TokenException(String.format("Token client could not fetch token due to bad status code, response {%s}", httpResponse));
        }
        return httpResponse;
    }

    private HttpPost buildRequest() throws UnsupportedEncodingException {
        HttpPost request = new HttpPost(generateAuthPath(uaaUrl.getPath()));
        request.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("client_id", clientId),
                new BasicNameValuePair("client_secret", clientSecret))
        ));
        request.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URL_ENCODED);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        return request;
    }

}
