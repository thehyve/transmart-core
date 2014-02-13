package org.transmartproject.rest.marshallers

import grails.converters.JSON
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.TypeFilter

public class MarshallersRegistrar implements FactoryBean {

    @Autowired
    ApplicationContext ctx

    String packageName

    void scanForClasses() {
        ClassPathBeanDefinitionScanner scanner = new
        ClassPathBeanDefinitionScanner((BeanDefinitionRegistry)ctx, false) {

            @Override
            protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
                Set<BeanDefinitionHolder> superValue = super.doScan(basePackages)

                superValue.each { holder ->
                    def bean = ctx.getBean(holder.beanName)
                    JSON.registerObjectMarshaller(bean.targetType,
                            bean.&convert)
                }

                superValue
            }
        }
        scanner.addIncludeFilter(new AnnotationTypeFilter(JsonMarshaller))
        scanner.scan packageName
    }

    @Override
    Object getObject() throws Exception {
        scanForClasses()
        null /* we would have need this for the side effects */
    }

    final Class<?> objectType = null
    final boolean singleton = false
}

