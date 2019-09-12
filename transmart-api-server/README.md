# TranSMART API server

The TranSMART API server is an application that provides a REST API
for a TranSMART database. It does not support the `v1` API. User management and authorisation should be provided
by a separate identity provider. 

## Patient counts threshold configuration

It is possible to configure a threshold value, below which counts are not available for users with `COUNTS_WITH_THRESHOLD` access permission to a study. In this case, instead of the counts, a special value: `-2` will be returned. The threshold will be applied only, when the specified query relates to the study with `COUNTS_WITH_THRESHOLD` permission and there is at least one patient from thi study as a result of this query.

```yaml
org.transmartproject.patientCountThreshold: 5
```

In order not to apply the threshold, set the value to 0.

## How to set up authentication for the API server

The TranSMART API server uses [OpenID Connect] for authorisation. 
In this document, we explain how to configure [Keycloak] as an identity provider for TranSMART
with Glowing Bear as user interface.
Let's assume these will be running at `https://transmart-api.example.com` and
 `https://glowingbear.example.com` and that a Keycloak instance is already available
  at `https://idp.example.com`.

### Keycloak configuration

Below are steps on how to set up Keycloak for TranSMART using the admin console.
The prerequisite is to have admin credentials to a Keycloak instance. 
Login to `https://idp.example.com/auth/admin/` and:

1. Create a realm, e.g., `dev`
2. Create a client, e.g., `transmart`
    - Client Protocol: `openid-connect`
    - Access Type: `confidential`
    - Standard Flow Enabled: `On`
    - Valid Redirect URIs: `https://glowingbear.example.com`
    - Web Origins: `https://glowingbear.example.com`

    **Note:** For Keycloak versions > 4.5.0 configure client mappers to include client ID in the aud (audience) Claim 
    by following [the official instruction](https://www.keycloak.org/docs/4.8/server_admin/#_audience_hardcoded).

3. Create Roles.  
    **Note:** not realm roles, but client roles.
    Follow `Clients > Roles` (tab)

    - Global roles:

        `ROLE_PUBLIC`
        `ROLE_ADMIN`

    - Study permissions:

        `EXP:SCSCP|COUNTS_WITH_THRESHOLD`
        `EXP:SCSCP|MEASUREMENTS`

    The convention is to represent study permissions as `study token|patient data access level`
    See [PatientDataAccessLevel] for the list of supported levels.
    The study token is loaded with study.
    Here, `EXP:SCSCP` is the study token for the `SHARED_CONCEPTS_STUDY_C_PRIV` test study.
    But the token could be simply the study id. 

4. Add user

5. Set credentials for the user.

    Provide new password and click `Reset Password`.

6. Give the user some roles.

    Go to `Role Mappings` tab. Then select `Client Roles` to be `transmart` and
    assign some roles.
    If TranSMART configured with the `denyAccessToUsersWithoutRole` setting,
    at least the `ROLE_PUBLIC` needs to be assigned for a user to be able to access any data.

## Configure TranSMART to accept tokens from Keycloak

Create an offline token in order to access Keycloak offline (e.g. by offline quartz jobs from transmart-notifications). To get the token on behalf of a user, the user needs to have the role mapping for the realm-level: `"offline_access"`.
```bash
    curl \
      -d 'client_id=<CLIENT_ID>' \
      -d 'username=<USERNAME>' \
      -d 'password=<PASSWORD>' \
      -d 'grant_type=password' \
      -d 'scope=offline_access' \
      'https://YOUR_KEYCLOAK_SERVER_HOST/auth/realms/YOUR_REALM/protocol/openid-connect/token'
```

You will get the following reply:
```json
{
  "access_token": "...",
  "expires_in": 480,
  "refresh_expires_in": 0,
  "refresh_token": "{the offline token}",
  "token_type": "bearer",
  "not-before-policy": 0,
  "session_state": "2b5e947f-4143-4e07-bf21-fc0871e3e335",
  "scope": "offline_access"
}
```

The value of the `refresh_token` field is the offline token.
It is used as an Refresh token, except it does not have the expiration date.

Note that the user needs to have the `view-users` rol for the `realm-management` client
to enable fetching of users with this token.

Create a file `transmart-api-server.config.yml` with the following settings (replace names in brackets with your data):
```yaml
keycloak:
    resource: {transmart}
    auth-server-url: https://{idp.example.com}/auth
    realm: {dev}
    bearer-only: true
    use-resource-role-mappings: true
    verify-token-audience: true

# to enable use of keycloak API to fetch list of users for jobs
keycloakOffline:
    offlineToken: {offlineToken}

# by default, users without any role are not denied access
org.transmartproject.security.denyAccessToUsersWithoutRole: false
```

Start `transmart-api-server` with this configuration file:
```bash
java -jar -Dspring.config.location=transmart-api-server.config.yml transmart-api-server.war
```

To use a custom logging configuration, provide the path of the `logback.groovy` file:
```bash
java -jar -Dlogging.config=/path/to/logback.groovy -Dspring.config.location=transmart-api-server.config.yml transmart-api-server.war
```
See [logback.groovy](grails-app/conf/logback.groovy) for the default logging configuration.

To disable writing logs to the database, add the following line to `transmart-api-server.config.yml`:
```yaml
org.transmartproject.system.writeLogToDatabase: false
```

To have the application create or update the database schemas at startup, add:
```yaml
grails.plugin.databasemigration.updateOnStart: true
``` 
This requires the configured database user to have permissions to create schemas and tables.
<br>*N.B.*: this only creates the essential tables from the `i2b2demodata`, `i2b2metadata` and `ts_batch` schemas.
E.g., high dimensional data tables and legacy user management tables are not included.


[OpenID Connect]: https://openid.net/connect
[Keycloak]: https://www.keycloak.org
[PatientDataAccessLevel]: https://github.com/thehyve/transmart-core/blob/dev/transmart-core-api/src/main/groovy/org/transmartproject/core/users/PatientDataAccessLevel.groovy
