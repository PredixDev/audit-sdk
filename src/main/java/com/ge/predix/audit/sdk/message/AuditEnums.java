package com.ge.predix.audit.sdk.message;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Created by 212584872 on 12/23/2016.
 */
@EqualsAndHashCode
public class AuditEnums implements Serializable {
    public enum Classifier implements Serializable {
        SUCCESS, FAILURE
    }

    public enum PublisherType implements Serializable {
        NETWORK_DEVICE, DB_SYSTEM, APP_SERVICE, OS;
    }

    public enum CategoryType implements Serializable {
        //Commons to anyone
        AUDIT_ACCOUNTABILITY, OPERATIONS, ADMINISTRATIONS, AUTHENTICATIONS, AUTHORIZATION, MALICIOUS

        //Only DB
        , DATA_INTEGRITY

        //Only APP
        , API_CALLS,
    }

    public enum EventType implements Serializable {
        CUSTOM
        //--------------AUDIT_ACCOUNTABILITY----------
        ,LOG_START, LOG_STOP, LOG_DELETION, LOG_DEACTIVATION, LOG_MODIFICATION

        //-------------------OPERATIONS---------------
        //valid to NETWORK_DEVICE, DB_SYSTEM, OS, APP_SERVICE (everyone)
        , UNAVAILABILITY, EXCEPTION, SERIOUS_ERROR

        //valid to NETWORK_DEVICE,DB_SYSTEM,APP_SERVICE
        , STARTUP_EVENT, SHUTDOWN_EVENT

        //valid to NETWORK_DEVICE , OS
        , START_SERVICE, STOP_SERVICE

        //-------------------ADMINISTRATIONS-------------
        //valid to NETWORK_DEVICE,DB_SYSTEM , OS, APP_SERVICE
        , ACCOUNT_PRIVILEGE_SUCCESS_MODIFICATION, ACCOUNT_PRIVILEGE_FAILURE_MODIFICATION, ADD_ADMIN_ACCOUNT, CHANGE_PASSWD_SUCCESS, CHANGE_PASSWD_FAILURE

        //valid to DB_SYSTEM , OS, APP_SERVICE
        , CHANGE_CONFIGURATIONS_SUCCESS, CHANGE_CONFIGURATIONS_FAILURE

        //valid to APP_SERVICE , OS,
        , ADD_ADMIN_GROUP_ACCOUNT

        //valid to NETWORK_DEVICE
        , CHANGE_CONFIGURATIONS

        //valid to DB_SYSTEM,
        , ADD_ROLE, REMOVE_ROLE

        //valid to OS
        , SECURITY_POLICY_CHANGE_SUCCESS, SECURITY_POLICY_CHANGE_FAILURE

        //-----------------AUTHENTICATIONS---------------
        //valid to NETWORK_DEVICE, DB ,APP_SERVICE , OS
        , LOGIN_SUCCESS, LOGIN_FAILURE, ACCOUNT_LOCKOUT, AUTHENTICATION_ERROR

        //valid to NETWORK_DEVICE
        , VPN_CONNECTION_ESTABLISHED_SUCCESS, VPN_CONNECTION_ESTABLISHED_FAILURE


        //-------------------AUTHORIZATION--------------
        //valid to NETWORK_DEVICE, DB, APP_SERVICE, OS
        , CHANGE_CRITICAL_FILE, PRIVILEGE_ACCOUNT_ACTION

        //valid to NETWORK_DEVICE, APP_SERVICE,OS
        , CHANGE_CRITICAL_RESOURCE

        //valid to NETWORK_DEVICE
        , INBOUND_CONNECTION_DENIED, OUTBOUND_CONNECTION_DENIED


        //-------------------MALICIOUS-------------- (OS has no malicious events)
        //valid to APP_SERVICE
        , INVALID_INPUTS, INVALID_APP_ABUSE, COMPONENT_INSTALLATION, COMPONENT_MODIFICATION, COMPONENT_DELETION

        //valid to NETWORK_DEVICE
        , DOS_ATTACK, EAVSDROPPING_ATTACK, USER_UNAPPROVED_OUTBOUND_TRAFFIC

        //valid to DB
        , VIRUS_ALERT, MALWARE_ALERT


        //------------------DATA_INTEGRITY---------------- (Only DB)
        , ACTION, CREATE, TRIGGER, DROP, INSERT, UPDATE, DELETE

        //-----------------------API_CALLS--------------- (Only APP_SERVICE)
        // Successful and unsuccessful API requests, Identity of the API caller, the time of the API call, the source IP address, request parameters, response elements
        , SUCCESS_API_REQUEST, FAILURE_API_REQUEST

    }



}
