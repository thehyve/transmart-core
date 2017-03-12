import grails.plugin.springsecurity.oauth2.SpringSecurityOauth2BaseService
import org.transmart.authorization.CurrentUserBeanFactoryBean
import org.transmart.authorization.CurrentUserBeanProxyFactory
import org.transmartproject.core.users.User

beans = {
    currentUserBean(CurrentUserBeanProxyFactory)
    "${CurrentUserBeanProxyFactory.SUB_BEAN_REQUEST}"(CurrentUserBeanFactoryBean) { bean ->
        bean.scope = 'request'
    }
    "${CurrentUserBeanProxyFactory.SUB_BEAN_QUARTZ}"(User) { bean ->
        // Spring never actually creates this bean
        bean.scope = 'quartz'
    }

    //needed for spring-google plugin
    oAuth2BaseService(SpringSecurityOauth2BaseService)
}
