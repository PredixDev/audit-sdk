package com.ge.predix.audit.sdk.routing.tms;

import com.ge.predix.audit.sdk.exception.TokenException;
import io.netty.util.internal.StringUtil;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.ge.predix.audit.sdk.util.ExceptionUtils.swallowSupplierException;

/**
 * This class extract appName suffix from a given {@link Token}
 * i.e if you want to extract STUF feature name from a Token with stuf.app.origin
 */
@AllArgsConstructor
public class AppNameClient {

    //Token client to fetch a token to hit the tokenService
    private final TokenClient client;

    //Prefix to remove from the appName scope (stuf.app.)
    private final String appNamePrefix;

    /**
     * This method will fetch "application token" from trusted issuer using {@link TokenClient} and extract
     *      the feature name from the scopes.
     *  There could be only 1 scope with the given suffix
     *  usually the suffix will be the feature, and the prefix will be stuf.app.origin
     * @return appName which is feature name
     * @throws TokenException when the token cannot be fetched from the TokenClient
     *      or application or there is more than 1 scope with different appName in the Token
     */
    public String getAppName() {
        Token token = getToken(false);
        return swallowSupplierException(()->
                getAppNameFromToken(token), "Failed to get application name from application token, will try again with new token", Level.INFO)
                .orElseGet(()-> getAppNameFromToken(getToken(true)));
    }

    private Token getToken(boolean shouldFetchNew) {
        return Optional.ofNullable(client.getToken(shouldFetchNew))
                .orElseThrow(() -> new TokenException("Could not get application name due to null token"));
    }

    private String getAppNameFromToken(Token token) throws TokenException {
        List<String> apps = getApps(token);
        validateAppsSize(apps, token.getScope());
        String appName = convertAppNameScopeToAppName(apps);
        validateAppName(appName, token.getScope());
        return appName;
    }

    private List<String> getApps(Token token) {
        return token.scopes().stream()
                .filter(scope -> scope.startsWith(appNamePrefix))
                .collect(Collectors.toList());
    }

    private void validateAppsSize(List<String> apps, String scope) {
        if (apps.size() != 1) {
            throw new TokenException("Could not find unique %s<origin> scope in token, scope: {%s}",
                    appNamePrefix, scope);
        }
    }

    private String convertAppNameScopeToAppName(List<String> apps) {
        return apps.get(0).replace(appNamePrefix, StringUtil.EMPTY_STRING);
    }

    private void validateAppName(String appName, String scope) {
        if (StringUtil.isNullOrEmpty(appName)) {
            throw new TokenException("Could not find any %s<origin> scope in token because appName is null or empty, scope: {%s}",
                    appNamePrefix, scope);
        }
    }
}
