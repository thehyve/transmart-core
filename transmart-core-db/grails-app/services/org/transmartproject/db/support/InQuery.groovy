/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.support

import grails.orm.HibernateCriteriaBuilder
import grails.util.Holders
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.Criteria as HibernateCriteria
import org.hibernate.criterion.Disjunction
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.exceptions.InvalidRequestException
import static org.transmartproject.db.support.DatabasePortabilityService.DatabaseType.ORACLE

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

    public static HibernateCriteria addIn(HibernateCriteriaBuilder criteriaBuilder, String property, Iterable listOfItems) {
        if (databaseTypeIsOracle) {
            def choppedItems = chopParametersValues(listOfItems)
            addConstraintsToCriteriaByFieldName(criteriaBuilder, property, choppedItems)
        } else {
            criteriaBuilder.add(Restrictions.in(property, listOfItems))
        }
    }

    public static Criteria addIn(Criteria criteria, String property, Iterable listOfItems) {
        if (databaseTypeIsOracle) {
            def choppedItems = chopParametersValues(listOfItems)
            addConstraintsToCriteriaByFieldName(criteria, property, choppedItems)
        } else {
            criteria.in(property, listOfItems)
        }
    }

    private static boolean databaseTypeIsOracle = {
        def dataSource = Holders.applicationContext.getBean(DatabasePortabilityService)
        dataSource.databaseType == ORACLE
    }

    private static List<List> chopParametersValues(Iterable inItems) {
        if (!inItems) return [[]]
        inItems.collate(MAX_LIST_SIZE)
    }

    private static HibernateCriteria addConstraintsToCriteriaByFieldName(HibernateCriteriaBuilder builder, String fieldName, List parameterValues)
            throws InvalidRequestException {
        builder.with {
            if (parameterValues.size() > 1) {
                Disjunction disjunction = Restrictions.disjunction()
                parameterValues.each { parVal ->
                    disjunction.add(Restrictions.in(fieldName, parVal))
                }
                builder.add(disjunction)
            } else {
                builder.add(Restrictions.in(fieldName, parameterValues[0] ?: []))
            }
        }
        builder.instance
    }

    private static Criteria addConstraintsToCriteriaByFieldName(Criteria criteria, String fieldName, List parameterValues)
            throws InvalidRequestException {
        if (parameterValues.size() > 1) {
            criteria.or {
                parameterValues.collect { parVal ->
                    'in' fieldName, parVal
                }
            }
        } else {
            criteria.in(fieldName, parameterValues[0] ?: [])
        }
        criteria
    }
}