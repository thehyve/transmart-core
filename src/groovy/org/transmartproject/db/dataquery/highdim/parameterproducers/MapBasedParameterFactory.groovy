package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.transmartproject.core.exceptions.InvalidArgumentsException

/**
 * Created by glopes on 11/18/13.
 */
class MapBasedParameterFactory implements DataRetrievalParameterFactory {

    private Map<String, Closure> producerMap

    /**
     * Constructor.
     * @param producerMap Takes a map between a data type name and a closure
     * that takes either 1) one argument, the parameters of the constraint/
     * projection or 2) two arguments, the parameters of the constraint/
     * projection and a callable object that allows the creation of the
     * constraints/projections. The closure should return the new constraint/
     * projection, never null. It may throw a {@link InvalidArgumentsException}.
     */
    MapBasedParameterFactory(Map<String, Closure> producerMap) {
        this.producerMap = producerMap
    }

    @Override
    Set<String> getSupportedNames() {
        producerMap.keySet()
    }

    @Override
    boolean supports(String name) {
        producerMap.containsKey name
    }

    @Override
    def createFromParameters(String name,
                             Map<String, Object> params,
                             Object createParameter) {
        Closure producer = producerMap[name]
        if (!producer) {
            return null
        }

        if (producer.maximumNumberOfParameters == 1) {
            producer.call params
        } else {
            producer.call params, createParameter
        }
    }
}
