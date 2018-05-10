package com.ge.predix.audit.sdk.config.vcap;

/**
 * Created by 212584872 on 1/8/2017.
 */

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class VcapServicesDeserializer implements JsonDeserializer<VcapServices> {

    @Setter
    private String auditServiceName;

    @Setter
    private Gson gson;

    public VcapServicesDeserializer(){
        this.auditServiceName = System.getenv("AUDIT_SERVICE_NAME");
        this.gson = new Gson();
    }

    @Override
    public VcapServices deserialize(
            JsonElement json,
            Type type,
            JsonDeserializationContext context)
            throws JsonParseException {
        if(json.isJsonObject()) {
            JsonElement jsonElement = ((JsonObject)json).get(auditServiceName);
            if (null != jsonElement) {
                Type listType = new TypeToken<ArrayList<AuditService>>(){}.getType();
                return new VcapServices(gson.fromJson(jsonElement, listType));
            }
        }
        return null;
    }
}