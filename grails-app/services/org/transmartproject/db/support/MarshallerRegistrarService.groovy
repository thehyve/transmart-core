package org.transmartproject.db.support

import grails.converters.JSON
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.TypeFilter

public class MarshallerRegistrarService {

    private final static PACKAGE = "org.transmartproject.db.marshallers"
    private final static RESOURCE_PATTERN = "**/*Marshaller.class"

    void scanForClasses(final ApplicationContext ctx) {
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
        scanner.setResourcePattern(RESOURCE_PATTERN)
        scanner.addIncludeFilter ({
            MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory ->
                metadataReader.classMetadata.className.matches(".+Marshaller")
            } as TypeFilter)

        scanner.scan(PACKAGE)
    }
}
