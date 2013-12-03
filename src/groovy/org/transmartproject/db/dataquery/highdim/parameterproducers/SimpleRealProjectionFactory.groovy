package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection
import org.transmartproject.db.dataquery.highdim.projections.SimpleRealProjection

class SimpleRealProjectionFactory extends AbstractMethodBasedParameterFactory {

    String property

    SimpleRealProjectionFactory(String property) {
        this.property = property
    }

    @ProducerFor(CriteriaProjection.DEFAULT_REAL_PROJECTION)
    Projection createSimpleRealProjection(Map<String, Object> params) {
        if (!params.isEmpty()) {
            throw new InvalidArgumentsException(
                    'This projection takes no parameters')
        }

        new SimpleRealProjection(property: property)
    }
}
