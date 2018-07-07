package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.hibernate.internal.SessionImpl
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.SortSpecification
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.db.TestData
import spock.lang.Specification
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.user.AccessLevelTestData
import spock.lang.Ignore

import java.sql.Timestamp

import static org.transmartproject.db.multidimquery.DimensionImpl.*

@Rollback
@Integration
class QueryServiceSpec extends Specification {

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    SystemResource systemResource

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    AggregateDataResource aggregateDataResource

    @Autowired
    PatientSetResource patientSetResource

    TestData testData
    AccessLevelTestData accessLevelTestData
    TestData hypercubeTestData

    void setupData() {
        TestData.clearAllData()

        testData = TestData.createDefault()
        testData.mrnaData.patients = testData.i2b2Data.patients

        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.i2b2Data.patients[2].age = 18
        accessLevelTestData = AccessLevelTestData.createWithAlternativeConceptData(testData.conceptData)
        accessLevelTestData.i2b2Secures.each {
            if (it.secureObjectToken == 'EXP:PUBLIC') {
                it.secureObjectToken = Study.PUBLIC
            }
        }
        testData.saveAll()
        accessLevelTestData.saveAll()
        sessionFactory.currentSession.flush()
    }

    void setupHypercubeData(){
        TestData.clearAllData()

        hypercubeTestData = TestData.createHypercubeDefault()
        hypercubeTestData.saveAll()

        accessLevelTestData = new AccessLevelTestData()
        accessLevelTestData.saveAuthorities()

        sessionFactory.currentSession.flush()
        ((SessionImpl)sessionFactory.currentSession).connection().commit()
    }

    Constraint createQueryForConcept(String conceptCode) {
        def concept = ConceptDimension.findByConceptCode(conceptCode)
        new ConceptConstraint(path: concept.conceptPath)
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

        Constraint constraint = new OrConstraint(hypercubeTestData.clinicalData.allHypercubeStudies.collect {
            new StudyObjectConstraint(it)})

        when:
        def result = multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()

        then:
        result.size() == hypercubeTestData.clinicalData.allHypercubeFacts.findAll { it.modifierCd == '@' }.size()
    }

