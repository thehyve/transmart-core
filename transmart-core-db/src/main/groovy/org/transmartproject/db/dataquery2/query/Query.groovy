package org.transmartproject.db.dataquery2.query

import grails.validation.Validateable
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import grails.databinding.BindUsing
import org.springframework.validation.Errors

/**
 * Superclass of the query types supported by {@link QueryBuilder}.
 */
abstract class Query implements Validateable {
    String type = this.class.simpleName
}

/**
 * A query for patient dimension entries that are associated with the
 * observations that are selected with the query in <code>subQuery</code>.
 */
@Canonical
class PatientQuery extends Query {
    ObservationQuery subQuery
}

/**
 * Result types for observation queries.
 */
@CompileStatic
enum QueryType {
    VALUES,
    MIN,
    MAX,
    EXISTS,
    NONE
}

/**
 * Query for observational data. Selects records as specified by the query type <code>queryType</code>
 * that satisfy the constraint in <code>constraint</code>.
 *
 * For query type <code>MIN</code> and <code>MAX</code>, the <code>fieldName</code> field needs to be set.
 */
@Canonical
class ObservationQuery extends Query {
    QueryType queryType
    List<String> select
    @BindUsing({ obj, source -> ConstraintFactory.create(source['constraint']) })
    Constraint constraint

    static constraints = {
        constraint validator: { Constraint c, obj, Errors errors ->
            if (!c?.validate()) {
                errors.rejectValue(
                        'constraint',
                        'org.transmartproject.query.invalid.constraint.message',
                        [c?.class?.simpleName] as String[],
                        'Invalid constraint')
            }
        }
        queryType validator: { it != QueryType.NONE }
    }
}
