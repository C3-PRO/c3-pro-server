# C3-PRO-Server #

C3-PRO-Server is a highly reliable and scalable FHIR DSTU2 compliant web server, designed to cope with the traffic from mobile apps. The current version can only be deployed in AWS. It populates an AWS SQS with the FHIR resources that are POST. It does not consume the queue. A consumer can be found in the project [c3pro-consumer] (https://bitbucket.org/ipinyol/c3pro-consumer)

The system servers the following rest methods:

    HTTP/1.1 GET /c3pro/fhir/Questionnaire
    HTTP/1.1 POST /c3pro/fhir/QuestionnaireAnswer
    HTTP/1.1 POST /c3pro/fhir/Contract

It uses oauth2 two legged for authorization, which needs an initial phase for registration:

**Registration request:**

    HTTP/1.1 POST /c3pro/register
    HTTP/1.1 Header Antispam: {{in-app-stored secret}}
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

    HTTP/1.1 POST /c3pro/oauth?grant_type=client_credentials
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

# Configuration and Deployment #

## AWS prerequisites ##

The following services must be deployed in AWS:

* **S3 bucket**: The system uses an S3 bucket to serve static content like Questionnaire resources and to store the public key of the Consumer
* **SQS queue**: A queue to store the pushed FHIR resources
* **Oracle RDS DB**: the system uses an oracle schema to manage credentials. Technically, it is not necessary to use a db schema deployed in AWS, but is highly recommended.

The access to S3 and SQS can be configured in the {{config.properties}} of each resource directories (dev, qa and prod). The access to the oracle DB must be configured as a datasource in the jboss {{standalone.xml}} file. See below.

## Installing Maven, Java && JBoss AS7 ##

The system uses java 7 and we recommend to use JBoss AS7. To install the basic tools in a Debian-based Linux distribution:

    sudo apt-get clean
    sudo apt-get update
    sudo apt-get install openjdk-7-jdk
    sudo apt-get install unzip
    sudo apt-get install maven
    wget http://download.jboss.org/jbossas/7.1/jboss-as-7.1.1.Final/jboss-as-7.1.1.Final.zip
    sudo unzip jboss-as-7.1.1.Final.zip -d /usr/share/
    sudo chown -fR {{you_chosen_user}}:{{you_chosen_user}} /usr/share/jboss-as-7.1.1.Final/


## Oracle DB configuration ##

The systems uses an oracle DB to manage credentials and bearer token. Here are the steps to configure the DB properly:

* Run the table creation script: *{{src/main/scripts/create_tables.sql}}* in the DB
* Insert an antispam token:
    
```
#!sql
insert into AntiSpamToken (token) values ('{{the_token_hashed_with_sha1}}');
```

  To generate sha1 hashed token execute the script: *{{src/main/scripts/generate_hashed_token.sql}}* replacing *{{"REPLACE by a high entropy token"}}* by the desired anti spam token.

* Deploy the provided oracle jdbc driver in jBoss:

```
#!shell
$C3PRO_HOME/cp ojdbc14.jar $JBOSS_HOME/standalone/deployments
```

* Configure the data source by editing the file *$JBOSS_HOME/standalone/configuration/standalone.xml*. In the data source section place the following:

```
#!xml

<datasource jndi-name="java:jboss/datasources/c3proAuthDS" pool-name="c3proAuthDS" enabled="true" use-java-context="true">
    <connection-url>{{jdbc_connection_to_db}}</connection-url>
    <driver>ojdbc14.jar</driver>
    <security>
        <user-name>{{db_username}}</user-name>
        <password>{{db_password}}</password>
    </security>
</datasource>
```

* Configure OAuth2LoginModule by editing the file *$JBOSS_HOME/standalone/configuration/standalone.xml*, and adding the following in the security-domains section:


```
#!xml

<security-domain name="StaticUserPwd" cache-type="default">
    <authentication>
        <login-module code="org.bch.security.oauth.OAuth2LoginModule" flag="required">
            <module-option name="dsJndiName" value="java:jboss/datasources/c3proAuthDS"/>
            <module-option name="principalsQuery" value="select passwd from Users where username=?"/>
            <module-option name="rolesQuery" value="select userRoles, 'Roles' from UserRoles where username=?"/>
            <module-option name="hashAlgorithm" value="SHA1"/>
            <module-option name="hashEncoding" value="BASE64"/>
            <module-option name="hashCharset" value="UTF-8"/>
            <module-option name="hashUserPassword" value="true"/>
            <module-option name="hashStorePassword" value="false"/>
        </login-module>
    </authentication>
</security-domain>
```

## Building and deploying in DEV ##

Once the project is cloned or download, in the root of the project:

    mvn clean package
    mvn jboss-as:deploy

The previous instructions take the resource files located in *src/main/resources/dev* and place them as the resource files of the deployment. This requires JBoss on:

    $JBOSS_HOME/bin/standalone.sh

To stop JBoss:

    $JBOSS_HOME/bin/jboss-cli.sh --connect command=:shutdown


## Building in QA and PROD environment ##

In QA:

    mvn clean package -Pqa
    mvn jboss-as:deploy

In PROD:

    mvn clean package -Pprod
    mvn jboss-as:deploy

These commands take the resource files located in *src/main/resources/qa* or *src/main/resources/prod* respectively, and place them as the resource files of the deployment.

## Notes on AWS SDK usage ##

The system uses the Java SDK provided by Amazon. The SDK will be installed automatically since it is a maven dependency. However, it grabs the credentials to access the S3 bucket and SQS from a file that should be located here:

    ~/.aws/credentials

The content of the file should be something like:

    [sqsqueue]
    aws_access_key_id={{access_key_to_SQS_and_S3}}
    aws_secret_access_key={{secret}}

The default configuration uses the same profile to connect to S3 and SQS. This is specified in *configuration.properties* file. If you want to use different profiles, the *credentials* file should look like:

    [sqsprofile]
    aws_access_key_id={{access_key_to_SQS}}
    aws_secret_access_key={{secret_SQS}}

    [s3profile]
    aws_access_key_id={{access_key_to_S3}}
    aws_secret_access_key={{secret_S3}}

And the corresponding variables in *configuration.properties* would look like:

    app.aws.sqs.profile=sqsprofile    
    app.aws.s3.profile=s3profile
    
To obtain access keys and secrets from AWS, visit http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html. We suggest to create a user in AWS-IAM with only permissions to access S3 and SQS, and generate the access key and secret for this user.

## Providing public key ##

The system uses a public key uploaded in the S3 bucket to encrypt the symmetric key used to encrypt the resources in the SQS. The name of the public key file is specified in *configuration.propeties* file:

    app.security.publickey=public-c3pro.der

This name must match with an existing file in the used S3 bucket. The public key comes from the consumer. See https://bitbucket.org/ipinyol/c3pro-consumer to see how to generate public-private keys.