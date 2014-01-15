package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.springframework.core.annotation.AnnotationUtils
import org.transmartproject.core.exceptions.InvalidArgumentsException

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Created by glopes on 11/18/13.
 */
class AbstractMethodBasedParameterFactory implements DataRetrievalParameterFactory {
    @Lazy private volatile Map<String, Method> producerMap = {
        def result = [:]
        for (method in delegate.getClass().methods) {
            def producerFor = AnnotationUtils.findAnnotation method, ProducerFor
            if (producerFor) {
                result[producerFor.value()] = method
            }
        }
        result
    }()

    @Override
    Set<String> getSupportedNames() {
        producerMap.keySet()
    }

    @Override
    boolean supports(String name) {
        supportedNames.contains name
    }

    @Override
    def createFromParameters(String name,
                             Map<String, Object> params,
                             Object createProducer) {

        Method producerMethod = producerMap[name]
        if (!producerMethod) {
            return null
        }

        try {
            if (producerMethod.parameterTypes.length == 1) {
                producerMethod.invoke this, params
            } else if (producerMethod.parameterTypes.length == 2) {
                producerMethod.invoke this, params, createProducer
            } else {
                throw new RuntimeException('The producer method should take either ' +
                        "one or two parameters; not the case for $producerMethod")
            }
        } catch (InvocationTargetException ite) {
            throw ite.targetException
        }
    }
}
