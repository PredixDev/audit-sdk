package com.ge.predix.audit.sdk.config.vcap;

/**
 * Created by 212584872 on 1/8/2017.
 */
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditService {
    private AuditServiceCredentials credentials;
    private String label;
    private String name;
    private String plan;
    private String provider;

    @SerializedName("syslog_drain_url")
    private String syslogDrainUrl;

    private List<String> tags;

    @SerializedName("volume_mounts")
    private List<String> volumeMounts;
}