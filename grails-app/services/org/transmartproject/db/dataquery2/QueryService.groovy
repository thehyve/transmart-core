package org.transmartproject.db.dataquery2

import grails.transaction.Transactional
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.dataquery2.query.Combination
import org.transmartproject.db.dataquery2.query.ConceptConstraint
import org.transmartproject.db.dataquery2.query.Constraint
import org.transmartproject.db.dataquery2.query.Field
import org.transmartproject.db.dataquery2.query.FieldConstraint
import org.transmartproject.db.dataquery2.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.dataquery2.query.InvalidQueryException
import org.transmartproject.db.dataquery2.query.NullConstraint
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.dataquery2.query.Operator
import org.transmartproject.db.dataquery2.query.QueryBuilder
import org.transmartproject.db.dataquery2.query.QueryType
import org.transmartproject.db.dataquery2.query.Type
import org.transmartproject.db.dataquery2.query.ValueDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.user.User


@Transactional
class QueryService {

    @Autowired
    AccessControlChecks accessControlChecks

    SessionFactory sessionFactory

    private final Field valueTypeField = new Field(dimension: ValueDimension, fieldName: 'valueType', type: Type.STRING)
    private final Field textValueField = new Field(dimension: ValueDimension, fieldName: 'textValue', type: Type.STRING)
    private final Field numberValueField = new Field(dimension: ValueDimension, fieldName: 'numberValue', type: Type.NUMERIC)

    private Object get(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).uniqueResult()
    }

    private List getList(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).list()
    }

    /**
     * Checks if an observation fact exists that satisfies <code>constraint</code>.
     * @param builder the {@link HibernateCriteriaQueryBuilder} used to build the query.
     * @param constraint the constraint that is applied to filter for observation facts.
     * @return true iff an observation fact is found that satisfies <code>constraint</code>.
     */
    private boolean exists(HibernateCriteriaQueryBuilder builder, Constraint constraint) {
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.EXISTS,
                constraint: constraint
        )
        DetachedCriteria criteria = builder.detachedCriteriaFor(query)
        (criteria.getExecutableCriteria(sessionFactory.currentSession).setMaxResults(1).uniqueResult() != null)
    }

    /**
     * @description Function for getting a list of observations that are specified by <code>query</code>.
     * The allowed queryType is VALUES.
     * @param query
     * @param user
     */
    List<ObservationFact> list(ObservationQuery query, User user) {
        if (query.queryType != QueryType.VALUES){
            throw new InvalidQueryException("Expected queryType VALUES, got ${query.queryType}")
        }

        def builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        DetachedCriteria criteria = builder.detachedCriteriaFor(query)
        getList(criteria)
    }

    private List<ConceptConstraint> findConceptConstraint(Constraint constraint){
        if (constraint instanceof ConceptConstraint){
            return [constraint]
        } else if (constraint instanceof Combination){
            def result = []
            constraint.args.each {
                result.addAll(findConceptConstraint(it))
            }
            result
        } else {
            return []
        }
    }

    /**
     * @description Function for getting a aggregate value of a single field.
     * The allowed queryTypes are MIN, MAX and AVERAGE.
     * The responsibility for checking the queryType is allocated to the controller.
     * Only allowed for numerical values and so checks if this is the case
     * @param query
     * @param user
     */
    Number aggregate(ObservationQuery query, User user){

        if (![QueryType.MIN, QueryType.AVERAGE, QueryType.MAX].contains(query.queryType)){
            throw new InvalidQueryException(
                    "Aggregate requires a query with a queryType of MIN, MAX or AVERAGE, got ${query.queryType}"
            )
        }

        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        // check if the constraint from the query has a conceptConstraint
        // and that it has only 1
        def conceptConstraint
        List<ConceptConstraint> conceptConstraintList = findConceptConstraint(query.constraint)
        switch (conceptConstraintList.size()){
            case 0:
                throw new InvalidQueryException('Aggregate requires a conceptConstraint, found 0')
            case {it > 1}:
                throw new InvalidQueryException("Aggregate requires just 1 conceptConstraint, found ${conceptConstraintList.size()}".toString())
            default:
                conceptConstraint = conceptConstraintList[0]
        }

        // check if that concept exists
        if (!exists(builder, conceptConstraint)){
            throw new InvalidQueryException("Concept path not found. Supplied path is: ${conceptConstraint.path}".toString())
        }

        // check if the concept is truly numerical (all textValue are E and all numberValue have a value)
        // all(A) and all(B) <=> not(any(not A) or any(not B))
        def valueTypeNotNumericConstraint = new FieldConstraint(
                operator: Operator.NOT_EQUALS,
                field: valueTypeField,
                value: ObservationFact.TYPE_NUMBER
        )
        def textValueNotEConstraint = new FieldConstraint(
                operator: Operator.NOT_EQUALS,
                field: textValueField,
                value: "E"
        )
        def numberValueNullConstraint = new NullConstraint(
                field: numberValueField
        )
        def notNumericalCombination = new Combination(
                operator: Operator.OR,
                args: [valueTypeNotNumericConstraint, textValueNotEConstraint, numberValueNullConstraint]
        )
        def conceptNotNumericalCombination = new Combination(
                operator: Operator.AND,
                args: [conceptConstraint, notNumericalCombination]
        )

        if (exists(builder, conceptNotNumericalCombination)){
            def message = 'One of the observationFacts had either an empty numerical value or a ' +
                    'textValue with something else then \'E\''
            throw new InvalidQueryException(message)
        }

        // get aggregate value
        DetachedCriteria queryCriteria = builder.detachedCriteriaFor(query)
        def result = get(queryCriteria)
        result
    }
    
}
