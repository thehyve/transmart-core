package org.transmartproject.rest.misc

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

class HandleAllExceptionsBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory
                .getBeanDefinition('businessExceptionResolver')
                .propertyValues
                .addPropertyValue('handleAll', true)
    }
}
