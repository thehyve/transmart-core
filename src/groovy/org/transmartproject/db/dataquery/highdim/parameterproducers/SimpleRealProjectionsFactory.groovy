package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.projections.SimpleRealProjection

class SimpleRealProjectionsFactory implements DataRetrievalParameterFactory {

    /* projection name -> property */
    Map<String, String> projectionToProperty

    SimpleRealProjectionsFactory(Map<String, String> projectionToProperty) {
        this.projectionToProperty = projectionToProperty
    }

    @Override
    Set<String> getSupportedNames() {
        projectionToProperty.keySet()
    }

    @Override
    boolean supports(String name) {
        projectionToProperty.containsKey(name)
    }

    @Override
    def createFromParameters(String name,
                             Map<String, Object> params,
                             Object createParameter) {
        if (!supports(name)) {
            return null
        }

        if (!params.isEmpty()) {
            throw new InvalidArgumentsException(
                    'This projection takes no parameters')
        }

        new SimpleRealProjection(property: projectionToProperty[name])
    }
}
