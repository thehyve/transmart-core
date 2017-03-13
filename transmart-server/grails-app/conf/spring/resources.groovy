import grails.plugin.springsecurity.oauth2.SpringSecurityOauth2BaseService
import org.transmart.oauth.authentication.AuthUserDetailsService

beans = {

    //overrides bean implementing GrailsUserDetailsService
    userDetailsService(AuthUserDetailsService)
    //needed for spring-google plugin
    oAuth2BaseService(SpringSecurityOauth2BaseService)
}
