package spring

import org.transmart.oauth.CurrentUserBeanFactoryBean
import org.transmart.oauth.CurrentUserBeanProxyFactory
import org.transmart.oauth.authentication.AuthUserDetailsService
import org.transmartproject.core.users.User

beans = {

    //overrides bean implementing GrailsUserDetailsService
    userDetailsService(AuthUserDetailsService)

    currentUserBean(CurrentUserBeanProxyFactory)
    "${CurrentUserBeanProxyFactory.SUB_BEAN_REQUEST}"(CurrentUserBeanFactoryBean) { bean ->
        bean.scope = 'request'
    }
    "${CurrentUserBeanProxyFactory.SUB_BEAN_QUARTZ}"(User) { bean ->
        // Spring never actually creates this bean
        bean.scope = 'quartz'
    }
}
