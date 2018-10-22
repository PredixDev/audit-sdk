<a href="http://predixdev.github.io/audit-sdk/javadocs/index.html" target="\_blank" >
	<img height="50px" width="100px" src="images/javadoc.png" alt="view javadoc"></a>
&nbsp;
<a href="http://predixdev.github.io/audit-sdk" target="\_blank">
	<img height="50px" width="100px" src="images/pages.jpg" alt="view github pages">
</a>

# audit-sdk

Audit Service Java SDK

Use the SDK to publish audit messages using the Audit client.

----------

## How to use the Audit Service SDK
- Add the below dependency to your maven pom XML file
```
    <dependency>
      <groupId>com.ge.predix</groupId>
      <artifactId>audit-sdk</artifactId>
      <version>1.0.0</version>
    </dependency>
```

## Initializing the client

 Prior to creating the auditClient, you need to create an AuditConfiguration.

 With this configuration you can create a Sync or Async audit clients.

#### Audit Configuration
 The AuditConfiguration exposes provisioning parameters of the audit client.

 AuditConfiguration can be constructed manually or automatically:

 **Use the VcapLoaderServiceImpl to automatically create your configuration.**
 ```
 private VcapLoaderServiceImpl vcapLoaderService = new VcapLoaderServiceImpl();
 private AuditConfiguration auditConfiguration = vcapLoaderService.getConfigFromVcap();
 ```
 Automatic mode requires that your application will be bound to the predix-audit service instance.
 It also requires to set the following environment variables in you application:
 + `AUDIT_SERVICE_NAME`: The name of the AuditService in the cf marketplace.
 + `AUDIT_UAA_URL`: https://<UAA instance>.predix-uaa.run.aws-usw02-dev.ice.predix.io/oauth/token.
 + `AUDIT_UAA_CLIENT_ID`: The UAA client for Audit Service.
 + `AUDIT_UAA_CLIENT_SECRET`: The UAA secret of above client.

 Example manifest.yml:

```
    ---
    applications:
    - name: my-auditing-application
      instances: 1
      services:
        - my-audit-service
      env:
        AUDIT_SERVICE_NAME: predix-audit
        AUDIT_UAA_URL: https://2e6f8552-acb2-4d96-b470-0d44fe8cd33b.predix-uaa.run.asv-pr.ice.predix.io/oauth/token
        AUDIT_UAA_CLIENT_ID: client
        AUDIT_UAA_CLIENT_SECRET: secret
```

 Additional optional parameters can be supplied through VCAP to control the client configuration:
   + `AUDIT_MAX_RETRY_COUNT`: The number of attempts made to resend a failed audit.
     - Valid values: 0-5. default: 2.
   + `AUDIT_RETRY_INTERVAL_MILLIS`: The amount of time in milliseconds between each retry attempt.
     - Valid values: 2,000 - 20,000 (2 sec - 20 sec). default - 10,000.
   + `AUDIT_MAX_CACHED_EVENTS`: The maximum number of events cached im memory, waiting to be sent.
     - Default: 50,000. minimum value is 1000.
   + `AUDIT_RECONNECT_POLICY`: Controls whether the Audit client will attempt a reconnect when disconnected.
     - Valid values: MANUEL, AUTOMATIC. default: MANUEL - no reconnect attempt is made.

 **Use one of the `AuditConfiguration` builders to create the configuration manually:**.
  + `AuditConfigurationBuilder` (`AuditConfiguration.builder()`) to create a client with uaa user / pass for authentication.
  + `AuditConfigurationWithAuthTokenBuilder` (`AuditConfiguration.builderWithAuthToken()`) to create a client with Auth token.


#### Audit Client

 You can create two types of clients:
 - Sync client  - requires AuditConfiguration.
 - Async Client - requires AuditConfiguration and AuditCallback.

 You should create the client according to the desired mode.

 For async client:
 ```
 AuditClient auditClient = new AuditClient(configuration , callback)
 ```

 For sync client:
 ```
 AuditClientSync auditClientSync = new AuditClient(configuration)
 ```
 **Be aware AuditClientType.SYNC is deprecated and should not be used to indicate the client mode**

## Client API

The AuditClient and AuditClientSync share a similar API. they only differ in the way they expose the results of the audit operation.

#### Publishing audit events

To publish event you can use the
`auditClient.audit(AuditEvent)` or
`auditClient.audit(List<AuditEvent>)`

Here is an example:

   ```
        AuditEvent eventV2 = AuditEventV2.builder()
                .payload("This is test payload")
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .tenantUuid(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .appName(applicationName)
                .build();

        auditClient.audit(eventV2);
   ```

#### Handling the results

##### Asynchronous client

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

#### For more information about the api:
https://docs.predix.io/en-US/content/service/security/audit/using-predix-audit-service

----
# Example Projects:
- https://github.com/PredixDev/samplePublisherApp
- https://github.com/PredixDev/samplePub-POJO

[![Analytics](https://ga-beacon.appspot.com/UA-82773213-1/audit-sdk/readme?pixel)](https://github.com/PredixDev)
