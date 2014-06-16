/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.marshallers

import grails.converters.JSON
import groovy.util.logging.Log4j
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.registerBeanDefinition

/**
 * For each {@link HalOrJsonSerializationHelper} we find, we must register them
 * a new renderer on the application context and we must also register the
 * JSON Marshaller so that {@link grails.converters.JSON} knows what to do
 * when it find <code>foo as JSON</code>.
 */
@Log4j
public class MarshallersRegistrar implements FactoryBean {

    @Autowired
    ApplicationContext ctx

    String packageName

    void scanForClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new
                ClassPathScanningCandidateComponentProvider(false, ctx.environment)

        scanner.addIncludeFilter(
                new AssignableTypeFilter(HalOrJsonSerializationHelper))
        Set<BeanDefinition> serializationHelpers =
                scanner.findCandidateComponents packageName

        BeanDefinitionRegistry registry = ctx
        for (BeanDefinition helperDef: serializationHelpers) {
            log.debug "Processing serialization helper ${helperDef.beanClassName}"

            // register serialization helper
            def helperDefinitionHolder = createHolderForHelper(helperDef)
            registerBeanDefinition helperDefinitionHolder, ctx

            //create and register marshaller
            def marshaller = new CoreApiObjectMarshaller(
                    serializationHelper: ctx.getBean(helperDefinitionHolder.beanName))
            JSON.registerObjectMarshaller marshaller

            // create renderer bean and register it on the application context
            registerBeanDefinition createHolderForRenderer(marshaller.targetType), ctx
        }

        // force initialization of renderers
        ctx.getBeansOfType(HalOrJsonRenderer)
    }

    BeanDefinitionHolder createHolderForHelper(BeanDefinition helperBeanDefinition) {
        new BeanDefinitionHolder(helperBeanDefinition,
                uncapitalize(helperBeanDefinition.beanClassName))
    }

    BeanDefinitionHolder createHolderForRenderer(Class targetType) {
        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.rootBeanDefinition(HalOrJsonRenderer).
                        addConstructorArgValue(targetType)

        String beanName = (targetType.simpleName -
                'SerializationHelper') +'Renderer'

        new BeanDefinitionHolder(builder.beanDefinition, uncapitalize(beanName))
    }

    static String uncapitalize(String original) {
        original[0].toLowerCase(Locale.ENGLISH) + original.substring(1)
    }

    @Override
    Object getObject() throws Exception {
        scanForClasses()
        null /* we would have need this for the side effects */
    }

    final Class<?> objectType = null
    final boolean singleton = false
}

