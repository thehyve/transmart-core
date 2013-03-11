package org.transmartproject.db.support;

import grails.converters.JSON;
import org.codehaus.groovy.grails.commons.spring.GrailsContextEvent;
import org.codehaus.groovy.runtime.MethodClosure;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

@Service
public class MarshallerRegistrarService implements
        ApplicationListener<GrailsContextEvent>, ApplicationContextAware {

    final static String PACKAGE = "org.transmartproject.db.marshallers";
    final static String RESOURCE_PATTERN = "**/*Marshaller.class";

    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(GrailsContextEvent event) {
        /* Listening to ContextRefreshedEvent instead does not work; probably
         * because it fires too early and Grails rewrites the JSON converter
         * configuration after that event */
        if (event.getEventType() == GrailsContextEvent
                .DYNAMIC_METHODS_REGISTERED) {
            scanForClasses(this.applicationContext);
        }
    }

    private void scanForClasses(final ApplicationContext ctx) {
        ClassPathBeanDefinitionScanner scanner = new
                ClassPathBeanDefinitionScanner((BeanDefinitionRegistry)ctx, false) {

            @Override
            protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
                Set<BeanDefinitionHolder> superValue = super.doScan(basePackages);

                for (BeanDefinitionHolder holder : superValue) {
                    Object bean = ctx.getBean(holder.getBeanName());
                    JSON.registerObjectMarshaller(
                            (Class<?>) new MethodClosure(bean,
                            "getTargetType").call(),
                            new MethodClosure(bean, "convert"));
                }

                return superValue;
            }
        };
        scanner.setResourcePattern(RESOURCE_PATTERN);
        scanner.addIncludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                return metadataReader.getClassMetadata().getClassName()
                        .matches(".+Marshaller");
            }
        });

        scanner.scan(PACKAGE);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
