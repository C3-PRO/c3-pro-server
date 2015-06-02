# C3-PRO-Server #

C3-PRO-Server is a highly reliable and scalable FHIR DSTU2 compliant web server, designed to cope with the traffic from mobile apps. The current version can only be deployed in AWS. It populates an AWS SQS with the FHIR resources that are POST. It does not consume the queue. A consumer can be found in the project [c3pro-consumer] (https://bitbucket.org/ipinyol/c3pro-server)

The system servers the following rest methods:

  GET /c3pro/fhir/Questionnaire
  POST /c3pro/fhir/QuestionnaireAnswer
  POST /c3pro/fhir/Contract

It uses oauth2 two legged for authorization, which needs an initial phase for registration:

**Registration request:**

  POST /c3pro/register
  HTTP Header Antispam: {{in-app-stored secret}}
  {
    “sandbox”: true/false,
    “receipt-data”: {{your apple-supplied app purchase receipt}}
  }

**Registration response:**

  HTTP/1.1 201 Created
  Content-Type: application/json
  {
    "client_id":"{{some opaque client id}}",
    "client_secret": "{{some high-entropy client secret}}",
    "grant_types": ["client_credentials"],
    "token_endpoint_auth_method":"client_secret_basic",
  }

The registration phase should be called only once per device. Once the device is registered, the same client_id and client_secret must be user in future oauth calls.

**Oauth2 authorization request**

  POST /c3pro/oauth?grant_type=client_credentials
  Authentication: Basic BASE64(ClientId:Secret)

**Oauth2 authorization response**
  HTTP/1.1 201 Created
  Content-Type: application/json
  {
    "access_token":"{{some token}}",
    "expires_in": "{{seconds to expiration}}",
    "token_type": "bearer",
  }

The Bearer token can be used in the rest calls that serve FHIR resources as authorization credentials.

### AWS prerequisits ###
