package org.transmartproject.db.multidimquery

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
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.query.*

@Slf4j
@Rollback
@Integration
class HibernateCriteriaQueryBuilderSpec extends TransmartSpecification {

    Field patientAgeField
    Field conceptCodeField
    TestData testData
    TestData hypercubeTestData

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
        patientAgeField = new Field(dimension: DimensionImpl.PATIENT.name, fieldName: 'age', type: Type.NUMERIC)
        conceptCodeField = new Field(dimension: DimensionImpl.CONCEPT.name, fieldName: 'conceptCode', type: Type.STRING)

        testData = new TestData().createDefault()
        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.saveAll()
    }

    void setupHypercubeData(){
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

        QueryBuilder builder = new HibernateCriteriaQueryBuilder(true)

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
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(true)

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
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(true)

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
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(true)

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
        builder = new HibernateCriteriaQueryBuilder(true)
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
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(true)

        def criteria = builder.buildCriteria(constraint)

        when:
        def result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        result

        when:
        builder = new HibernateCriteriaQueryBuilder(true)
        criteria = builder.buildCriteria(new Negation(arg: constraint))
        result = exists(criteria)
        log.info "Exists: ${result}"

        then:
        result

        when:
        constraint.value = "xyzabc"
        builder = new HibernateCriteriaQueryBuilder(true)
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
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(true)
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
        def builder = new HibernateCriteriaQueryBuilder(true)

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
        QueryBuilder builder = new HibernateCriteriaQueryBuilder([hypercubeTestData.clinicalData.sampleStudy])

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
        QueryBuilder builder = new HibernateCriteriaQueryBuilder([hypercubeTestData.clinicalData.sampleStudy])

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
}
