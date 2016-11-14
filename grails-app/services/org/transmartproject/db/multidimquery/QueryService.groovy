package org.transmartproject.db.multidimquery

import grails.transaction.Transactional
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Subqueries
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.multidimquery.query.Combination
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.Field
import org.transmartproject.db.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.db.multidimquery.query.NullConstraint
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.QueryBuilder
import org.transmartproject.db.multidimquery.query.QueryBuilderException
import org.transmartproject.db.multidimquery.query.AggregateType
import org.transmartproject.db.multidimquery.query.StudyConstraint
import org.transmartproject.db.multidimquery.query.StudyObjectConstraint
import org.transmartproject.db.multidimquery.query.Type
import org.transmartproject.db.multidimquery.query.ValueDimension
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

    private Number getAggregate(AggregateType aggregateType, DetachedCriteria criteria) {
        switch (aggregateType) {
            case AggregateType.MIN:
                criteria = criteria.setProjection(Projections.min('numberValue'))
                break
            case AggregateType.AVERAGE:
                criteria = criteria.setProjection(Projections.avg('numberValue'))
                break
            case AggregateType.MAX:
                criteria = criteria.setProjection(Projections.max('numberValue'))
                break
            case AggregateType.COUNT:
                criteria = criteria.setProjection(Projections.rowCount())
                break
            default:
                throw new QueryBuilderException("Query type not supported: ${aggregateType}")
        }
        aggregateType == AggregateType.COUNT ? (Long)get(criteria) : (Number)get(criteria)
    }

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
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        (criteria.getExecutableCriteria(sessionFactory.currentSession).setMaxResults(1).uniqueResult() != null)
    }

    /**
     * @description Function for getting a list of observations that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    List<ObservationFact> list(Constraint constraint, User user) {
        def builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        getList(criteria)
    }

    /**
     * @description Function for getting a list of patients for which there are observations
     * that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    List<org.transmartproject.db.i2b2data.PatientDimension> listPatients(Constraint constraint, User user) {
        def builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        DetachedCriteria constraintCriteria = builder.buildCriteria(constraint)
        DetachedCriteria patientIdsCriteria = constraintCriteria.setProjection(Projections.property('patient'))
        DetachedCriteria patientCriteria = DetachedCriteria.forClass(org.transmartproject.db.i2b2data.PatientDimension, 'patient')
        patientCriteria.add(Subqueries.propertyIn('id', patientIdsCriteria))
        getList(patientCriteria)
    }

    static List<StudyConstraint> findStudyConstraints(Constraint constraint){
        if (constraint instanceof StudyConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findStudyConstraints(it) }
        } else {
            return []
        }
    }

    static List<StudyObjectConstraint> findStudyObjectConstraints(Constraint constraint){
        if (constraint instanceof StudyObjectConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findStudyObjectConstraints(it) }
        } else {
            return []
        }
    }

    static List<ConceptConstraint> findConceptConstraints(Constraint constraint){
        if (constraint instanceof ConceptConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findConceptConstraints(it) }
        } else {
            return []
        }
    }

    Long count(Constraint constraint, User user) {
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        (Long) get(builder.buildCriteria(constraint).setProjection(Projections.rowCount()))
    }

    /**
     * @description Function for getting a aggregate value of a single field.
     * The allowed queryTypes are MIN, MAX and AVERAGE.
     * The responsibility for checking the queryType is allocated to the controller.
     * Only allowed for numerical values and so checks if this is the case
     * @param query
     * @param user
     */
    Number aggregate(AggregateType type, Constraint constraint, User user) {

        if (type == AggregateType.NONE) {
            throw new InvalidQueryException("Aggregate requires a valid aggregate type.")
        }

        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        List<ConceptConstraint> conceptConstraintList = findConceptConstraints(constraint)
        if (conceptConstraintList.size() == 0) throw new InvalidQueryException('Aggregate requires exactly one ' +
                'concept constraint, found none.')
        if (conceptConstraintList.size() > 1) throw new InvalidQueryException("Aggregate requires exactly one concept" +
                " constraint, found ${conceptConstraintList.size()}.")
        def conceptConstraint = conceptConstraintList[0]

        // check if the concept exists
        def concept = org.transmartproject.db.i2b2data.ConceptDimension.findByConceptPath(conceptConstraint.path)
        if (concept == null) {
            throw new InvalidQueryException("Concept path not found. Supplied path is: ${conceptConstraint.path}")
        }
        // check if there are any observations for the concept
        if (!exists(builder, conceptConstraint)) {
            throw new InvalidQueryException("No observations found for concept path: ${conceptConstraint.path}")
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
        DetachedCriteria queryCriteria = builder.buildCriteria(constraint)
        return getAggregate(type, queryCriteria)
    }

}
