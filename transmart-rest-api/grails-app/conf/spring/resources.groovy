import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.web.filter.CharacterEncodingFilter
import org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean
import org.codehaus.groovy.grails.plugins.GrailsPluginManagerFactoryBean
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator

beans = {
    handleAllExceptionsBeanFactoryPostProcessor(HandleAllExceptionsBeanFactoryPostProcessor)
    grailsApplication(GrailsApplicationFactoryBean)  {
        grailsDescriptor = ['/WEB-INF/grails.xml']
        grailsResourceLoader = ref('grailsResourceLoader')
    }
    pluginManager(GrailsPluginManagerFactoryBean)  {
        grailsDescriptor = ['/WEB-INF/grails.xml']
        application = ref('grailsApplication')
    }

    grailsConfigurator(GrailsRuntimeConfigurator, grailsApplication)  {
        pluginManager = ref('pluginManager')
    }

    grailsResourceLoader(GrailsResourceLoaderFactoryBean)

    characterEncodingFilter(CharacterEncodingFilter)  {
        encoding = 'utf-8'
    }
    conversionService(ConversionServiceFactoryBean)
}

class HandleAllExceptionsBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory
                .getBeanDefinition('businessExceptionResolver')
                .propertyValues
                .addPropertyValue('handleAll', true)
    }
}
