package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.exception.TmsClientException;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;

import static com.ge.predix.audit.sdk.util.RestUtils.APPLICATION_JSON;

/**
 * Generic Class to fetch service instances from TMS
 */
@AllArgsConstructor
public class TmsClient {

    private static CustomLogger log = LoggerUtils.getLogger(TmsClient.class.getName());

    //TMS URL
    private final String tmsUrl;

    //Token client to fetch a Token to interact with TMS
    private final TokenClient tokenClient;

    //Http client to call TMS API
    private final CloseableHttpClient httpClient;

    //ObjectMapper to parse TMS response.
    private final ObjectMapper om;

    /**
     * Method to fetch service instance from TMS, the response type is Optional of {@link TmsServiceInstance}.
     * When the optional is empty, it means that TMS response was 404 (tenant/service instance not found)
     * When optional is present it means that TMS response was 200
     * @param tenantId tenantId in TMS
     * @param targetClass target class of service instance binding credentials.
     * @param canonicalServiceName the canonical service name of the service in TMS
     * @param <T> The class of the service instance binding credentials
     * @return service instance with Credentials of type T
     * @throws TmsClientException when TMS response is not 200/404, or some error occurred during the parsing.
     */
    public <T> Optional<TmsServiceInstance<T>> fetchServiceInstance(String tenantId, Class<T> targetClass, String canonicalServiceName) throws TmsClientException {
        try {
            TmsServiceInstanceResponse<T> response = getTmsServiceInstanceResponse(tenantId, targetClass, canonicalServiceName);
            return Optional.ofNullable(response.getTmsServiceInstance());
        } catch (Exception e) {
            throw new TmsClientException(e, String.format("Failed to get TmsServiceInstance %s of tenant %s from TMS due to %s",
                    canonicalServiceName, tenantId, e.getMessage()));
        }
    }

    private <T> TmsServiceInstanceResponse<T> getTmsServiceInstanceResponse(String tenantId, Class<T> targetClass, String canonicalServiceName)
            throws IOException, URISyntaxException {
        TmsServiceInstanceResponse<T> response = fetchFromTms(tenantId, targetClass, canonicalServiceName, false);
        int statusCode = response.getStatusCode();
        if (existingTokenIsBad(statusCode)) {
            log.info("TMS request for tenantId %s is %d, renew existing token to try again", tenantId, statusCode);
            response = fetchFromTms(tenantId, targetClass, canonicalServiceName, true);
            statusCode = response.getStatusCode();
        }
        validateResponse(response, statusCode);
        return response;
    }

    private boolean existingTokenIsBad(int statusCode) {
        return statusCode == HttpStatus.SC_FORBIDDEN || statusCode == HttpStatus.SC_UNAUTHORIZED;
    }

    private <T> void validateResponse(TmsServiceInstanceResponse<T> response, int statusCode) {
        if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NOT_FOUND) {
            throw new TmsClientException(String.format("Got bad status code: {%d}, Response: {%s}", statusCode, response));
        }
    }

    private <T> TmsServiceInstanceResponse<T> fetchFromTms(String tenantId, Class<T> targetClass, String canonicalServiceName, boolean shouldRefreshToken)
            throws IOException, URISyntaxException {
        try (CloseableHttpResponse httpResponse = executeRequest(tenantId, canonicalServiceName, shouldRefreshToken);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()))) {
            return parseResponse(IOUtils.toString(bufferedReader), httpResponse.getStatusLine().getStatusCode(), targetClass);
        }
    }

    private <T> TmsServiceInstanceResponse<T> parseResponse(String response, int statusCode, Class<T> targetClass) throws IOException {
        log.info("TMS response: {%s}, status: {%d}", response, statusCode);
        return TmsServiceInstanceResponse.<T>builder()
                .response(response)
                .statusCode(statusCode)
                .tmsServiceInstance(HttpStatus.SC_OK == statusCode? convertResponse(response, targetClass) : null)
                .build();
    }

    private <T> TmsServiceInstance<T> convertResponse(String response, Class<T> targetClass) throws IOException {
        JavaType type = om.getTypeFactory().constructParametricType(TmsServiceInstance.class, targetClass);
        return om.readValue(response, type);
    }

    private CloseableHttpResponse executeRequest(String tenantId, String canonicalServiceName, boolean shouldRefreshToken) throws IOException, URISyntaxException {
        return httpClient.execute(buildRequest(tenantId, canonicalServiceName, shouldRefreshToken));
    }

    private HttpGet buildRequest(String tenant, String canonicalServiceName, boolean shouldRefreshToken) throws URISyntaxException {
        HttpGet request = new HttpGet(new URIBuilder(String.format("%s/v1/tenants/%s/serviceinstances/%s", tmsUrl, tenant, canonicalServiceName)).build());
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        request.setHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", tokenClient.getToken(shouldRefreshToken).getAccessToken()));
        log.info("TMS request for tenant %s: {%s}, headers: {%s}", tenant, request, Arrays.toString(request.getAllHeaders()));
        return request;
    }

}
