package com.ge.predix.audit.sdk.util;

import io.netty.util.internal.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by 212582776 on 3/16/2018.
 */
public class EnvUtils {

    static Map<String, String> variables = new HashMap<>();

    public static String getEnvironmentVar (String envVarName){
            return Optional.ofNullable(envVarName)
                    .map( (v) -> {
                        if (variables.containsKey(v)) {
                                return variables.get(v);
                        }
                        String var = System.getenv(envVarName);
                        variables.put(envVarName, var);
                        return var;})
                    .orElse(null);
    }

    public static <T> T mapExistingEnvironmentVar(String key, Function<String, T> mapper) {
        return Optional.ofNullable(EnvUtils.getEnvironmentVar(key))
                .filter(v -> !StringUtil.isNullOrEmpty(v))
                .map(mapper)
                .orElse(null);
    }

}
