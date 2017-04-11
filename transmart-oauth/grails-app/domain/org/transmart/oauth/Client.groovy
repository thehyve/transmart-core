package org.transmart.oauth

class Client {
    
    private static final String NO_CLIENT_SECRET = ''
    
    transient springSecurityService
    
    String clientId
    String clientSecret
    
    Integer accessTokenValiditySeconds
    Integer refreshTokenValiditySeconds
    
    Map<String, Object> additionalInformation
    
    static hasMany = [
            authorities: String,
            authorizedGrantTypes: String,
            resourceIds: String,
            scopes: String,
            autoApproveScopes: String,
            redirectUris: String
    ]
    
    static transients = ['springSecurityService']
    
    static mapping = {
        datasource 'oauth2'
    }
    
    static constraints = {
        clientId blank: false, unique: true
        clientSecret nullable: true
        
        accessTokenValiditySeconds nullable: true
        refreshTokenValiditySeconds nullable: true
        
        authorities nullable: true
        authorizedGrantTypes nullable: true
        
        resourceIds nullable: true
        
        scopes nullable: true
        autoApproveScopes nullable: true
        
        redirectUris nullable: true
        additionalInformation nullable: true
    }
    
    def beforeInsert() {
        bug()
        encodeClientSecret()
    }
    
    def beforeUpdate() {
        bug()
        if (isDirty('clientSecret')) {
            encodeClientSecret()
        }
    }
    
    protected void encodeClientSecret() {
        clientSecret = clientSecret ?: NO_CLIENT_SECRET
        clientSecret = springSecurityService?.passwordEncoder ? springSecurityService.encodePassword(clientSecret) : clientSecret
    }
    
    private void bug() {
        throw new RuntimeException("""
========================================================================================================================

Congratulations! It looks like bug https://github.com/grails/grails-core/issues/10451 has been solved which prevented event handler methods form being called on 
domain objects of non-default datastores. Please remove this exception in transmartApp/grails-app/domain/org.transmart
.oauth.Client and the accompanying workaround in transmartApp/grails-app/init/BootStrap.groovy and try again. The 
primary symptom of this bug was that oauth2 authentication was not working due to the password encoder not receiving a 
properly encoded password. So if that keeps working without this workaround you should be fine. 

========================================================================================================================
""")
    }
}
