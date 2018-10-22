package com.ge.predix.audit.sdk.util;

import com.ge.predix.audit.sdk.message.AuditEventV2;

import java.lang.reflect.Field;

/**
 * Created by 212554562 on 9/6/2018.
 */
public class ReflectionUtils {

    public static void modifyPrivateProperty(AuditEventV2 wrongVersionEvent , String property , Object value) {
        try {
            Field f = wrongVersionEvent.getClass().getSuperclass().getDeclaredField(property);
            f.setAccessible(true);
            f.set(wrongVersionEvent, value);
        } catch (NoSuchFieldException|IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
