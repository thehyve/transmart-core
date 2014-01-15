package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.dataquery.highdim.projections.GenericProjection

/**
 * Created by jan on 1/7/14.
 */
class GenericProjectionsFactory implements DataRetrievalParameterFactory {

    Collection<String> fields

    GenericProjectionsFactory(Collection<String> fields) {
        this.fields = fields
    }

    @Override
    Set<String> getSupportedNames() {
        [Projection.GENERIC_PROJECTION] as Set
    }

    @Override
    boolean supports(String name) {
        Projection.GENERIC_PROJECTION == name
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

        new GenericProjection(fields)
    }
}
