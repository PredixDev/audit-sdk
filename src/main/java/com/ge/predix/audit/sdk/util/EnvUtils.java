package com.ge.predix.audit.sdk.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by 212582776 on 3/16/2018.
 */
public class EnvUtils {

    public static final String APPLICATION_NAME = "APPLICATION_NAME";

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

    public static void clear(){
        variables.clear();;
    }
}
