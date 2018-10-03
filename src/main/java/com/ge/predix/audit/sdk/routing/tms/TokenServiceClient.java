package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.exception.TokenException;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import org.apache.commons.codec.binary.Base64;
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

import javax.validation.constraints.NotEmpty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static com.ge.predix.audit.sdk.util.RestUtils.*;

/**
 * Class to fetch a Token of a given tenantId for a service instance of that tenantId from TMS tokenService
 */
public class TokenServiceClient {

    private static CustomLogger log = LoggerUtils.getLogger(TokenServiceClient.class.getName());

    private final CloseableHttpClient client;
    private final URIBuilder tokenServiceUriBuilder;
    private final ObjectMapper om;
    private final String systemClientId;
    private final String systemClientSecret;

    /**
     * @param client closableHttpClient to hit the tokenService
     * @param tokenServiceUrl url of tokenService in the following format: http://tokenService.../oauth/token
     * @param om Object mapper to parse {@link Token} response
     * @param systemClientId client id to hit tokenService (usually it will be STUF client id)
     * @param systemClientSecret client secret to hit tokenService (usually it will be STUF client secret
     * @throws URISyntaxException when the URL of the tokenService is not valid.
     */
    public TokenServiceClient(CloseableHttpClient client, String tokenServiceUrl, ObjectMapper om, String systemClientId, String systemClientSecret)
            throws URISyntaxException {
        this.client = client;
        this.tokenServiceUriBuilder = new URIBuilder(tokenServiceUrl);
        this.om = om;
        this.systemClientId = systemClientId;
        this.systemClientSecret = systemClientSecret;
    }

    public Token getToken(String tenant, String canonicalServiceName) throws TokenException {
        try (CloseableHttpResponse httpResponse = executeRequest(tenant, canonicalServiceName);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent())))
        {
            String response = IOUtils.toString(bufferedReader);
            log.info("tokenService response for tenant %s is: {%s}", tenant, response);
            return om.readValue(response , Token.class);
        }
        catch (Exception e) {
            throw new TokenException(e, String.format("Failed to get token for tenant %s due to: %s", tenant, e.getMessage()));
        }
    }

    private CloseableHttpResponse executeRequest(String tenant, String target) throws IOException {
        HttpPost request = buildRequest(tenant, target);
        HttpHost host = new HttpHost(tokenServiceUriBuilder.getHost(), tokenServiceUriBuilder.getPort(), tokenServiceUriBuilder.getScheme());
        CloseableHttpResponse response = client.execute(host, request);
        int statusCode = response.getStatusLine().getStatusCode();
        if ( statusCode != HttpStatus.SC_OK ) {
            throw new TokenException(String.format("Got bad status from tokenService, tenant: %s statusCode %d statusLine %s", tenant, statusCode, response.getStatusLine()));
        }
        return response;
    }

    private HttpPost buildRequest(String tenant, String target) throws UnsupportedEncodingException {
        HttpPost request = new HttpPost(generateAuthPath(tokenServiceUriBuilder.getPath()));
        request.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("tenant", tenant),
                new BasicNameValuePair("target", target))
        ));
        request.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URL_ENCODED);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        request.setHeader(HttpHeaders.AUTHORIZATION, String.format("Basic %s",encode(systemClientId, systemClientSecret) ));
        log.info("tokenService request for tenant %s: {%s}, headers: {%s}", tenant, request, Arrays.toString(request.getAllHeaders()));
        return request;
    }

    private String encode(@NotEmpty String clientId, @NotEmpty String clientSecret) {
        String auth = String.format("%s:%s", clientId ,clientSecret);
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.defaultCharset()));
        return new String(encodedAuth);
    }


}
