package org.transmartproject.db.dataquery2.query

import grails.validation.Validateable
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import grails.databinding.BindUsing
import groovy.util.logging.Slf4j
import org.springframework.validation.Errors

/**
 * Superclass of the query types supported by {@link QueryBuilder}.
 */
abstract class Query implements Validateable {
    String type = this.class.simpleName
}

/**
 * Result types for observation queries.
 */
@CompileStatic
@Slf4j
enum QueryType {
    VALUES,
    MIN,
    MAX,
    AVERAGE,
    COUNT,
    EXISTS,
    NONE

    private static final Map<String, QueryType> mapping = new HashMap<>();
    static {
        for (QueryType type: values()) {
            mapping.put(type.name().toLowerCase(), type);
        }
    }

    public static QueryType forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            log.error "Unknown query type: ${name}"
            return NONE
        }
    }
}

/**
 * Query for observational data. Selects records as specified by the query type <code>queryType</code>
 * that satisfy the constraint in <code>constraint</code>.
 *
 * For query type <code>MIN</code> and <code>MAX</code>, the <code>fieldName</code> field needs to be set.
 */
@Canonical
class ObservationQuery extends Query {
    @BindUsing({obj, source -> QueryType.forName(source['queryType']) })
    QueryType queryType
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
