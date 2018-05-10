package com.ge.predix.audit.sdk;

import java.io.Serializable;

/**
 * Created by 212582776 on 2/20/2018.
 */
public enum AuthenticationMethod implements Serializable{

    /**
     * Used to indicate client was created using an authentication token.
     */
    AUTH_TOKEN,

    /**
     * Used to indicate client was created using Oauth provider details (UAA details)
     */
    UAA_USER_PASS
}