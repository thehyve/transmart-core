import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.transmartproject.webservices.*

beans = {
    xmlns context: 'http://www.springframework.org/schema/context'

    studyLoadingServiceProxy(ScopedProxyFactoryBean) {
        targetBeanName = 'studyLoadingService'
    }

    transmartMarshallerRegistrar(TransmartMarshallerRegistrar) {
        marshallers = [
                new OntologyTermJsonMarshaller(),
                new ObservationJsonMarshaller(),
                new SubjectJsonMarshaller(),
                new ConceptDimensionJsonMarshaller(),
                new StudyJsonMarshaller(),
        ]
    }
    userDetailsService(com.recomdata.security.AuthUserDetailsService)
}
