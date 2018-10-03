package com.ge.predix.audit.sdk.util;

import io.netty.util.internal.StringUtil;
public class RestUtils {

    public static final String APPLICATION_X_WWW_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String APPLICATION_JSON = "application/json";

    public static String generateAuthPath(String uaaPath) {
        String suffix = "/oauth/token";
        if (! StringUtil.isNullOrEmpty(uaaPath)) {
            if (uaaPath.endsWith("/")) {
                uaaPath = uaaPath.substring(0, uaaPath.length() -1);
            }
        } else {
            uaaPath = "";
        }
        if (! uaaPath.endsWith(suffix) ) {
            uaaPath += suffix;
        }
        return uaaPath;
    }

}
