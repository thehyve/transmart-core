package org.transmartproject.db.multidimquery

import grails.converters.JSON
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.AccessLevelTestData
import spock.lang.Ignore

import java.sql.Timestamp

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

    Constraint createQueryForConcept(String conceptCode) {
        def conceptDimension = ConceptDimension.findByConceptCode(conceptCode)
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
                                            type:   'study_name',
                                            studyId:  it.studyId
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
                    type:   'study_name',
                    studyId:  hypercubeTestData.clinicalData.longitudinalStudy.studyId
                ]
            ]
        ]
        Constraint constraint = ConstraintFactory.create(constraintMap)
        String constraintJson = constraintMap as JSON
        String apiVersion = "2.1-tests"

        when: "I query for all observations and patients for a constraint"
        def observations = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()
        def patients = multiDimService.getDimensionElements(multiDimService.getDimension('patient'),
                constraint, accessLevelTestData.users[0]).toList()

        then: "I get the expected number of observations and patients"
        observations.size() == hypercubeTestData.clinicalData.longitudinalClinicalFacts.size()
        patients.size() == 3

        then: "the set of patients matches the patients associated with the observations"
        observations*.getAt(DimensionImpl.PATIENT) as Set == patients as Set

        when: "I build a patient set based on the constraint"
        def patientSet = multiDimService.createPatientSetQueryResult("Test set",
                                                       constraint,
                                                       accessLevelTestData.users[0],
                                                       constraintJson.toString(),
                                                       apiVersion)
        then: "I get a patient set id"
        patientSet != null
        patientSet.id != null
        patientSet.queryResultType.id == QueryResultType.PATIENT_SET_ID

        when: "I query for patients based on the patient set id"
        Constraint patientSetConstraint = ConstraintFactory.create(
                [ type: 'patient_set', patientSetId: patientSet.id ]
        )
        def patients2 = multiDimService.getDimensionElements(multiDimService.getDimension('patient'),
                patientSetConstraint, accessLevelTestData.users[0]).toList()

        then: "I get the same set of patient as before"
        patients as Set == patients2 as Set

        when: "I query for patients based on saved constraints"
        def setConstraint = patientSet.queryInstance.queryMaster.requestConstraints
        def setVersion = patientSet.queryInstance.queryMaster.apiVersion
        def savedPatientSetConstraint = ConstraintFactory.create(JSON.parse(setConstraint) as Map)
        def patients3 = multiDimService.getDimensionElements(multiDimService.getDimension('patient'),
                savedPatientSetConstraint, accessLevelTestData.users[0]).toList()

        then: "I get the same set of patient as before"
        patients as Set == patients3 as Set
        setVersion == apiVersion
    }

    void "test query for all patient sets"() {
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
                                type:   'study_name',
                                studyId:  hypercubeTestData.clinicalData.longitudinalStudy.studyId
                        ]
                ]
        ]
        Constraint constraint = ConstraintFactory.create(constraintMap)
        String constraintJson = constraintMap as JSON
        String apiVersion = "2.1-tests"
        def adminUser = accessLevelTestData.users[0]
        def otherUser = accessLevelTestData.users[3]
        def patientSet = multiDimService.createPatientSetQueryResult("Test admin set ",
                constraint,
                adminUser,
                constraintJson.toString(),
                apiVersion)

        when: "I query for all patient sets with admin user"
        def adminPatientSetList = multiDimService.findPatientSetQueryResults(adminUser)

        then: "List of all patient_sets contains the newly created one for admin user"
        assert adminPatientSetList.contains(patientSet)

        when: "I query for all patient sets with a different user"
        def otherUserPatientSetList = multiDimService.findPatientSetQueryResults(otherUser)

        then: "List of all patient_sets does NOT contain the newly created patient set"
        assert !otherUserPatientSetList.contains(patientSet)
    }

    void "test observation count and patient count"() {
        setupData()
        ObservationFact of1 = createObservationWithSameConcept()
        of1.numberValue = 50
        testData.clinicalData.facts << of1
        testData.saveAll()
        def query = createQueryForConcept(of1.conceptCode)

        when:
        def counts = multiDimService.counts(query, accessLevelTestData.users[0])
        def patientCount1 = multiDimService.getDimensionElementsCount(DimensionImpl.PATIENT, query, accessLevelTestData.users[0])

        then:
        counts.observationCount == 2L
        counts.patientCount == 2L
        counts.patientCount == patientCount1

        when:
        ObservationFact of2 = createObservationWithSameConcept()
        of2.numberValue = 51
        of2.patient = of1.patient
        testData.clinicalData.facts << of2
        testData.saveAll()
        multiDimService.clearCountsCache()
        def counts2 = multiDimService.counts(query, accessLevelTestData.users[0])
        def patientCount2 = multiDimService.getDimensionElementsCount(DimensionImpl.PATIENT, query, accessLevelTestData.users[0])

        then:
        counts2.observationCount == counts.observationCount + 1
        patientCount2 == patientCount1
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

    void "test query for 'trial visit' dimension elements"() {
        setupHypercubeData()
        DimensionImpl dimension = DimensionImpl.TRIAL_VISIT
        Constraint constraint = new StudyNameConstraint(studyId: hypercubeTestData.clinicalData.multidimsStudy.studyId )
        def expectedResults = hypercubeTestData.clinicalData.multidimsStudy.trialVisits as Set<TrialVisit>

        when:"I query for all trial visits for a constraint"
        def trialVisits = multiDimService.getDimensionElements(dimension, constraint, accessLevelTestData.users[1]).collect {
            dimension.asSerializable(it)
        }

        then: "List of all trial visits matching the constraints is returned"
        trialVisits.size() == expectedResults.size()
        expectedResults.every { expected ->
            trialVisits.any {
                it.id == expected.id &&
                        it.relTime == expected.relTime &&
                        it.relTimeLabel == expected.relTimeLabel &&
                        it.relTimeUnit == expected.relTimeUnit
            }
        }

        when:"I query for trial visits count"
        def trialVisitsCount = multiDimService.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[1])

        then: "Number of trial visits matching the constraints is returned"
        trialVisitsCount == Long.valueOf(expectedResults.grep().size())
    }

    void "test query for modifier dimensions elements"() {
        setupHypercubeData()
        DimensionImpl dimension = hypercubeTestData.clinicalData.doseDimension
        Constraint constraint = new StudyNameConstraint(studyId: hypercubeTestData.clinicalData.sampleStudy.studyId )
        def expectedResults = hypercubeTestData.clinicalData.sampleClinicalFacts.
                findAll {it.modifierCd == dimension.modifierCode}?.numberValue as Set<BigDecimal>

        when:"I query for all dose dimension elements from sample study"
        def doseDimElements = multiDimService.getDimensionElements(dimension, constraint, accessLevelTestData.users[0]).collect {
            dimension.asSerializable(it)
        }

        then: "List of all dose dimension elements matching the constraints is returned"
        doseDimElements.size() == expectedResults.size()
        doseDimElements.sort() == expectedResults.sort()

        when:"I query for dose dimension elements count"
        def doseDimElementsCount = multiDimService.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

        then: "Number of trial visits matching the constraints is returned"
        doseDimElementsCount == Long.valueOf(expectedResults.grep().size())
    }

    void "test query for SPARSE dimensions elements"() {
        setupHypercubeData()
        Constraint constraint = new StudyNameConstraint(studyId: hypercubeTestData.clinicalData.ehrStudy.studyId)

        when: "I query for all start time dimension elements"
        DimensionImpl dimension = DimensionImpl.START_TIME
        def expectedResults = hypercubeTestData.clinicalData.ehrClinicalFacts.startDate.collect {
            it?.toTimestamp()
        } as Set<Timestamp>
        def startTimes = multiDimService.getDimensionElements(dimension, constraint, accessLevelTestData.users[0]).collect {
            dimension.asSerializable(it)
        }

        then: "List of all start dates matching the constraints is returned"
        startTimes.size() == expectedResults.size()
        startTimes.sort() == expectedResults.sort()

        when:"I query for start time dimension elements count"
        def startTimesCount = multiDimService.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

        then: "Number of start time dimension elements matching the constraints is returned"
        startTimesCount == Long.valueOf(expectedResults.grep().size())

        when: "I query for all end time dimension elements"
        dimension = DimensionImpl.END_TIME
        expectedResults = hypercubeTestData.clinicalData.ehrClinicalFacts.endDate.collect {
            it?.toTimestamp()
        } as Set<Timestamp>
        def endTimes = multiDimService.getDimensionElements(dimension, constraint, accessLevelTestData.users[0]).collect {
            dimension.asSerializable(it)
        }
        then: "List of all end dates matching the constraints is returned"
        endTimes.size() == expectedResults.size()
        endTimes.sort() == expectedResults.sort()

        when:"I query for end time dimension elements count"
        def endTimesCount = multiDimService.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

        then: "Number of end time dimension elements matching the constraints is returned"
        endTimesCount != Long.valueOf(expectedResults.size()) // null element is not included in the elements count
        endTimesCount == Long.valueOf(expectedResults.grep().size())

        when: "I query for all location dimension elements"
        dimension = DimensionImpl.LOCATION
        expectedResults = hypercubeTestData.clinicalData.ehrClinicalFacts.locationCd as Set<String>
        def locations = multiDimService.getDimensionElements(dimension, constraint, accessLevelTestData.users[0]).collect {
            dimension.asSerializable(it)
        }
        then: "List of all locations matching the constraints is returned"
        locations.size() == expectedResults.size()
        locations.sort() == expectedResults.sort()

        when:"I query for locations count"
        def locationsCount = multiDimService.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

        then: "Number of locations matching the constraints is returned"
        locationsCount == Long.valueOf(expectedResults.grep().size())
    }

}
