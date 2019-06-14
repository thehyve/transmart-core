/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.support

import grails.orm.HibernateCriteriaBuilder
import grails.util.Holders
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.Criteria as HibernateCriteria
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Disjunction
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException

import java.util.function.Supplier
import java.util.stream.Collectors

import static org.transmartproject.db.support.DatabasePortabilityService.DatabaseType.ORACLE

/**
 * This class aimed to overcome oracle limitation on the number of items in IN clauses:
 *   'Message: ORA-01795: maximum number of expressions in a list is 1000'
 * and the maximum number of parameters PostgreSQL can handle:
 *   'Tried to send an out-of-range integer as a 2-byte value'.
 *
 * For Oracle, this class constructs disjunctions of value restrictions instead of creating a single IN-clause:
 * instead of 'patient_num IN (1, 2, 3, ... 1200)', the list of values is split in smaller sublists
 * and a disjunction of IN-clauses for each of the sublists is created:
 * 'patient_num IN (1, 2, 3, ... 1000) OR patient_num IN (1001, 1002, 1003, ... 1200)'
 *
 * For PostgreSQL, given a query, a property and a list of values, the query is executed for
 * sublists of the list of values and the results are combined.
 */
@CompileStatic
class InQuery {

    public static final int ORACLE_MAX_LIST_SIZE = 1000
    public static final int POSTGRES_MAX_PARAMETERS = 10000

    /**
     * Creates a disjunctive property constraint, the equivalent of
     * <code>Restrictions.in(property, values.collect().toArray())<code>.
     *
     * The value constraints are passed as a disjunction of IN-expressions to cope with
     * the maximum number of values in an IN-expression for Oracle ({@link #ORACLE_MAX_LIST_SIZE}).
     *
     * @param property the property to filter by.
     * @param values the list of values to filter on.
     * @return the criterion.
     */
    static Criterion inValues(String property, List values) {
        if (values.size() == 0) {
            return Restrictions.not(Restrictions.sqlRestriction('1=1'))
        } else if (databaseTypeIsOracle) {
            def collatedValues = collateValues(values, ORACLE_MAX_LIST_SIZE)
            return inCollatedValues(property, collatedValues)
        } else {
            return Restrictions.in(property, values.collect().toArray())
        }
    }

    @CompileDynamic
    static HibernateCriteria addIn(HibernateCriteriaBuilder criteriaBuilder, String property, List values) {
        return criteriaBuilder.add(inValues(property, values))
    }

    @CompileDynamic
    private static List<Object> executeIn(Supplier<HibernateCriteriaBuilder> builderProducer, String property, List values) {
        builderProducer.get().list {
            add(inValues(property, values))
        }
    }

    /**
     * Execute an IN-query for a list of values in batches. The maximum batch size is
     * {@link #POSTGRES_MAX_PARAMETERS} for PostgreSQL and unlimited for Oracle.
     * Within a batch, the value constraints are passed as a disjunction of IN-expressions.
     * The maximum number of values in an IN-expression is {@link #ORACLE_MAX_LIST_SIZE} for
     * Oracle and only limited by the maximum number of parameters for PostgreSQL.
     *
     * @param builderProducer function that provides a criteria builder to execute queries in batches.
     * @param property the property to restrict disjunctively.
     * @param values the values to restrict the property to.
     * @return the list of objects retrieved by the queries.
     */
    static List<Object> listIn(Supplier<HibernateCriteriaBuilder> builderProducer, String property, List values) {
        List<List> valueGroups
        if (databaseTypeIsOracle) {
            valueGroups = [values]
        } else {
            // Split values in groups
            valueGroups = collateValues(values, POSTGRES_MAX_PARAMETERS)
        }
        valueGroups.stream()
                .flatMap({ List groupValues ->
                    // Execute the query for a group of values
                    executeIn(builderProducer, property, groupValues).stream() })
                .collect(Collectors.toList())
    }

    /**
     * Adds disjunctive property constraints to a criteria, the equivalent of
     * <code>criteria.in(property, listOfItems)<code>.
     *
     * The value constraints are passed as a disjunction of IN-expressions to cope with
     * the maximum number of values in an IN-expression for Oracle ({@link #ORACLE_MAX_LIST_SIZE}).
     * Empty lists are not supported.
     *
     * @param criteria the criteria to which disjunctive property constraints are added.
     * @param property the property to filter by.
     * @param listOfItems the list of values to filter on.
     * @return the criteria.
     * @throws InvalidArgumentsException when an empty list is passed.
     */
    static Criteria addIn(Criteria criteria, String property, List listOfItems) {
        if (listOfItems.size() == 0) {
            throw new InvalidArgumentsException('Empty list')
        } else if (databaseTypeIsOracle) {
            def choppedItems = collateValues(listOfItems, ORACLE_MAX_LIST_SIZE)
            addConstraintsToCriteriaByFieldName(criteria, property, choppedItems)
        } else {
            criteria.in(property, listOfItems)
        }
    }

    @Memoized
    private static boolean getDatabaseTypeIsOracle() {
        def dataSource = Holders.applicationContext.getBean(DatabasePortabilityService)
        dataSource.databaseType == ORACLE
    }

    static List<List> collateValues(Iterable inItems, int maxSize) {
        if (!inItems) return [[]]
        inItems.collate(maxSize)
    }

    private static Criterion inCollatedValues(String property, List<List> values)
        throws InvalidRequestException {
        if (values.size() == 0) {
            return Restrictions.not(Restrictions.sqlRestriction('1=1'))
        } else if (values.size() == 1) {
            return Restrictions.in(property, values[0])
        } else {
            Disjunction disjunction = Restrictions.disjunction()
            for (Object value: values) {
                disjunction.add(Restrictions.in(property, value))
            }
            return disjunction
        }
    }

    @CompileDynamic
    private static Criteria addConstraintsToCriteriaByFieldName(Criteria criteria, String fieldName, List parameterValues)
            throws InvalidRequestException {
        if (parameterValues.size() == 0) {
            throw new InvalidArgumentsException('Empty list')
        } else if (parameterValues.size() == 1) {
            criteria.in(fieldName, parameterValues[0])
        } else {
            criteria.or {
                parameterValues.collect { parVal ->
                    'in' fieldName, parVal
                }
            }
        }
        criteria
    }

}
