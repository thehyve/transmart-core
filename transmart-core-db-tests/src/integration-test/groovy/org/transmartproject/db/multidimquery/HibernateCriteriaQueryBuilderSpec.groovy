package org.transmartproject.db.multidimquery

import grails.converters.JSON
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.core.multidimquery.query.ModifierConstraint
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.NullConstraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.QueryBuilder
import org.transmartproject.core.multidimquery.query.QueryBuilderException
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.TemporalConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.multidimquery.query.ValueConstraint
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.db.StudyTestData
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import spock.lang.Specification
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.multidimquery.query.*

@Slf4j
@Rollback
@Integration
class HibernateCriteriaQueryBuilderSpec extends Specification {

    @Autowired
    SessionFactory sessionFactory

    Field patientAgeField
    Field conceptCodeField
    TestData testData
    TestData hypercubeTestData

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
        TestData.prepareCleanDatabase()

        patientAgeField = new Field(dimension: DimensionImpl.PATIENT.name, fieldName: 'age', type: Type.NUMERIC)
        conceptCodeField = new Field(dimension: DimensionImpl.CONCEPT.name, fieldName: 'conceptCode', type: Type.STRING)

        testData = new TestData().createDefault()
        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.saveAll()
    }

    void setupHypercubeData(){
        TestData.prepareCleanDatabase()

        hypercubeTestData = TestData.createHypercubeDefault()
        hypercubeTestData.saveAll()
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

        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()

        when:
        def patientIds = [this.testData.clinicalData.patients[1].id,
                          this.testData.clinicalData.patients[2].id]

        PatientSetConstraint subqueryConstraint = new PatientSetConstraint(
                patientIds: patientIds
        )

        DetachedCriteria criteriaForPatientId = builder.buildCriteria(subqueryConstraint)
        List resultsForPatientId = getList(criteriaForPatientId)

        then:
        resultsForPatientId.size() == 1
        (resultsForPatientId[0] as ObservationFact).patient.id == patientIds[0]

        when:
        def patientSetId = this.testData.clinicalData.patientsQueryMaster.queryInstances[0].id
        subqueryConstraint = new PatientSetConstraint(
                patientSetId: patientSetId
        )
        DetachedCriteria criteriaForPatientSetId = builder.buildCriteria(subqueryConstraint)
        List resultsForPatientSetId = getList(criteriaForPatientSetId)

        then:
        resultsForPatientSetId.size() == 2
        resultsForPatientSetId*.patient*.id as Set == testData.clinicalData.patients[0..1]*.id as Set
    }

    void 'test CriteriaQueryBuilder with clinical data'() {
        setupData()

        FieldConstraint constraint = new FieldConstraint()
        constraint.field = patientAgeField
        constraint.operator = Operator.GREATER_THAN
        constraint.value = 60L
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()

        DetachedCriteria criteria = builder.buildCriteria(new Negation(arg: constraint))

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
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()

        DetachedCriteria criteria = builder.buildCriteria(constraint)

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
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()

        DetachedCriteria criteria = builder.buildCriteria(constraint)

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
                eventConstraint: constraint
        )
        builder = HibernateCriteriaQueryBuilder.forAllStudies()
        DetachedCriteria temporalCriteria = builder.buildCriteria(beforeConstraint)
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
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()

        def criteria = builder.buildCriteria(constraint)

        when:
        def result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        result

        when:
        builder = HibernateCriteriaQueryBuilder.forAllStudies()
        criteria = builder.buildCriteria(new Negation(arg: constraint))
        result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        result

        when:
        constraint.value = "xyzabc"
        builder = HibernateCriteriaQueryBuilder.forAllStudies()
        criteria = builder.buildCriteria(constraint)
        result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        !result
    }

    void 'test ConceptConstraint'(){
        setupData()


        when:
        def constraint= new ConceptConstraint(path: '\\foo\\study1\\bar\\')
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()
        def criteria = builder.buildCriteria(constraint)

        def result = getList(criteria)
        def fact = result[0]
        then:
        result.size() == 1
        fact.conceptCode == '2'

        when:
        constraint.path = 'NoExistingPath'
        criteria = builder.buildCriteria(constraint)
        result = exists(criteria)
        then:
        result == false
    }

    void 'test NullConstraint'(){
        setupData()

        def fact = testData.clinicalData.facts.find({
            it.valueType == 'T'
        })
        fact.textValue = null
        testData.clinicalData.saveAll()

        when:
        def constraint = new NullConstraint(
                field: new Field(
                        dimension: DimensionImpl.VALUE.name,
                        fieldName: 'textValue',
                        type: 'STRING'
                )
        )
        def builder = HibernateCriteriaQueryBuilder.forAllStudies()

        def criteria = builder.buildCriteria(constraint)
        def result = getList(criteria)

        def foundFact = result[0]
        then:
        result.size() == 1
        foundFact.valueType == 'T'
        foundFact.textValue == null
    }

    void 'test CriteriaQueryBuilder with default modifier code'() {
        setupHypercubeData()
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forStudies([hypercubeTestData.clinicalData.sampleStudy])

        when:
        def patientIds = hypercubeTestData.clinicalData.sampleClinicalFacts*.patientId
        PatientSetConstraint subqueryConstraint = new PatientSetConstraint(
                patientIds: patientIds
        )
        def expectedResults = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll{ it.modifierCd == '@'}
        assert expectedResults.size() < hypercubeTestData.clinicalData.ehrClinicalFacts.size()

        DetachedCriteria criteria = builder.buildCriteria(subqueryConstraint)
        List results = getList(criteria)

        then:
        results.size() == expectedResults.size()
        results.sort() == expectedResults.sort()
    }

    void 'test CriteriaQueryBuilder with modifier constraints'() {
        setupHypercubeData()
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forStudies([hypercubeTestData.clinicalData.sampleStudy])

        // test modifierCode + textValue.equals
        when:
        ValueConstraint valueConstraint = new ValueConstraint()
        valueConstraint.valueType = Type.STRING
        valueConstraint.operator = Operator.EQUALS
        valueConstraint.value = 'CONNECTIVE TISSUE'
        ModifierConstraint subqueryConstraint = new ModifierConstraint(
                modifierCode: 'TEST:TISSUETYPE',
                values: valueConstraint
        )
        def modifierFacts = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll{
            it.modifierCd == 'TEST:TISSUETYPE' && it.textValue == 'CONNECTIVE TISSUE'
        }
        def expectedResults = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll { fact ->
            fact.modifierCd == '@' &&
            modifierFacts.find { modifier ->
                fact.encounterNum == modifier.encounterNum
                fact.patient == modifier.patient
                fact.conceptCode == modifier.conceptCode
                fact.providerId == modifier.providerId
                fact.startDate == modifier.startDate
                fact.instanceNum == modifier.instanceNum
            }
        }
        DetachedCriteria criteria = builder.buildCriteria(subqueryConstraint)
        List results = getList(criteria)

        then:
        results.size() == expectedResults.size()
        results.sort() == expectedResults.sort()

        // test modifierPath subquery(modifierDimensions) + numericalValue.greaterThan
        when:
        valueConstraint = new ValueConstraint()
        valueConstraint.valueType = Type.NUMERIC
        valueConstraint.operator = Operator.GREATER_THAN
        valueConstraint.value = 325
        subqueryConstraint = new ModifierConstraint(
                path: hypercubeTestData.clinicalData.modifierDimensions[0].path,
                values: valueConstraint
        )
        modifierFacts = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll{
            it.modifierCd == 'TEST:DOSE' && it.numberValue > 325
        }
        expectedResults = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll { fact ->
            fact.modifierCd == '@' &&
            modifierFacts.find { modifier ->
                fact.encounterNum == modifier.encounterNum
                fact.patient == modifier.patient
                fact.conceptCode == modifier.conceptCode
                fact.providerId == modifier.providerId
                fact.startDate == modifier.startDate
                fact.instanceNum == modifier.instanceNum
            }
        }
        criteria = builder.buildCriteria(subqueryConstraint)
        results = getList(criteria)

        then:
        results.size() == expectedResults.size()
        results == expectedResults

        // test modifierPath subquery(modifierDimensions) without value constraint(optional)
        when:
        subqueryConstraint = new ModifierConstraint(
                path: hypercubeTestData.clinicalData.modifierDimensions[0].path,
        )
        modifierFacts = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll{
            it.modifierCd == 'TEST:DOSE'
        }
        expectedResults = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll { fact ->
            fact.modifierCd == '@' &&
            modifierFacts.find { modifier ->
                fact.encounterNum == modifier.encounterNum
                fact.patient == modifier.patient
                fact.conceptCode == modifier.conceptCode
                fact.providerId == modifier.providerId
                fact.startDate == modifier.startDate
                fact.instanceNum == modifier.instanceNum
            }
        }
        criteria = builder.buildCriteria(subqueryConstraint)
        results = getList(criteria)

        then:
        results.size() == expectedResults.size()
        results.sort() == expectedResults.sort()

        when:
        subqueryConstraint = new ModifierConstraint(
                dimensionName: hypercubeTestData.clinicalData.tissueTypeDimension.name
        )
        modifierFacts = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll {
           it.modifierCd == hypercubeTestData.clinicalData.tissueTypeDimension.modifierCode
        }
        expectedResults = hypercubeTestData.clinicalData.sampleClinicalFacts.findAll { fact ->
            fact.modifierCd == '@' &&
                    modifierFacts.find { modifier ->
                        fact.encounterNum == modifier.encounterNum
                        fact.patient == modifier.patient
                        fact.conceptCode == modifier.conceptCode
                        fact.providerId == modifier.providerId
                        fact.startDate == modifier.startDate
                        fact.instanceNum == modifier.instanceNum
                    }
        }
        criteria = builder.buildCriteria(subqueryConstraint)
        results = getList(criteria)

        then:
        results.size() == expectedResults.size()
        results.sort() == expectedResults.sort()
    }

    void 'test queries for empty database'() {
        given: 'No studies loaded'
        TestData.prepareCleanDatabase()
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forStudies([])

        when: 'Querying for all observations'
        def criteria = builder.buildCriteria(new TrueConstraint())
        def results = getList(criteria) as List

        then: 'The result is empty'
        results.empty
    }

    void 'test queries for user with no access to any study'() {
        given: 'No access to any study'
        setupHypercubeData()
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forStudies([])

        when: 'Querying for all observations'
        def criteria = builder.buildCriteria(new TrueConstraint())
        def results = getList(criteria) as List

        then: 'The result is empty'
        results.empty

        when: 'Querying for a particular study'
        criteria = builder.buildCriteria(new StudyNameConstraint(hypercubeTestData.clinicalData.multidimsStudy.studyId))
        results = getList(criteria) as List

        then: 'The result is empty'
        results.empty
    }

    void 'test queries for a study without trial visits'() {
        given: 'A study without trial visits'
        setupHypercubeData()
        def studyWithoutTrialVisits =
                StudyTestData.createStudy('studyWithoutTrialVisits', ['patient', 'concept'], false)
        studyWithoutTrialVisits.save(flush: true)
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()

        when: 'Querying for the study'
        def criteria = builder.buildCriteria(new StudyNameConstraint(studyWithoutTrialVisits.studyId))
        def results = getList(criteria) as List

        then: 'The result is empty'
        results.empty
    }

    void 'test queries with collection operators'() {
        setupData()
        QueryBuilder builder = HibernateCriteriaQueryBuilder.forAllStudies()

        when: 'A collection operator is used'
        FieldConstraint constraint1 = new FieldConstraint()
        constraint1.field = patientAgeField
        constraint1.operator = Operator.IN
        constraint1.value = [70L, 31L]
        DetachedCriteria criteria1 = builder.buildCriteria(constraint1)
        List results1 = getList(criteria1)

        then: 'Constraint passes validation and results are returned'
        results1.size() == 2

        when: 'A non-collection operator is used'
        FieldConstraint constraint2 = new FieldConstraint()
        constraint2.field = constraint1.field
        constraint2.value = constraint1.value
        constraint2.operator = Operator.CONTAINS
        builder.buildCriteria(constraint2)

        then: 'Validation of the constraint fails'
        def e = thrown(QueryBuilderException)
        e.message.contains("Field type NUMERIC not supported for operator 'contains'.")

    }

}
