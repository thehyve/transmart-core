package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.NoopDataConstraint

import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.getParam
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.validateParameterNames

@Component
class StandardDataConstraintFactory extends AbstractMethodBasedParameterFactory {

    private static Log LOG = LogFactory.getLog this

    private static final String SUBCONSTRAINTS_PARAM = 'subconstraints'

    @ProducerFor(DataConstraint.DISJUNCTION_CONSTRAINT)
    DataConstraint createDisjunctionConstraint(Map<String, Object> params,
                                                      Object createConstraint) {
        validateParameterNames([SUBCONSTRAINTS_PARAM], params)
        Map<String, Object> subConstraintsSpecs =
            getParam params, SUBCONSTRAINTS_PARAM, Map

        List<DataConstraint> constraints = new LinkedList()

        if (subConstraintsSpecs.size() == 0) {
            LOG.info "Sub-constraints map that was provided is empty; this " +
                    "disjunction constraint will be a no-op"
            constraints << new NoopDataConstraint()
        } else {
            subConstraintsSpecs.each {
                String key, Object value ->

                checkValue value
                if (value instanceof Map) {
                    constraints << createConstraint(key, value)
                } else { /* value is List of maps instead */
                    value.each { def innerValue ->
                        checkIsMap innerValue
                        constraints << createConstraint(key, innerValue)
                    }
                }
            }
        }

        if (constraints.size() == 1) {
            constraints[0]
        } else {
            new DisjunctionDataConstraint(constraints: constraints)
        }
    }

    private static void checkIsMap(Object value) {
        if (!(value instanceof Map)) {
            throw new InvalidArgumentsException("On a disjunction constraint, " +
                    "the $SUBCONSTRAINTS_PARAM parameter should be a map whose " +
                    "values are themselves Maps or lists of maps; got a list " +
                    "that instead of a map had a ${value.getClass()} with value $value")
        }
    }

    private static void checkValue(Object value) {
        if (!(value instanceof Map) && !(value instanceof List)) {
            throw new InvalidArgumentsException("On a disjunction constraint, " +
                    "the $SUBCONSTRAINTS_PARAM parameter should be a map whose " +
                    "values are themselves Maps or lists of maps; got a " +
                    "${value.getClass()} with value $value")
        }
    }
}
