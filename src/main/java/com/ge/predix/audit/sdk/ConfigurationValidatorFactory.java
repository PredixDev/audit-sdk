package com.ge.predix.audit.sdk;

/**
 * Created by 212582776 on 2/20/2018.
 */
public class ConfigurationValidatorFactory {

    private static ConfigurationValidatorWithDeprecatedTypeSupport configurationValidatorImpl = new ConfigurationValidatorWithDeprecatedTypeSupport();

    public static ConfigurationValidator getConfigurationValidator(){
        return configurationValidatorImpl;
    }
}