    void "test query for patients with values > 1 and subject id 2"() {
        setupHypercubeData()

        Constraint constraint = new AndConstraint([
                new ValueConstraint(Type.NUMERIC, Operator.GREATER_THAN, 1),
                new FieldConstraint(
                        new Field('patient', Type.STRING, 'sourcesystemCd'),
                        Operator.CONTAINS, 'SUBJ_ID_2')
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

    private List<HypercubeValue> getObservationsList(Constraint constraint) {
        multiDimService.retrieveClinicalData(constraint, accessLevelTestData.users[0]).asList()
    }

    private List<HypercubeValue> getObservationsList(DataRetrievalParameters args) {
        multiDimService.retrieveClinicalData(args, accessLevelTestData.users[0]).asList()
    }

    private List<Patient> getPatients(Constraint constraint) {
        multiDimService.getDimensionElements(multiDimService.getDimension('patient'),
                constraint, accessLevelTestData.users[0]).toList()
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
        String apiVersion = "2.1-tests"

        when: "I query for all observations and patients for a constraint"
        def observations = getObservationsList(constraint)
        def patients = getPatients(constraint)

        then: "I get the expected number of observations and patients"
        observations.size() == hypercubeTestData.clinicalData.longitudinalClinicalFacts.size()
        patients.size() == 3

        then: "the set of patients matches the patients associated with the observations"
        observations*.getAt(DimensionImpl.PATIENT) as Set == patients as Set

        when: "I build a patient set based on the constraint"
        def patientSet = (QtQueryResultInstance)patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                accessLevelTestData.users[0],
                apiVersion,
                false)
        then: "I get a patient set id"
        patientSet != null
        patientSet.id != null
        patientSet.queryResultType.id == QueryResultType.PATIENT_SET_ID

        when: "I query for patients based on the patient set id"
        Constraint patientSetConstraint = new PatientSetConstraint(patientSet.id)
        def patients2 = getPatients(patientSetConstraint)

        then: "I get the same set of patient as before"
        patientSet.setSize.intValue() == patients.size()
        patients as Set == patients2 as Set

        when: "I query for patients based on saved constraints"
        def setConstraint = patientSet.queryInstance.queryMaster.requestConstraints as String
        def setVersion = patientSet.queryInstance.queryMaster.apiVersion
        def savedPatientSetConstraint = ConstraintFactory.read(setConstraint)
        def patients3 = getPatients(savedPatientSetConstraint)

        then: "I get the same set of patient as before"
        patients as Set == patients3 as Set
        setVersion == apiVersion
    }

    void "test patient set creation"() {
        setupHypercubeData()
        Constraint constraint = new AndConstraint([
                new SubSelectionConstraint('patient',
                        new StudyNameConstraint(hypercubeTestData.clinicalData.longitudinalStudy.studyId)),
                new SubSelectionConstraint('patient',
                        new OrConstraint([
                                new ConceptConstraint(conceptCode: 'c5'),
                                new ConceptConstraint(conceptCode: 'c6')
                        ])
                )
        ])
        String apiVersion = "2.1-tests"

        when: "I query for all patients for the constraint"
        def patients = getPatients(constraint)

        then: "I get the expected number of patients"
        patients.size() == 3

        when: "I build a patient set based on the constraint"
        def patientSet = (QtQueryResultInstance)patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                accessLevelTestData.users[0],
                apiVersion,
                false)

        then: "I get a patient set id"
        patientSet != null
        patientSet.id != null
        patientSet.queryResultType.id == QueryResultType.PATIENT_SET_ID

        when: "I query for patients based on the patient set id"
        Constraint patientSetConstraint = new PatientSetConstraint(patientSet.id)
        def patients2 = getPatients(patientSetConstraint)

        then: "I get the same set of patient as before"
        patientSet.setSize.intValue() == patients.size()
        patients as Set == patients2 as Set
    }

    void "test patient set creation with negation of the subselection"() {
        setupHypercubeData()
        Constraint constraint = new AndConstraint([
                new SubSelectionConstraint('patient',
                        new StudyNameConstraint(hypercubeTestData.clinicalData.longitudinalStudy.studyId)),
                new Negation(
                        new SubSelectionConstraint('patient', new ConceptConstraint(conceptCode: 'c5')))
        ])
        String apiVersion = "2.1-tests"

        when: "I query for all patients for the constraint"
        def patients = getPatients(constraint)

        then: "I get the expected number of patients"
        patients.size() == 0

        when: "I build a patient set based on the constraint"
        def patientSet = (QtQueryResultInstance)patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                accessLevelTestData.users[0],
                apiVersion,
                false)

        then: "I get a patient set id"
        patientSet != null
        patientSet.id != null
        patientSet.queryResultType.id == QueryResultType.PATIENT_SET_ID

        when: "I query for patients based on the patient set id"
        Constraint patientSetConstraint = new PatientSetConstraint(patientSet.id)
        def patients2 = getPatients(patientSetConstraint)

        then: "I get the same set of patient as before"
        patientSet.setSize.intValue() == patients.size()
        patients as Set == patients2 as Set
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
        String apiVersion = "2.1-tests"
        def adminUser = accessLevelTestData.users[0]
        def otherUser = accessLevelTestData.users[3]
        def patientSet = patientSetResource.createPatientSetQueryResult("Test admin set ",
                constraint,
                adminUser,
                apiVersion,
                false)

        when: "I query for all patient sets with admin user"
        def adminPatientSetList = patientSetResource.findPatientSetQueryResults(adminUser)

        then: "List of all patient_sets contains the newly created one for admin user"
        assert adminPatientSetList.contains(patientSet)

        when: "I query for all patient sets with a different user"
        def otherUserPatientSetList = patientSetResource.findPatientSetQueryResults(otherUser)

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
        def counts1 = aggregateDataResource.counts(query, accessLevelTestData.users[0])

        then:
        counts1.observationCount == 2L
        counts1.patientCount == 2L

        when:
        ObservationFact of2 = createObservationWithSameConcept()
        of2.numberValue = 51
        of2.patient = of1.patient
        testData.clinicalData.facts << of2
        testData.saveAll()
        systemResource.clearCaches()
        def counts2 = aggregateDataResource.counts(query, accessLevelTestData.users[0])

        then:
        counts2.observationCount == counts1.observationCount + 1
        counts2.patientCount == counts1.observationCount
    }

