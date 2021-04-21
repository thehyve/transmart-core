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
2. Create a client, e.g., `transmart-client`
    - Client Protocol: `openid-connect`
    - Access Type: `public`
    - Standard Flow Enabled: `On`
    - Valid Redirect URIs: `https://glowingbear.example.com/*`
    - Web Origins: `https://glowingbear.example.com`

    **Note:** For Keycloak versions > 4.5.0, configure client mappers to include client ID in the aud (audience) claim. 

    - Add a client mapper to include an `aud` (audience) claim to the token (see the [official Keycloak documentation](https://www.keycloak.org/docs/6.0/server_admin/#_audience_hardcoded)).
    - Go to `Clients`, select `transmart-client`, and select the `Mappers` tab.
      ![client mappers overview](images/client%20mappers%20overview.png)
    - Click `Create`, type name `transmart-client-audience`, select mapper type `Audience`, select the included client audience: `transmart-client`, and click `Save`.
      ![create client audience mapper](images/create%20client%20audience%20mapper.png)

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

Create a file `transmart-api-server.config.yml` with the following settings (replace names in brackets with your data):
```yaml
keycloak:
    resource: {transmart-client}
    auth-server-url: {https://idp.example.com/auth}
    realm: {dev}
    bearer-only: true
    use-resource-role-mappings: true
    verify-token-audience: true

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
