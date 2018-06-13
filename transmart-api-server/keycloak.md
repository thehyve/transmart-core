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
    
        EXP:SCSCP|AGGREGATE_WITH_THRESHOLD
        EXP:SCSCP|OWN
        
    The convention is to represent study permissions as `study token|minimal access level`
    See `org.transmartproject.core.users.AccessLevel` for the list of supported levels.
    The study token is picked during data uploading.
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
    

## Point tranmsart to the keycloak

Your `application.yml` file has to have following settings. Replace names in braces with your data.

       security:
           oauth2:
               client:
                   clientId: <transmart>
               resource:
                   userInfoUri: https://<domain>/auth/realms/<dev>/protocol/openid-connect/userinfo
