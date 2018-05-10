package com.ge.predix.audit.sdk.config;

import java.io.Serializable;

/**
 * Created by 212582776 on 2/8/2018.
 */
public enum ReconnectMode implements Serializable{

    /**
     * User is expected to reconnect manually if desires, by calling the client.reconnect() API.
     */
    MANUAL,
    /**
     * The audit client will attempt to reconnect until the connection is established. This is done using an exponential backoff mechanism
     */
    AUTOMATIC

}
