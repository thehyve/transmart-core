package org.transmartproject.db.dataquery.highdim.parameterproducers

/**
 * Created by glopes on 11/18/13.
 */
class MapBasedParameterFactory implements DataRetrievalParameterFactory {

    private Map<String, Closure> producerMap

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
    def createFromParameters(String name, Map<String, Object> params) {
        if (!producerMap[name]) {
            return null
        }

        producerMap[name].call params
    }
}
