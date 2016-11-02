package org.transmartproject.db.dataquery2

import grails.converters.JSON
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.dataquery2.query.*
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study

@Slf4j
@Rollback
@Integration
class HibernateCriteriaQueryBuilderSpec extends TransmartSpecification {

    Field patientAgeField
    Field conceptCodeField
    TestData testData

    SessionFactory sessionFactory

    Object get(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).uniqueResult()
    }

    List getList(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).list()
    }

    boolean exists(DetachedCriteria criteria) {
        (criteria.getExecutableCriteria(sessionFactory.currentSession).setMaxResults(1).uniqueResult() != null)
    }

    void setupData() {
        patientAgeField = new Field(dimension: PatientDimension.class, fieldName: 'age', type: Type.NUMERIC)
        conceptCodeField = new Field(dimension: ConceptDimension.class, fieldName: 'conceptCode', type: Type.STRING)

        testData = new TestData().createDefault()
        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.saveAll()
    }

    void 'test Hibernate subqueries'() {
        setupData()

        when:
        DetachedCriteria criteria = DetachedCriteria.forClass(ObservationFact, 'o1')
                .createAlias('patient', 'patient')
                .add(Restrictions.gt('patient.age', 60L))
        List result1 = criteria.getExecutableCriteria(sessionFactory.currentSession).list()
        log.info "RESULT: ${result1.asList()}"

        then:
        result1.size() == 1

        when:
        def query = DetachedCriteria.forClass(ObservationFact, 'o2')
                .add(Subqueries.propertyLt('startDate',
                    criteria
                        .add(Restrictions.eqProperty('o1.patient', 'o2.patient'))
                        .setProjection(Projections.min('startDate'))
        ))
        List result2 = getList(query)
        log.info "RESULT: ${result2}"

        then:
        result2.size() == 0
    }

    void 'test CriteriaQueryBuilder with patient set constraint'() {
        setupData()
        org.transmartproject.db.i2b2data.ConceptDimension concept =
                org.transmartproject.db.i2b2data.ConceptDimension.find { conceptPath == '\\foo\\study1\\bar\\'}

        FieldConstraint constraint = new FieldConstraint()
        constraint.field = conceptCodeField
        constraint.operator = Operator.EQUALS
        constraint.value = concept.conceptCode

        ObservationQuery subquery = new ObservationQuery()
        subquery.queryType = QueryType.VALUES
        subquery.constraint = constraint

        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )

        when:
        def patientIds = [this.testData.clinicalData.patients[1].id,
                          this.testData.clinicalData.patients[2].id]

        PatientSetConstraint subqueryConstraint = new PatientSetConstraint(
                patientIds: patientIds
        )
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.VALUES,
                constraint: subqueryConstraint
        )

        DetachedCriteria criteriaForPatientId = builder.detachedCriteriaFor(query)
        List resultsForPatientId = getList(criteriaForPatientId)

        then:
        resultsForPatientId.size() == 1
        (resultsForPatientId[0] as ObservationFact).patient.id == patientIds[0]

        when:
        def patientSetId = this.testData.clinicalData.patientsQueryMaster.queryInstances[0].queryResults[0].patientSet[0].id
        subqueryConstraint = new PatientSetConstraint(
                patientSetId: patientSetId
        )
        query = new ObservationQuery(
                queryType: QueryType.VALUES,
                constraint: subqueryConstraint
        )
        DetachedCriteria criteriaForPatientSetId = builder.detachedCriteriaFor(query)
        List resultsForPatientSetId = getList(criteriaForPatientSetId)

        then:
        resultsForPatientSetId.size() == 1
        (resultsForPatientSetId[0] as ObservationFact).patient.id == testData.clinicalData.patients[0].id
    }


    void 'test CriteriaQueryBuilder with clinical data'() {
        setupData()

        FieldConstraint constraint = new FieldConstraint()
        constraint.field = patientAgeField
        constraint.operator = Operator.GREATER_THAN
        constraint.value = 60L
        ObservationQuery query = new ObservationQuery()
        query.queryType = QueryType.VALUES
        query.constraint = new Negation(arg: constraint)
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )

        DetachedCriteria criteria = builder.detachedCriteriaFor(query)

        when:
        List results = getList(criteria)

        then:
        results.size() == 1
        (results[0] as ObservationFact).patient.id == testData.i2b2Data.patients[1].id
    }

    void 'test CriteriaQueryBuilder with observation value constraint'() {
        setupData()

        // Query for observations for values > 1
        ValueConstraint constraint = new ValueConstraint()
        constraint.valueType = Type.NUMERIC
        constraint.operator = Operator.GREATER_THAN
        constraint.value = 1L
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.VALUES,
                constraint: constraint
        )
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )

        DetachedCriteria criteria = builder.detachedCriteriaFor(query)

        when:
        List results = getList(criteria)

        then:
        // should only return observations for patient testData.i2b2Data.patients[1]
        results.size() == 2
        results.each { it.valueType == ObservationFact.TYPE_NUMBER && it.numberValue > 1 }
    }

    void 'test CriteriaQueryBuilder with temporal constraints'() {
        setupData()

        org.transmartproject.db.i2b2data.ConceptDimension concept =
                org.transmartproject.db.i2b2data.ConceptDimension.find { conceptPath == '\\foo\\study1\\bar\\'}
        FieldConstraint constraint = new FieldConstraint()
        constraint.field = conceptCodeField
        constraint.operator = Operator.EQUALS
        constraint.value = concept.conceptCode
        ObservationQuery subquery = new ObservationQuery()
        subquery.queryType = QueryType.VALUES
        subquery.constraint = constraint
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )

        DetachedCriteria criteria = builder.detachedCriteriaFor(subquery)

        when:
        List results = getList(criteria)
        log.info "Subquery results:"
        results.each {
            def json = ((ObservationFact)it) as JSON
            log.info json.toString(true)
        }

        then:
        results.size() == 1
        ((ObservationFact)results[0]).conceptCode == concept.conceptCode

        when:
        TemporalConstraint beforeConstraint = new TemporalConstraint(
                operator: Operator.BEFORE,
                eventQuery: subquery
        )
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.VALUES,
                constraint: beforeConstraint
        )
        builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )
        DetachedCriteria temporalCriteria = builder.detachedCriteriaFor(query)
        List temporalResults = getList(temporalCriteria)
        log.info "Main query results:"
        temporalResults.each {
            def json = ((ObservationFact)it) as JSON
            log.info json.toString(true)
        }

        then:
        temporalResults.empty
    }

    void 'test CriteriaQueryBuilder with existential queries'() {
        setupData()

        org.transmartproject.db.i2b2data.ConceptDimension concept =
                org.transmartproject.db.i2b2data.ConceptDimension.find { conceptPath == '\\foo\\study1\\bar\\'}
        FieldConstraint constraint = new FieldConstraint()
        constraint.field = conceptCodeField
        constraint.operator = Operator.EQUALS
        constraint.value = concept.conceptCode
        ObservationQuery query = new ObservationQuery()
        query.queryType = QueryType.EXISTS
        query.constraint = constraint
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )

        def criteria = builder.detachedCriteriaFor(query)

        when:
        def result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        result

        when:
        query.constraint = new Negation(arg: constraint)
        builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )
        criteria = builder.detachedCriteriaFor(query)
        result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        result

        when:
        constraint.value = "xyzabc"
        query.constraint = constraint
        builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )
        criteria = builder.detachedCriteriaFor(query)
        result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        !result
    }

    void 'test CriteriaQueryBuilder with min and max queries'() {
        setupData()

        when:
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.MAX,
                select: ['numberValue'],
                constraint: new TrueConstraint()
        )
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )
        def criteria = builder.detachedCriteriaFor(query)
        def result = get(criteria)
        log.info "Max: ${result}"

        then:
        result == 100

        when:
        query.queryType = QueryType.MIN
        builder = new HibernateCriteriaQueryBuilder(
                studies: Study.findAll()
        )
        criteria = builder.detachedCriteriaFor(query)
        result = get(criteria)
        log.info "Min: ${result}"

        then:
        result == 10
    }

}
