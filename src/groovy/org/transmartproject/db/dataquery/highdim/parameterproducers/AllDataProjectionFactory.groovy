package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.projections.AllDataProjectionImpl

class AllDataProjectionFactory implements DataRetrievalParameterFactory {

    private Map<String, Class> dataProperties
    private Map<String, Class> rowProperties

    AllDataProjectionFactory(Map<String, Class> dataProperties, Map<String, Class> rowProperties) {
        this.dataProperties = dataProperties
        this.rowProperties = rowProperties
    }

    @Override
    Set<String> getSupportedNames() {
        [Projection.ALL_DATA_PROJECTION] as Set
    }

    @Override
    boolean supports(String name) {
        Projection.ALL_DATA_PROJECTION == name
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

        new AllDataProjectionImpl(dataProperties, rowProperties)
    }
}
