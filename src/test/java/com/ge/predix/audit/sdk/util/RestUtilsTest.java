package com.ge.predix.audit.sdk.util;

import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class RestUtilsTest {

    @Test
    public void generateAuthPathPathIsAlreadyEnhanced() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/oauth/token");
        assertEquals("/oauth/token", uriBuilder.getPath());

        assertEquals("/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathPathIsAlreadyEnhancedWithSlash() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/oauth/token/");
        assertEquals("/oauth/token/", uriBuilder.getPath());

        assertEquals("/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathPathIsAlreadyEnhancedWithOtherPath() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/kuku/oauth/token");
        assertEquals("/kuku/oauth/token", uriBuilder.getPath());

        assertEquals("/kuku/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathPathIsAlreadyEnhancedWithSlashAndOtherPath() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/kuku/oauth/token/");
        assertEquals("/kuku/oauth/token/", uriBuilder.getPath());

        assertEquals("/kuku/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathEmptyPath() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com");
        assertEquals("", uriBuilder.getPath());

        assertEquals("/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathEndWithSlash() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/");
        assertEquals("/", uriBuilder.getPath());

        assertEquals("/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathEndWithOtherThing() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/kuku");
        assertEquals("/kuku", uriBuilder.getPath());

        assertEquals("/kuku/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathEndWithOtherThingAndSlash() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/kuku/");
        assertEquals("/kuku/", uriBuilder.getPath());

        assertEquals("/kuku/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }

    @Test
    public void generateAuthPathPathIsAlreadyOK() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://kuku.com/kuku/oauth/token");
        assertEquals("/kuku/oauth/token", uriBuilder.getPath());

        assertEquals("/kuku/oauth/token", RestUtils.generateAuthPath(uriBuilder.getPath()));
    }


}