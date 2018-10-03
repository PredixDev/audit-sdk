package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.ReconnectMode;

/**
 * Created by 212582776 on 2/20/2018.
 */
public class ReconnectStrategyFactory {

    //TODO check manual mode
    public static ReconnectStrategy getReconnectStrategy(ReconnectMode reconnectMode, Runnable reconnectFunc, String logPrefix){
        switch(reconnectMode){
            case AUTOMATIC:{
                return new ExponentialReconnectStrategy(reconnectFunc, logPrefix);
            }
            case MANUAL:
            default: {
                return (auditState) -> {};
            }

        }
    }
}
