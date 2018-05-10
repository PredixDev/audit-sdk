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
public class VcapApplication{

    @SerializedName("application_id")
    private String appId;

    @SerializedName("application_name")
    private String appName;

    @SerializedName("application_uris")
    private List<String> appUris;

    @SerializedName("application_version")
    private String appVersion;

    @SerializedName("space_id")
    private String spaceId;

    @SerializedName("space_name")
    private String spaceName;

    private List<String> uris;
    private String version;
    private Limits limits;
    private String name;

}