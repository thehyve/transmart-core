package org.transmartproject.db.support

import grails.orm.HibernateCriteriaBuilder
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.Criteria as HibernateCriteria
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.exceptions.InvalidRequestException

/**
 * This class aimed to overcome oracle limitation on the number of items in IN clause:
 * Message: ORA-01795: maximum number of expressions in a list is 1000
 *
 * This class constructs composite condition
 * Instead of making one condition
 * patient_num IN (1, 2, 3, ... 1200)
 * it chops list of items and join sub-conditions with OR operations
 * patient_num IN (1, 2, 3, ... 1000) OR patient_num IN (1001, 1002, 1003, ... 1200)
 */
class InQuery {

    public static final int MAX_LIST_SIZE = 1000

    public static HibernateCriteria addIn(HibernateCriteriaBuilder criteriaBuilder, String property, List listOfItems) {
        def choppedItems = chopParametersValues(listOfItems)
        addConstraintsToCriteriaByFieldName(criteriaBuilder, property, choppedItems)
    }

    public static Criteria addIn(Criteria criteria, String property, List listOfItems) {
        def choppedItems = chopParametersValues(listOfItems)
        addConstraintsToCriteriaByFieldName(criteria, property, choppedItems)
    }

    private static Map chopParametersValues(List inItems) {
        if (!inItems) return [:]
        def chunks = inItems.collate(MAX_LIST_SIZE)
        (0..<chunks.size()).collectEntries { index -> ["_${index}".toString(), chunks[index]] }
    }

    private static HibernateCriteria addConstraintsToCriteriaByFieldName(HibernateCriteriaBuilder builder, String fieldName, Map parameterValues)
            throws InvalidRequestException {
        builder.with {
            if (parameterValues.size() > 0) {
                or {
                    parameterValues.collect { parVal ->
                        builder.add(Restrictions.in(fieldName, parVal.value))
                    }
                }
            } else {
                and {
                    builder.add(Restrictions.in(fieldName, []))
                }
            }
        }
        builder.instance
    }

    private static Criteria addConstraintsToCriteriaByFieldName(Criteria criteria, String fieldName, Map parameterValues)
            throws InvalidRequestException {
        if (parameterValues.size() > 0) {
            criteria.or {
                parameterValues.collect { parVal ->
                    'in' fieldName, parVal.value
                }
            }
        } else {
            criteria.and {
                'in' fieldName, []
            }
        }
        criteria
    }
}