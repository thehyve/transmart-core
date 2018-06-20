# TranSMART API server

The TranSMART API server is an application that provides a REST API
for a TranSMART database. User management and authorisation should be provided
by a separate identity provider. 

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

3. Create Roles.  
    **Note:** not realm roles, but client roles.
    Follow `Clients > Roles` (tab)

    - Global roles:

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

7. Set up authorities mapping.

    This has to be done once per client.
    
    Go to `Clients > transmart > Mappers > Create Protocol Mappers`
    Set the fields as following:

    - Name: `authorities`
    - Mapper Type: `User Client Role`
    - Client: `transmart`
    - Multivalued: `On`
    - Token Claim Name: `authorities`
    - Claim JSON Type: `String`
    

## Configure TranSMART to accept tokens from Keycloak

Create a file `transmart-api-server.config.yml` with the following settings (replace names in brackets with your data):
```yaml
security:
   oauth2:
       client:
           clientId: {transmart}
       resource:
           userInfoUri: https://{idp.example.com}/auth/realms/{dev}/protocol/openid-connect/userinfo
```

Start `transmart-api-server` with this configuration file:
```bash
java -jar -Dspring.config.location=transmart-api-server.config.yml transmart-api-server.war
```


[OpenID Connect]: https://openid.net/connect
[Keycloak]: https://www.keycloak.org
[PatientDataAccessLevel]: https://github.com/thehyve/transmart-core/blob/dev/transmart-core-api/src/main/groovy/org/transmartproject/core/users/PatientDataAccessLevel.groovy
