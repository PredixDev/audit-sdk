package com.ge.predix.audit.sdk.config.vcap;


import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.VcapLoadException;

/**
 * Created by 212584872 on 1/8/2017.
 */
public interface VcapLoaderService {
    VcapServices getVcapServices();
    VcapApplication getVcapApplication();
    AuditConfiguration getConfigFromVcap() throws VcapLoadException;
    VcapApplication getApplicationFromVcap() throws VcapLoadException;
}
