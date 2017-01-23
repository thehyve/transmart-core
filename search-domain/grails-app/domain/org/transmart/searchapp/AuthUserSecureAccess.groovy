package org.transmart.searchapp

class AuthUserSecureAccess {
    static transients = ['objectAccessName', 'principalAccessName']

    Long id
    AuthUser authUser
    SecureObject secureObject
    SecureAccessLevel accessLevel
    String objectAccessName
    String principalAccessName

    static mapping = {
        table 'SEARCH_AUTH_USER_SEC_ACCESS_V'
        version false
        columns {
            id column: 'SEARCH_AUTH_USER_SEC_ACCESS_ID'
            authUser column: 'SEARCH_AUTH_USER_ID'
            secureObject column: 'SEARCH_SECURE_OBJECT_ID'
            accessLevel column: 'SEARCH_SEC_ACCESS_LEVEL_ID'
        }
    }

    static constraints = {
        authUser(nullable: true)
    }

    public String getObjectAccessName() {
        return secureObject.displayName + ' (' + accessLevel.accessLevelName + ')';
    }

    public void setObjectAccessName(String s) {}

    public String getPrincipalAccessName() {
        return authUser.name + ' (' + accessLevel.accessLevelName + ')';
    }

    public void setPrincipalAccessName(String s) {}
}
