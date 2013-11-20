package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.NoopDataConstraint

@Component
class StandardDataConstraintFactory extends AbstractMethodBasedParameterFactory {

    private static Log LOG = LogFactory.getLog this

    private static final String SUBCONSTRAINTS_PARAM = 'subconstraints'

    @ProducerFor(DataConstraint.DISJUNCTION_CONSTRAINT)
    DataConstraint createDisjunctionConstraint(Map<String, Object> params,
                                                      Object createConstraint) {
        Map<String, Object> subConstraintsSpecs =
            getParam params, SUBCONSTRAINTS_PARAM, Map

        if (subConstraintsSpecs.size() == 0) {
            LOG.info "Sub-constraints map that was provided is empty; this " +
                    "disjunction constraint will be a no-op"
            new NoopDataConstraint()
        } else if (subConstraintsSpecs.size() == 1) {
            Map.Entry<String, Object> entry =
                subConstraintsSpecs.entrySet().iterator().next()

            checkIsMap entry.value
            createConstraint entry.key, entry.value
        } else {
            def subConstraints = subConstraintsSpecs.collect {
                String key, Object value ->

                checkIsMap value
                createConstraint key, value
            }

            new DisjunctionDataConstraint(constraints: subConstraints)
        }
    }

    private static void checkIsMap(Object value) {
        if (!(value instanceof Map)) {
            throw new InvalidArgumentsException("Values of " +
                    "$SUBCONSTRAINTS_PARAM Map param should be maps themselves, " +
                    "got a ${value.getClass()} with value $value")
        }
    }

}
