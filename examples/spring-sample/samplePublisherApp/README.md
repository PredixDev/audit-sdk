
# Sample Publisher App
This application is a sample publisher demonstrating how to use and run Audit-SDK.


### How to use the app?

 1. Follow instructions to create UAA Service, clients and etc. [here](https://docs.predix.io/en-US/content/service/security/audit/getting-started-with-predix-audit#IODQyYmQ5ZjUtNTc3My00ZDBlLThmZGMtMjQ1MzQzYjc2YTlj)
 2. Update the following in the manifest.yml:
 	* "services" - Name of your audit service instance (this is of automaticlly binding)
 	* AUDIT_SERVICE_NAME - Name of your audit service defenition. i.e: "predix-audit"
 	* AUDIT_UAA_URL - URL of your Audit UAA 
 	* AUDIT_UAA_CLIENT_SECRET - Client secret of the above uaa
 	* AUDIT_UAA_CLIENT_ID - Client ID of the above uaa
 3. Do mvn clean install
 4. Push the application: ```cf push```


That's it. 
Browse to the app URL with ```/publishAsync``` (GET request) to publish a message
Browse to the app URL with ```/getLastResponse``` (GET request) to see the last response


[![Analytics](https://predix-beacon.appspot.com/UA-82773213-1/predixcli/readme?pixel)](https://github.com/PredixDev)