    void "test_patient_selection_constraint"() {
        setupHypercubeData()

        def testObservation = hypercubeTestData.clinicalData.longitudinalClinicalFacts[-1]
        Constraint constraint = new AndConstraint([
                new StudyObjectConstraint(study: hypercubeTestData.clinicalData.longitudinalStudy),
                new SubSelectionConstraint(
                        dimension: DimensionImpl.PATIENT.name,
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
                dimension: DimensionImpl.STUDY.name,
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
        Constraint constraint = new AndConstraint([
                new StudyObjectConstraint(hypercubeTestData.clinicalData.ehrStudy),
                new SubSelectionConstraint(
                        DimensionImpl.VISIT.name,
                        new ValueConstraint(
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
        Constraint constraint = new StudyNameConstraint(hypercubeTestData.clinicalData.multidimsStudy.studyId )
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
        def trialVisitsCount = aggregateDataResource.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[1])

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
        def doseDimElementsCount = aggregateDataResource.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

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
        def startTimesCount = aggregateDataResource.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

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
        def endTimesCount = aggregateDataResource.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

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
        def locationsCount = aggregateDataResource.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

        then: "Number of locations matching the constraints is returned"
        locationsCount == Long.valueOf(expectedResults.grep().size())
    }

    void "test query for study dimension elements"() {
        setupHypercubeData()
        DimensionImpl dimension = DimensionImpl.STUDY
        def testObservation = hypercubeTestData.clinicalData.longitudinalClinicalFacts[-1]
        Constraint constraint = new ValueConstraint(
                        valueType: "STRING",
                        operator: Operator.EQUALS,
                        value: testObservation.textValue
        )
        def expectedResults = hypercubeTestData.clinicalData.longitudinalStudy

        when:"I query for all studies for a constraint with admin user"
        def studies = multiDimService.getDimensionElements(dimension, constraint, accessLevelTestData.users[0]).collect {
            dimension.asSerializable(it)
        }

        then: "List of all studies matching the constraints is returned"
        studies.size() == 1
        studies.any {
            it.name == expectedResults.name
        }

        when:"I query for studies count with admin user"
        def studiesCount = aggregateDataResource.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[0])

        then: "Number of studies matching the constraints is returned"
        studiesCount == 1

        when:"I query for all studies for a constraint with user without access to any study"
        def studies2 = multiDimService.getDimensionElements(dimension, constraint, accessLevelTestData.users[4]).collect {
            dimension.asSerializable(it)
        }

        then: "Empty list is returned with user without access to any study"
        studies2.size() == 0

        when:"I query for studies count"
        def studies2Count = aggregateDataResource.getDimensionElementsCount(dimension, constraint, accessLevelTestData.users[4])

        then: "Number is zero"
        studies2Count == 0
    }

    void 'test_sorting'() {
        setupHypercubeData()

        def studies = modifiers ? hypercubeTestData.clinicalData.allHypercubeStudies :
                hypercubeTestData.clinicalData.with { [longitudinalStudy, ehrStudy, multidimsStudy] }
        Constraint constraint = new OrConstraint(studies.collect { new StudyObjectConstraint(it) })

        when:
        def args = new DataRetrievalParameters(constraint: constraint)
        args.sort = [SortSpecification.asc('patient')]
        def result = getObservationsList(args)

        then:
        result*.getDimKey(PATIENT) == result*.getDimKey(PATIENT).sort(false)

        when:
        args.sort = [SortSpecification.desc('patient')]
        result = getObservationsList(args)

        then:
        result*.getDimKey(PATIENT) == result*.getDimKey(PATIENT).sort(false).reverse()

        when:
        args.sort = [SortSpecification.desc('patient'), SortSpecification.asc('concept')]
        result = getObservationsList(args)

        then:
        result == result.sort(false) { a, b ->
                b.getDimKey(PATIENT) <=> a.getDimKey(PATIENT) ?:
                a.getDimKey(CONCEPT) <=> b.getDimKey(CONCEPT) }

        where:
        modifiers | _
        false     | _
        true      | _
    }

    @Ignore
    /**
     * FIXME: We need to determine which dimensions are sortable, under which conditions.
     * E.g., study and trial visit are not per se sortable, if we do not check consistency of
     * the data in observation_fact, i.e., that rows with the same primary key have the same values
     * for all other sortable dimensions (study, trial visit, end time).
     */
    void 'test_failing_sort'() {
        setupHypercubeData()

        Constraint constraint = new OrConstraint(hypercubeTestData.clinicalData.allHypercubeStudies.collect {
            new StudyObjectConstraint(it) })

        when:
        def args = new DataRetrievalParameters(constraint: constraint)
        args.sort = [SortSpecification.asc(hypercubeTestData.clinicalData.doseDimension.name)]
        getObservationsList(args)

        then:
        thrown InvalidArgumentsException

        when:
        // END_TIME can be made sortable, if it is loaded in a sort-compatible way. If we decide to support that, this
        // test can be removed or test another dimension that cannot be sorted with modifiers present.
        args.sort = [SortSpecification.asc(END_TIME.name)]
        getObservationsList(args)

        then:
        thrown UnsupportedByDataTypeException
    }

}
