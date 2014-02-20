import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.transmartproject.db.test.H2Views
import org.transmartproject.rest.marshallers.MarshallersRegistrar

beans = {
    xmlns context: 'http://www.springframework.org/schema/context'

    studyLoadingServiceProxy(ScopedProxyFactoryBean) {
        targetBeanName = 'studyLoadingService'
    }

    marshallersRegistrar(MarshallersRegistrar) {
        packageName = 'org.transmartproject.rest.marshallers'
    }

    userDetailsService(com.recomdata.security.AuthUserDetailsService)

    h2Views(H2Views)

}
