import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.transaction.interceptor.TransactionInterceptor


import java.util.logging.Logger

def logger = Logger.getLogger('com.recomdata.conf.resources')

beans = {
    xmlns context: "http://www.springframework.org/schema/context"
    xmlns aop: "http://www.springframework.org/schema/aop"

    //oAuth2BaseService(SpringSecurityOauth2BaseService)

    sessionRegistry(SessionRegistryImpl)

    redirectStrategy(DefaultRedirectStrategy)
    accessDeniedHandler(AccessDeniedHandlerImpl) {
        errorPage = '/login'
    }
    failureHandler(SimpleUrlAuthenticationFailureHandler) {
        defaultFailureUrl = '/login'
    }

    transactionInterceptor(TransactionInterceptor) {
        transactionManagerBeanName = 'transactionManager'
        transactionAttributeSource = ref('transactionAttributeSource')
    }
}
