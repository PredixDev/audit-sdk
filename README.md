<a href="../../../../pages/adoption/audit-sdk/javadocs/index.html" target="\_blank" >
	<img height="50px" width="100px" src="images/javadoc.png" alt="view javadoc"></a>

<a href="../../../../pages/adoption/audit-sdk" target="\_blank">
	<img height="50px" width="100px" src="images/pages.jpg" alt="view github pages">
</a>

# Audit-SDK

Audit Service Java SDK

Use the SDK to publish audit messages using the Audit client.

----------

## How to use the Audit Service SDK
- Add the below dependency to your maven pom XML file
```
    <dependency>
      <groupId>com.ge.predix</groupId>
      <artifactId>audit-sdk</artifactId>
      <version>1.3.0</version>
    </dependency>
```

### Supported Audit Clients
Audit SDK supports two types of clients:
 1. Multi-Tenancy backed by a single audit service instance
    - [Asynchronous Audit Client](https://github.com/PredixDev/audit-sdk/wiki/Asynchronous-Audit-Client)
    - [Synchronous Audit Client](https://github.com/PredixDev/audit-sdk/wiki/Synchronized-Audit-Client)
 2. Multi-Tenancy backed by Audit service instance per TMS tenant
    - [Asynchronous Audit Client With TMS](https://github.com/PredixDev/audit-sdk/wiki/Audit-Client-With-TMS)


### Additional information about the api
- [Additional Configurations](https://github.com/PredixDev/audit-sdk/wiki/Additional-Configuration)
- https://docs.predix.io/en-US/content/service/security/audit/
- https://docs.predix.io/en-US/content/service/security/audit/using-predix-audit-service

<<<<<<< HEAD
 The results of the audit operation, as well as other client related info are propagated through the AuditCallback.

 You should implement the AuditCallback interface and supply the auditClient with the implementation.
 ```
 public AuditCallback auditCallback(){
          return new AuditCallback() {
              @Override
              public void onValidate(AuditEvent auditEvent, List<ValidatorReport> list) {
                  log.info("onValidate {}", list);
              }

              @Override
              public void onFailure(AuditEvent auditEvent, FailReport failReport, String description) {
                  log.info("onFailure {} \n {} \n {}", failReport, auditEvent, description);
              }

              @Override
              public void onFailure(FailReport failReport, String description) {
                  log.info("onFailure {} \n {}", failReport, description);
              }

              @Override
              public void onSuccees(AuditEvent auditEvent) {
                  log.info("onSuccees {}", auditEvent);
              }
          };
      }
 ```

 ##### Synchronous client
 The results of the audit operation are returned with the AuditingResult. It contains the following elements:
 ```
 	List<AuditEventFailReport> failedEvents;
 	List<AuditEvent> sentEvents;
 ```
 Each `AuditEventFailReport` contains the failed event, a `FailReport` status code, and a failure description.

 `FailReport` indicates the type of error encountered.

 When returned with an event (i.e. `onFailure(event, failReport, description)` ) it can take one of the following values:
 + 'BAD_ACK' - NACK was received. The description contains the ack details.
 + 'NO_ACK' - no ACK was received after max retry attempts was made.
 + 'CACHE_IS_FULL' - the Audit cache is full and the event had to be dropped.


#### Additional API

##### Reconnect option
 Occasionally, audit client can be disconnected. In such a case, you receive an ```onFailure``` callback with ```FailReport.STREAM_IS_CLOSE```
 **This notification is currently available only for ASYNC mode**.

 There are two ways to handle reconnection:
 + MANUAL (default) - the `auditClient.reconnect()` API should be used to re-establish the connection.
 + AUTOMATIC - no API call is needed. the sdk automatically reconnects until the connection is restored.

 Use the configuration to change the reconnect mode:
```
AuditConfiguration.builder()
    .reconnectMode(ReconnectMode.AUTOMATIC)
    .build()
```
##### Authentication token
The sdk supports two modes of authentication:
 - Using a UAA user and pass. Use the `AuditConfiguration.builder()` to obtain a regular, UAA based configuration.
 - Using an authentication token. Use the `AuditConfiguration.builderWithAuthToken()` to obtain an auth token based configuration.

 Once a client was created, its authentication mode cannot be changed.
 When working with authentication token, you might need to refresh the token. This can be achieved by running the `auditClient.setAuthToken(token)` API.

 Please note that this API throws IllegalStateException if executed on a client configured with UAA authentication mode.

----
## Additional information

##### Debug flag
You can enable audit debug log level by setting `AUDIT_DEBUG_ENABLED: true` in your environment.

##### Tracing abilities
The SDK audits a marked message once every few minutes. The message is not sent to your DB and is for debugging purposes. This feature may be disabled in the configuration:
```
AuditConfiguration.builder()
    .traceEnabled(false)
    .build()
```
##### Auditing Application Name
When building an auditEvent (V2 and up), you can add the auditing application name. This is a friendly name that can take a String up to 100 characters.
```
AuditConfiguration.builder()
    .appName("application name")
    .build()
```
If APPLICATION_NAME variable was set in the environment, it will be added to each message and there is no need to explicitly provide it when building the message.

#### For more information about the api: https://docs.predix.io/en-US/content/service/security/audit/using-predix-audit-service

----
### Demo Projects
- https://github.com/PredixDev/samplePublisherApp
- https://github.com/PredixDev/samplePub-POJO

[![Analytics](https://ga-beacon.appspot.com/UA-82773213-1/audit-sdk/readme?pixel)](https://github.com/PredixDev)
=======
