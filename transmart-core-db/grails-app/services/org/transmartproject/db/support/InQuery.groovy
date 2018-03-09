/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.support

import grails.orm.HibernateCriteriaBuilder
import grails.util.Holders
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.Criteria as HibernateCriteria
import org.hibernate.criterion.Criterion
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

    @CompileStatic
    static Criterion inValues(String property, Iterable values) {
        if (databaseTypeIsOracle) {
            def collatedValues = collateValues(values)
            return inCollatedValues(property, collatedValues)
        } else {
            return Restrictions.in(property, values.collect().toArray())
        }
    }

    static HibernateCriteria addIn(HibernateCriteriaBuilder criteriaBuilder, String property, Iterable values) {
        return criteriaBuilder.add(inValues(property, values))
    }

    static Criteria addIn(Criteria criteria, String property, Iterable listOfItems) {
        if (databaseTypeIsOracle) {
            def choppedItems = collateValues(listOfItems)
            addConstraintsToCriteriaByFieldName(criteria, property, choppedItems)
        } else {
            criteria.in(property, listOfItems)
        }
    }

    private static boolean databaseTypeIsOracle = {
        def dataSource = Holders.applicationContext.getBean(DatabasePortabilityService)
        dataSource.databaseType == ORACLE
    }

    @CompileStatic
    static List<List> collateValues(Iterable inItems) {
        if (!inItems) return [[]]
        inItems.collate(MAX_LIST_SIZE)
    }

    @CompileStatic
    private static Criterion inCollatedValues(String property, List<List> values)
        throws InvalidRequestException {
        if (values.size() > 1) {
            Disjunction disjunction = Restrictions.disjunction()
            values.each { parVal ->
                disjunction.add(Restrictions.in(property, parVal))
            }
            return disjunction
        } else {
            return Restrictions.in(property, values[0] ?: [])
        }
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

