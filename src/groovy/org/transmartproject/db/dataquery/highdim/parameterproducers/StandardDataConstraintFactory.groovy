package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.NoopDataConstraint

@Component
class StandardDataConstraintFactory extends AbstractMethodBasedParameterFactory {

    private DisjunctionConstraintFactory disjunctionConstraintFactory =
            new DisjunctionConstraintFactory(DisjunctionDataConstraint, NoopDataConstraint)

    @ProducerFor(DataConstraint.DISJUNCTION_CONSTRAINT)
    DataConstraint createDisjunctionConstraint(Map<String, Object> params,
                                               Object createConstraint) {
        disjunctionConstraintFactory.createDisjunctionConstraint params, createConstraint
    }
}
