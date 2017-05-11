package org.transmartproject.db.multidimquery

import grails.converters.JSON
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.AggregateType
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.AccessLevelTestData
import spock.lang.Ignore

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

@Rollback
@Integration
class QueryServiceSpec extends TransmartSpecification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    SessionFactory sessionFactory

    TestData testData
    AccessLevelTestData accessLevelTestData
    TestData hypercubeTestData

    void setupData() {
        testData = new TestData().createDefault()
        testData.mrnaData.patients = testData.i2b2Data.patients

        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.i2b2Data.patients[2].age = 18
        accessLevelTestData = new AccessLevelTestData().createWithAlternativeConceptData(testData.conceptData)
        accessLevelTestData.i2b2Secures.each {
            if (it.secureObjectToken == 'EXP:PUBLIC') {
                it.secureObjectToken = Study.PUBLIC
            }
        }
        testData.saveAll()
        accessLevelTestData.saveAll()
    }

    void setupHypercubeData(){
        hypercubeTestData = TestData.createHypercubeDefault()
        hypercubeTestData.saveAll()

        accessLevelTestData = new AccessLevelTestData()
        save accessLevelTestData.accessLevels
        save accessLevelTestData.roles
        save accessLevelTestData.groups
        save accessLevelTestData.users
        accessLevelTestData.users[0].addToRoles(accessLevelTestData.roles.find { it.authority == 'ROLE_ADMIN' })
        accessLevelTestData.users[1].addToGroups(accessLevelTestData.groups.find { it.category == 'group_-201' })
        sessionFactory.currentSession.flush()
    }

    Constraint createQueryForConcept(ObservationFact observationFact) {
        def conceptCode = observationFact.conceptCode
        def conceptDimension = ConceptDimension.find {
            conceptCode == conceptCode
        }
        new ConceptConstraint(path: conceptDimension.conceptPath)
    }

    ObservationFact findObservation(String type='N') {
        testData.clinicalData.facts.find { it.valueType == type }
    }

    ObservationFact createObservationWithSameConcept(ObservationFact obs = findObservation()) {
        def clinicalTestdata = testData.clinicalData
        def conceptDimension = testData.conceptData.conceptDimensions.find { it.conceptCode == obs.conceptCode }
        def patientsWithConcept = clinicalTestdata.facts.collect {
            if (it.conceptCode == conceptDimension.conceptCode) {
                it.patient
            }
        }
        def patientDimension = clinicalTestdata.patients.find {
            !patientsWithConcept.contains(it)
        }

        ObservationFact observationFact = clinicalTestdata.createObservationFact(
                conceptDimension, patientDimension, clinicalTestdata.DUMMY_ENCOUNTER_ID, obs.value
        )

        observationFact
    }

    ConceptDimension createNewConcept(String templateConceptCode) {
        def c = ConceptDimension.findByConceptCode(templateConceptCode)
        new ConceptDimension(conceptPath: c.conceptPath+"2", conceptCode: c.conceptCode+"2")
    }

    void "test query for all observations"() {
        setupHypercubeData()

        Constraint constraint = new OrConstraint(args: hypercubeTestData.clinicalData.allHypercubeStudies.collect {
            new StudyObjectConstraint(it)})

        when:
        def result = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()

        then:
        result.size() == hypercubeTestData.clinicalData.allHypercubeFacts.findAll { it.modifierCd == '@' }.size()
    }

    void "test query for values > 1 and subject id 2"() {
        setupHypercubeData()

        Constraint constraint = ConstraintFactory.create([
                type    : 'combination',
                operator: 'and',
                args    : [
                        [
                                type     : 'value',
                                valueType: 'NUMERIC',
                                operator : '>',
                                value    : 1
                        ],
                        [
                                type    : 'field',
                                field   : [dimension: 'patient', fieldName: 'sourcesystemCd'],
                                operator: 'contains',
                                value   : 'SUBJ_ID_2'
                        ],
                        [
                                type    : 'or',
                                args    : hypercubeTestData.clinicalData.allHypercubeStudies.collect {
                                    [
                                            type:   'study',
                                            study:  it
                                    ]
                                }
                        ]
                ]
        ])

        when:
        def observations = ObservationFact.findAll {
            modifierCd == '@'
            valueType == ObservationFact.TYPE_NUMBER
            numberValue > 1
            createAlias('patient', 'p')
            like('p.sourcesystemCd', '%SUBJ_ID_2%')
        }
        def result = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()

        then:
        result.size() == observations.size()
        result[0].value.class in Number
        result[0].value > 1
        result[0][DimensionImpl.PATIENT].sourcesystemCd.contains('SUBJ_ID_2')
    }

    void "test patient query and patient set creation"() {
        setupHypercubeData()

        def constraintMap = [
                type    : 'and',
                args    : [
                [
                    type    : 'or',
                    args    : [
                            [ type: 'concept', path: '\\foo\\concept 5\\' ],
                            [ type: 'concept', path: '\\foo\\concept 6\\' ]
                    ]
                ], [
                    type:   'study',
                    study:  hypercubeTestData.clinicalData.longitudinalStudy
                ]
            ]
        ]
        Constraint constraint = ConstraintFactory.create(constraintMap)
        String constraintJson = constraintMap as JSON
        String apiVersion = "2.1-tests"

        when: "I query for all observations and patients for a constraint"
        def observations = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()
        def patients = multiDimService.listPatients(constraint, accessLevelTestData.users[0])

        then: "I get the expected number of observations and patients"
        observations.size() == hypercubeTestData.clinicalData.longitudinalClinicalFacts.size()
        patients.size() == 3

        then: "I set of patients matches the patients associated with the observations"
        observations*.getAt(DimensionImpl.PATIENT) as Set == patients as Set

        when: "I build a patient set based on the constraint"
        def patientSet = multiDimService.createPatientSet("Test set",
                                                       constraint,
                                                       accessLevelTestData.users[0],
                                                       constraintJson.toString(),
                                                       apiVersion)
        then: "I get a patient set id"
        patientSet != null
        patientSet.id != null

        when: "I query for patients based on the patient set id"
        Constraint patientSetConstraint = ConstraintFactory.create(
                [ type: 'patient_set', patientSetId: patientSet.id ]
        )
        def patients2 = multiDimService.listPatients(patientSetConstraint, accessLevelTestData.users[0])

        then: "I get the same set of patient as before"
        patients as Set == patients2 as Set

        when: "I query for patients based on saved constraints"
        def constraintAndVersion = multiDimService.getPatientSetConstraint(patientSet.id)
        def savedPatientSetConstraint = ConstraintFactory.create(JSON.parse(constraintAndVersion.constraint) as Map)
        def patients3 = multiDimService.listPatients(savedPatientSetConstraint, accessLevelTestData.users[0])

        then: "I get the same set of patient as before"
        patients as Set == patients3 as Set
        constraintAndVersion.version == apiVersion
    }

    void "test for max, min, average aggregate"() {
        setupData()

        ObservationFact observationFact = createObservationWithSameConcept()
        observationFact.numberValue = 50
        testData.clinicalData.facts << observationFact

        testData.saveAll()
        def query = createQueryForConcept(observationFact)

        when:
        def result = multiDimService.aggregate([AggregateType.MAX], query, accessLevelTestData.users[0])

        then:
        result.max == 50

        when:
        result = multiDimService.aggregate([AggregateType.MIN], query, accessLevelTestData.users[0])

        then:
        result.min == 10

        when:
        result = multiDimService.aggregate([AggregateType.AVERAGE], query, accessLevelTestData.users[0])

        then:
        result.average == 30 //(10+50) / 2

        when:
        result = multiDimService.aggregate([AggregateType.MIN, AggregateType.MAX, AggregateType.AVERAGE], query,
                accessLevelTestData.users[0])
        then:
        result.min == 10
        result.max == 50
        result.average == 30
    }

    void "test for values aggregate"() {
        setupData()

        def fact = findObservation('T')
        int instanceId = fact.instanceNum
        def facts = (1..3).collect {createObservationWithSameConcept(fact)}
        facts[0].textValue = "hello"
        facts[0].instanceNum = ++instanceId
        facts[1].textValue = "you"
        facts[1].instanceNum = ++instanceId
        facts[2].textValue = "there"
        facts[2].instanceNum = ++instanceId
        testData.clinicalData.facts += facts

        facts*.save()
        def query = createQueryForConcept(facts[0])

        when:
        def result = multiDimService.aggregate([AggregateType.VALUES], query, accessLevelTestData.users[0])

        then:
        [fact.textValue, "hello", "you", "there"] as Set == result.values as Set
    }

    void "test observation count and patient count"() {
        setupData()
        ObservationFact of1 = createObservationWithSameConcept()
        of1.numberValue = 50
        testData.clinicalData.facts << of1
        testData.saveAll()
        def query = createQueryForConcept(of1)

        when:
        def count1 = multiDimService.count(query, accessLevelTestData.users[0])
        def patientCount1 = multiDimService.countPatients(query, accessLevelTestData.users[0])

        then:
        count1 == 2L
        patientCount1 == 2L

        when:
        ObservationFact of2 = createObservationWithSameConcept()
        of2.numberValue = 51
        of2.patient = of1.patient
        testData.clinicalData.facts << of2
        testData.saveAll()
        def count2 = multiDimService.count(query, accessLevelTestData.users[0])
        def patientCount2 = multiDimService.countPatients(query, accessLevelTestData.users[0])

        then:
        count2 == count1 + 1
        patientCount2 == patientCount1
    }

    void "test for check if aggregate returns error when any numerical value is null"() {
        setupData()

        def observationFact = createObservationWithSameConcept()
        def newConcept = createNewConcept(observationFact.conceptCode).save()

        observationFact.numberValue = null
        observationFact.textValue = 'E'
        observationFact.valueType = 'N'
        observationFact.conceptCode = newConcept.conceptCode
        testData.clinicalData.facts << observationFact.save()

        when:
        Constraint query = createQueryForConcept(observationFact)
        multiDimService.aggregate([AggregateType.MAX], query, accessLevelTestData.users[0])

        then:
        thrown(DataInconsistencyException)
    }

    void "test for check if aggregate returns error when the textValue is other then E and no numeric value"() {
        setupData()

        def observationFact = createObservationWithSameConcept()
        def newConcept = createNewConcept(observationFact.conceptCode).save()
        observationFact.textValue = 'GT'
        observationFact.numberValue = null
        observationFact.conceptCode = newConcept.conceptCode
        testData.clinicalData.facts << observationFact.save()

        when:
        Constraint query = createQueryForConcept(observationFact)
        multiDimService.aggregate([AggregateType.MAX], query, accessLevelTestData.users[0])

        then:
        thrown(DataInconsistencyException)
    }

    void "test_patient_selection_constraint"() {
        setupHypercubeData()

        def testObservation = hypercubeTestData.clinicalData.longitudinalClinicalFacts[-1]
        Constraint constraint = new AndConstraint(args: [
                new StudyObjectConstraint(study: hypercubeTestData.clinicalData.longitudinalStudy),
                new SubSelectionConstraint(
                        dimension: DimensionImpl.PATIENT,
                        constraint: new ValueConstraint(
                                valueType: "STRING",
                                operator: Operator.EQUALS,
                                value: testObservation.textValue
                        )
                )
        ])

        when:
        def result = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()
        def expectedObservations = hypercubeTestData.clinicalData.longitudinalClinicalFacts.findAll {
            it.patient == testObservation.patient
        }

        then:
        result.size() == expectedObservations.size()
        result.every { it[DimensionImpl.PATIENT] == testObservation.patient }
        result*.value.sort() == expectedObservations*.value.sort()
    }

    void "test_study_selection_constraint"() {
        setupHypercubeData()

        def testObservation = hypercubeTestData.clinicalData.longitudinalClinicalFacts[-1]
        Constraint constraint = new SubSelectionConstraint(
                dimension: DimensionImpl.STUDY,
                constraint: new ValueConstraint(
                        valueType: "STRING",
                        operator: Operator.EQUALS,
                        value: testObservation.textValue
                )
        )

        when:
        def result = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()
        def expectedObservations = hypercubeTestData.clinicalData.longitudinalClinicalFacts

        then:
        result.size() == expectedObservations.size()
        result.every { it[DimensionImpl.STUDY] == testObservation.trialVisit.study }
        result*.value.sort() == expectedObservations*.value.sort()
    }

    @Ignore("H2 does not support tuple comparisons with IN, which is used for subselections on visits, so this functionality will only work with Postgres or Oracle (Error: Subquery is not a single column query)")
    void "test_visit_selection_constraint"() {
        setupHypercubeData()

        def testObservation = hypercubeTestData.clinicalData.ehrClinicalFacts[0]
        Constraint constraint = new AndConstraint(args: [
                new StudyObjectConstraint(study: hypercubeTestData.clinicalData.ehrStudy),
                new SubSelectionConstraint(
                        dimension: DimensionImpl.VISIT,
                        constraint: new ValueConstraint(
                                valueType: "NUMERIC",
                                operator: Operator.EQUALS,
                                value: testObservation.numberValue
                        )
                )
        ])

        when:
        def result = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()
        def expectedObservations = hypercubeTestData.clinicalData.ehrClinicalFacts.findAll {
            it.visit == testObservation.visit
        }

        then:
        result.size() == expectedObservations.size()
        result.every { it[DimensionImpl.VISIT] == testObservation.visit }
        result*.value.sort() == expectedObservations*.value.sort()
    }

    void "test query for dimension elements"() {
        setupHypercubeData()

        when: "Dimension name is 'trial visit'"
        String dimensionName = DimensionImpl.TRIAL_VISIT.name
        Constraint constraint = new StudyNameConstraint(studyId: hypercubeTestData.clinicalData.multidimsStudy.studyId )
        def expectedResult = hypercubeTestData.clinicalData.multidimsStudy.trialVisits as Set<TrialVisit>
        def result = multiDimService.listDimensionElements(dimensionName, accessLevelTestData.users[1], constraint)

        then: "TrialVisit dimension elements are returned"
        result.size() == expectedResult.size()
        expectedResult.every { expected ->
            result.any {
                it.id == expected.id &&
                it.relTime == expected.relTime &&
                it.relTimeLabel == expected.relTimeLabel &&
                it.relTimeUnit == expected.relTimeUnit
            }
        }

        when: "Dimension name is incorrect"
        dimensionName = 'incorrect name'
        multiDimService.listDimensionElements(dimensionName, accessLevelTestData.users[1], null)

        then: "InvalidArgumentsException is thrown"
        def e = thrown(InvalidArgumentsException)
        e.message == "dimension $dimensionName is not a valid dimension or dimension name"
    
        when: "Dimension with given name does not support listing elements"
        dimensionName = DimensionImpl.START_TIME.name
        multiDimService.listDimensionElements(dimensionName, accessLevelTestData.users[1], null)
    
        then: "InvalidArgumentsException is thrown"
        e = thrown(InvalidArgumentsException)
        e.message == "Dimension not supported."
    }
}
