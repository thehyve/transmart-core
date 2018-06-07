# How to set up transmart to work with keycloak.

##Keycloak configuration

Below are steps on how to set keycloak for transmart using the admin console.
The prerequisite is to have admin credentials to a keycloak instance. https://<domain>/auth/admin/

1. Create a realm. e.g. Dev
2. Create a client. e.g. transmart

    - Client Protocol: openid-conect
    - Access Type: confidential
    - Standard Flow Enabled: On
    - Valid Redirect URIs: https://<client url. e.g. glowing bear>
    - Web Origins: https://<client url. e.g. glowing bear>
    
3. Create Roles.  
    **Note:** not realm roles, but client roles.
    Follow Clints > Roles (tab)
    
    -Roles
    
        ROLE_ADMIN
        
    -Study permissions
    
        SHOW_SUMMARY_STATISTICS|EXP:SCSCP
        READ|EXP:SCSCP
        
    The convention is to represent study permissions as `operation|study token`
    See `org.transmartproject.core.users.ProtectedOperation.WellKnownOperations` for list of supported operations.
    Teh study token is decided during data uploading.
    Here `EXP:SCSCP` is token for `SHARED_CONCEPTS_STUDY_C_PRIV` test study.
    The token could be simply the study id. 
    
        
4. Add user

5. Set credentials for the user.

Provide new password and click "Reset Password"

6. Give user some roles.

Go to "Role Mappings" tab. Then select "Client Roles" to be "transmart"
Assign some roles.

7. Set up authorities mapping.

It has to be done once per client.

Go to `Clients > transmart > Mappers > Create Protocol Mappers`
Set the fields as following:

    Name: authorities
    Mapper Type: User Client Role
    Client: transmart
    Multivalued: On
    Token Claim Name: authorities
    Claim JSON Type: String
    
## Create an offline token

In order to access Keycloak offline (e.g. by offline quartz jobs from transmart-notifications), an offline_token has to be configured. To get the token on behalf of a user, the user needs to have the role mapping for the realm-level: `"offline_access"`.

    curl \
      -d 'client_id=<CLIENT_ID>' \
      -d 'username=<USERNAME>' \
      -d 'password=<PASSWORD>' \
      -d 'grant_type=password' \
      -d 'scope=offline_access' \
      'https://YOUR_KEYCLOAK_SERVER_HOST/auth/realms/YOUR_REALM/protocol/openid-connect/token'


This token is used as an Refresh token, but an offline token will never expire. The token has to be configured in `application.yml` file:

       keycloak:
           offlineToken: <your-offline-token>

## Point tranmsart to the keycloak

Your `application.yml` file has to have following settings. Replace names in braces with your data.

       security:
           oauth2:
               client:
                   clientId: <transmart>
               resource:
                   userInfoUri: https://<domain>/auth/realms/<dev>/protocol/openid-connect/userinfo
