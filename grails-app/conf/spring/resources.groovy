import grails.util.Environment
import org.springframework.aop.scope.ScopedProxyFactoryBean
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

    if (Environment.currentEnvironment == Environment.TEST) {
        h2Views(Class.forName('org.transmartproject.db.test.H2Views'))
    }
}
