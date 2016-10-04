package transmart.rest.api

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.PluginSource
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.transmartproject.db.http.BusinessExceptionResolver
import org.transmartproject.db.ontology.http.BusinessExceptionController
import org.transmartproject.rest.TransmartRestApiGrailsPlugin
import org.transmartproject.rest.marshallers.MarshallersRegistrar
import org.transmartproject.rest.marshallers.TransmartRendererRegistry
import org.transmartproject.rest.misc.HandleAllExceptionsBeanFactoryPostProcessor

//@Import([TransmartRestApiGrailsPlugin.class])
@PluginSource
class Application extends GrailsAutoConfiguration {

    @Override
    Closure doWithSpring() {{ ->
        println "DO WITH SPRING"
        xmlns context: 'http://www.springframework.org/schema/context'

        context.'component-scan'('base-package': 'org.transmartproject.rest') {
            context.'include-filter'(
                    type: 'annotation',
                    expression: Component.canonicalName)
        }

        studyLoadingServiceProxy(ScopedProxyFactoryBean) {
            targetBeanName = 'studyLoadingService'
        }

        marshallersRegistrar(MarshallersRegistrar) {
            packageName = 'org.transmartproject.rest.marshallers'
        }
        rendererRegistry(TransmartRendererRegistry)

        businessExceptionController(BusinessExceptionController)

        businessExceptionResolver(BusinessExceptionResolver)

        handleAllExceptionsBeanFactoryPostProcessor(HandleAllExceptionsBeanFactoryPostProcessor)
    }}

    @Override
    void doWithApplicationContext() {
        // Force the bean being initialized
        applicationContext.getBean 'marshallersRegistrar'
    }

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}

