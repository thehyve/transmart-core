/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.converters.JSON
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.user.User
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.multidimquery.DimensionImpl.*
import static spock.util.matcher.HamcrestSupport.that

@Rollback
@Integration
class QueryServicePgSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    void 'get whole hd data for single node'() {
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        Hypercube hypercube = multiDimService.highDimension(conceptConstraint, user, 'autodetect')

        then:
        hypercube.toList().size() == hypercube.dimensionElements(BIOMARKER).size() *
                hypercube.dimensionElements(ASSAY).size() *
                hypercube.dimensionElements(PROJECTION).size()
        hypercube.dimensionElements(BIOMARKER).size() == 3
        hypercube.dimensionElements(ASSAY).size() == 6
        hypercube.dimensionElements(PROJECTION).size() == 10

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get hd data for selected patients'() {
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def malesIn26 = org.transmartproject.db.i2b2data.PatientDimension.find {
            sourcesystemCd == 'CLINICAL_TRIAL_HIGHDIM:1'
        }
        Constraint assayConstraint = new PatientSetConstraint(patientIds: malesIn26*.id)
        Constraint combinationConstraint = new Combination(
                operator: Operator.AND,
                args: [
                        conceptConstraint,
                        assayConstraint
                ]
        )

        when:
        Hypercube hypercube = multiDimService.highDimension(combinationConstraint, user, 'autodetect')

        then:
        hypercube.toList().size() == hypercube.dimensionElements(BIOMARKER).size() *
                hypercube.dimensionElements(ASSAY).size() *
                hypercube.dimensionElements(PROJECTION).size()
        hypercube.dimensionElements(BIOMARKER).size() == 3
        hypercube.dimensionElements(ASSAY).size() == 2
        hypercube.dimensionElements(PROJECTION).size() == 10

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get hd data for selected biomarkers'() {
        def user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        BiomarkerConstraint bioMarkerConstraint = new BiomarkerConstraint(
                biomarkerType: DataConstraint.GENES_CONSTRAINT,
                params: [
                        names: ['TP53']
                ]
        )

        when:
        Hypercube hypercube = multiDimService.highDimension(conceptConstraint, bioMarkerConstraint, user, 'mrna')

        then:
        hypercube.toList().size() == hypercube.dimensionElements(BIOMARKER).size() *
                hypercube.dimensionElements(ASSAY).size() *
                hypercube.dimensionElements(PROJECTION).size()
        hypercube.dimensionElements(BIOMARKER).size() == 2
        hypercube.dimensionElements(ASSAY).size() == 6
        hypercube.dimensionElements(PROJECTION).size() == 10

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get hd data for selected trial visit dimension'() {
        def user = User.findByUsername('test-public-user-1')
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def trialVisitConstraint = new FieldConstraint(
                field: new Field(
                        dimension: TRIAL_VISIT,
                        fieldName: 'relTimeLabel',
                        type: 'STRING'
                ),
                operator: Operator.EQUALS
        )
        def combination

        when:
        trialVisitConstraint.value = 'Baseline'
        combination = new Combination(operator: Operator.AND, args: [conceptConstraint, trialVisitConstraint])
        Hypercube hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:
        hypercube.dimensionElements(BIOMARKER).size() == 3
        hypercube.dimensionElements(ASSAY).size() == 4
        hypercube.dimensionElements(PROJECTION).size() == 10

        when:
        trialVisitConstraint.value = 'Week 1'
        combination = new Combination(operator: Operator.AND, args: [conceptConstraint, trialVisitConstraint])
        hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:

        hypercube.dimensionElements(ASSAY).size() == 1
        def patient = hypercube.dimensionElement(PATIENT, 0)
        patient.id == -601
        patient.age == 26

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get hd data for selected time constraint'() {
        def user = User.findByUsername('test-public-user-2')
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def startDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: START_TIME,
                        fieldName: 'startDate',
                        type: 'DATE'
                ),
                values: [sdf.parse('2016-03-29 10:30:30')],
                operator: Operator.AFTER
        )

        def endDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: END_TIME,
                        fieldName: 'endDate',
                        type: 'DATE'
                ),
                values: [sdf.parse('2016-04-02 01:00:00')],
                operator: Operator.BEFORE
        )

        def combination
        Hypercube hypercube
        when:
        combination = new Combination(operator: Operator.AND, args: [conceptConstraint, startDateTimeConstraint])
        hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:
        hypercube.dimensionElements(ASSAY).size() == 3

        when:
        combination = new Combination(operator: Operator.AND, args: [conceptConstraint, endDateTimeConstraint])
        hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:
        hypercube.dimensionElements(ASSAY).size() == 1

        cleanup:
        if (hypercube) hypercube.close()
    }

    void "get hd data types"() {
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        def result = multiDimService.retrieveHighDimDataTypes(conceptConstraint, user)
        then:
        result != null
        result.contains("mrna")
    }

    void 'Clinical data selected on visit dimension'() {
        def user = User.findByUsername('test-public-user-1')
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def minDate = sdf.parse('2016-04-01 10:00:00')
        def visitStartConstraint = new FieldConstraint(
                operator: Operator.AFTER,
                value: minDate,
                field: new Field(
                        dimension: VISIT,
                        fieldName: 'startDate',
                        type: 'DATE'
                )
        )
        def studyNameConstraint = new StudyNameConstraint(
                studyId: 'EHR'
        )
        def combination = new Combination(
                args: [visitStartConstraint, studyNameConstraint],
                operator: Operator.AND
        )
        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(combination, user)
        def observations = hypercube.toList()

        then:
        observations.size() == 2
        hypercube.dimensionElements(VISIT).each {
            assert (it.getAt('startDate') as Date) > minDate
        }

        cleanup:
        if (hypercube) hypercube.close()
    }


    void 'HD data selected on visit dimension'() {
        def user = User.findByUsername('test-public-user-1')
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def timeDimensionConstraint = new FieldConstraint(
                operator: Operator.AFTER,
                value: sdf.parse('2016-05-05 10:00:00'),
                field: new Field(
                        dimension: VISIT,
                        fieldName: 'endDate',
                        type: 'DATE'
                )

        )
        def conceptConstraint = new ConceptConstraint(
                path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\'
        )
        def combination = new Combination(
                args: [timeDimensionConstraint, conceptConstraint],
                operator: Operator.AND
        )
        when:
        Hypercube hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:
        hypercube.dimensionElements(ASSAY).size() == 1

        cleanup:
        if (hypercube) hypercube.close()
    }


    void 'HD data selected based on sample type (modifier)'() {
        def user = User.findByUsername('test-public-user-1')
        def modifierConstraint = new ModifierConstraint(
                modifierCode: 'TNS:SMPL',
                //path: '\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\', //choose either code or path
                values: new ValueConstraint(
                        operator: Operator.EQUALS,
                        valueType: Type.STRING,
                        value: 'Tumor'

                )
        )
        def conceptConstraint = new ConceptConstraint(
                path: '\\Public Studies\\TUMOR_NORMAL_SAMPLES\\HD\\Breast\\'
        )

        def combination = new Combination(
                operator: Operator.AND,
                args: [modifierConstraint, conceptConstraint]
        )
        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(combination, user)
        hypercube.toList()

        then:
        hypercube.dimensionElements(PATIENT).size() == 2

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'Test for empty set of assayIds'() {
        def user = User.findByUsername('test-public-user-1')
        def sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def endDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: END_TIME,
                        fieldName: 'endDate',
                        type: 'DATE'
                ),
                values: [sdf.parse('2016-04-02 01:00:00')],
                operator: Operator.AFTER //only exist one before, none after
        )
        when:
        Combination combination = new Combination(operator: Operator.AND, args: [conceptConstraint, endDateTimeConstraint])
        Hypercube hypercube = multiDimService.highDimension(combination, user, 'autodetect')


        then:
        hypercube.toList().empty

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get data for the patient with given id'() {
        def user = User.findByUsername('test-public-user-1')
        Constraint subjectIdConstraint = new PatientSetConstraint(subjectIds: ['123457'])


        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(subjectIdConstraint, user)
        then:
        hypercube.dimensionElements(PATIENT).size() == 1
        hypercube.dimensionElements(PATIENT).first().id == -3001L

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get transcript data for selected patients and selected transcripts'() {
        def user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\RNASEQ_TRANSCRIPT\\HD\\Breast\\')
        def secondSubject = org.transmartproject.db.i2b2data.PatientDimension.find {
            sourcesystemCd == 'RNASEQ_TRANSCRIPT:2'
        }
        Constraint assayConstraint = new PatientSetConstraint(patientIds: secondSubject*.id)
        Constraint combinationConstraint = new Combination(
                operator: Operator.AND,
                args: [
                        conceptConstraint,
                        assayConstraint
                ]
        )
        BiomarkerConstraint bioMarkerConstraint = new BiomarkerConstraint(
                biomarkerType: DataConstraint.TRANSCRIPTS_CONSTRAINT,
                params: [
                        names: ['tr2']
                ]
        )

        when:
        Hypercube hypercube = multiDimService.highDimension(combinationConstraint, bioMarkerConstraint, user, 'rnaseq_transcript')

        then:
        hypercube.toList().size() == hypercube.dimensionElements(BIOMARKER).size() *
                hypercube.dimensionElements(ASSAY).size() *
                hypercube.dimensionElements(PROJECTION).size()
        hypercube.dimensionElements(BIOMARKER).size() == 1
        hypercube.dimensionElements(ASSAY).size() == 1
        hypercube.dimensionElements(PROJECTION).size() == 13

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get transcript data for selected genes'() {
        def user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\RNASEQ_TRANSCRIPT\\HD\\Breast\\')
        BiomarkerConstraint bioMarkerConstraint = new BiomarkerConstraint(
                biomarkerType: DataConstraint.GENES_CONSTRAINT,
                params: [
                        names: ['AURKA']
                ]
        )

        when:
        Hypercube hypercube = multiDimService.highDimension(conceptConstraint, bioMarkerConstraint, user, 'rnaseq_transcript')

        then:
        hypercube.toList().size() == hypercube.dimensionElements(BIOMARKER).size() *
                hypercube.dimensionElements(ASSAY).size() *
                hypercube.dimensionElements(PROJECTION).size()
        hypercube.dimensionElements(BIOMARKER).size() == 1
        hypercube.dimensionElements(ASSAY).size() == 3
        hypercube.dimensionElements(PROJECTION).size() == 13

        cleanup:
        if (hypercube) hypercube.close()
    }

    // This is basically a copy of QueryServiceSpec.test_visit_selection_constraint in transmart-core-db-tests.
    // Since we cannot run that one due to limitations in the H2 database this version ensures that the functionality
    // is still automatically tested.
    void "test visit selection constraint"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new SubSelectionConstraint(
                dimension: VISIT,
                constraint: new AndConstraint(args: [
                        new ValueConstraint(
                                valueType: "NUMERIC",
                                operator: Operator.EQUALS,
                                value: 59.0
                        ),
                        new StudyNameConstraint(studyId: "EHR")
                ])
        )

        when:
        def result = multiDimService.retrieveClinicalData(constraint, user).asList()
        def visits = result.collect { it[VISIT] } as Set

        then:
        result.size() == 2
        // ensure we are also finding other cells than the value we specified in the constraint
        result.collect { it.value }.any { it != 59.0 }
        visits.size() == 1
    }

    // This test should be a part of QueryServiceSpec tests in transmart-core-db-tests.
    // The same issue as for the test above: since we cannot run that one due to limitations in the H2 database
    // this version ensures that the functionality is still automatically tested.
    void "test query for 'visit' dimension elements"() {
        def user = User.findByUsername('test-public-user-1')
        DimensionImpl dimension = DimensionImpl.VISIT

        Constraint constraint = new StudyNameConstraint(studyId: "EHR")
        def results = multiDimService.retrieveClinicalData(constraint, user).asList()
        def expectedResult = results.collect { it[VISIT] }.findAll { it } as Set

        when: "I query for all visits for a constraint"
        def visits = multiDimService.getDimensionElements(dimension, constraint, user).collect {
            dimension.asSerializable(it)
        }

        then: "List of all trial visits matching the constraints is returned"
        visits.size() == expectedResult.size()
        expectedResult.every { expect ->
            visits.any {
                [expect.patientInTrialId, expect.encounterNum] == [it.patientInTrialId, it.encounterNum]
            }
        }

        //TODO Fix counting elements of visit dimension - find a way to count distinct on two properties
        // (it does not seem possible in the legacy hibernate criteria api)
        // when:"I query for visits count"
        // def locationsCount = multiDimService.getDimensionElementsCount
        // then: "Number of visits matching the constraints is returned"
        // locationsCount == Long.valueOf(expectedResult.size())
    }

    void "test patient set query"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        QueryResult patientSetQueryResult = multiDimService.createPatientSetQueryResult("Test set",
                constraint,
                user,
                (constraint as JSON).toString(),
                'v2')

        then:
        patientSetQueryResult.id > 0
        patientSetQueryResult.queryResultType.id == QueryResultType.PATIENT_SET_ID
        patientSetQueryResult.setSize == 3L
        patientSetQueryResult.status == QueryStatus.FINISHED
        patientSetQueryResult.username == user.name
        !patientSetQueryResult.description.empty
        patientSetQueryResult.errorMessage == null
        def patientSetEntries = QtPatientSetCollection
                .findAllByResultInstance(QtQueryResultInstance.load(patientSetQueryResult.id))
        patientSetEntries.size() == 3
        (patientSetEntries*.setIndex - (1..3 as List)).empty
        patientSetEntries*.patient.id != null
    }

    void "test large text values (raw data type)"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(conceptCode: 'favouritebook')

        when:
        Long observationCount = multiDimService.counts(constraint, user).observationCount
        Hypercube data = multiDimService.retrieveClinicalData(constraint, user)
        def values = data.collect { HypercubeValue value -> value.value as String }

        then:
        observationCount == 2
        that values, hasSize(2)
        that values, hasItems(containsString('The Brothers Karamazov'), containsString('funny dialogues'))
    }

    void "test searching for large text values (raw data type)"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new AndConstraint(args: [
                new ConceptConstraint(conceptCode: 'favouritebook'),
                new ValueConstraint(Type.TEXT, Operator.CONTAINS, 'Karamazov')
        ])

        when:
        Long observationCount = multiDimService.counts(constraint, user).observationCount
        Hypercube data = multiDimService.retrieveClinicalData(constraint, user)
        def values = data.collect { HypercubeValue value -> value.value as String }

        then:
        observationCount == 1
        that values, hasSize(1)
        that values, hasItems(containsString('The Brothers Karamazov'))
    }

    /**
     * Test the functionality to count patients and observations grouped by
     * study, concept, or concept and study.
     */
    void "test counts per study, concept"() {
        def user = User.findByUsername('test-public-user-1')
        Constraint studyConstraint = new StudyNameConstraint(studyId: "EHR")

        when: "fetching all counts per concept for study EHR"
        def countsPerConcept = multiDimService.countsPerConcept(studyConstraint, user)

        then: "the result should contain entries for both concepts in the study"
        !countsPerConcept.empty
        countsPerConcept.keySet() == ['EHR:DEM:AGE', 'EHR:VSIGN:HR'] as Set

        then: "the result should contain the correct counts for both concepts"
        countsPerConcept['EHR:DEM:AGE'].patientCount == 3
        countsPerConcept['EHR:DEM:AGE'].observationCount == 3
        countsPerConcept['EHR:VSIGN:HR'].patientCount == 3
        countsPerConcept['EHR:VSIGN:HR'].observationCount == 9

        when: "fetching all counts per study"
        def countsPerStudy = multiDimService.countsPerStudy(new TrueConstraint(), user)

        then: "the result should contain all study ids as key"
        !countsPerStudy.empty
        countsPerStudy.keySet() == [
                'CATEGORICAL_VALUES',
                'CLINICAL_TRIAL',
                'CLINICAL_TRIAL_HIGHDIM',
                'EHR',
                'EHR_HIGHDIM',
                'MIX_HD',
                'ORACLE_1000_PATIENT',
                'RNASEQ_TRANSCRIPT',
                'SHARED_CONCEPTS_STUDY_A',
                'SHARED_CONCEPTS_STUDY_B',
                'SHARED_HD_CONCEPTS_STUDY_A',
                'SHARED_HD_CONCEPTS_STUDY_B',
                'TUMOR_NORMAL_SAMPLES',
                'SURVEY1',
                'SURVEY2',
        ] as Set

        then: "the result should have the correct counts for study EHR"
        countsPerStudy['EHR'].patientCount == 3
        countsPerStudy['EHR'].observationCount == 12

        when: "fetching all counts per study and concept"
        def countsPerStudyAndConcept = multiDimService.countsPerStudyAndConcept(new TrueConstraint(), user)
        def observationCount = multiDimService.counts(new TrueConstraint(), user).observationCount

        then: "the result should contain the counts for study EHR and concept EHR:VSIGN:HR"
        !countsPerStudyAndConcept.empty
        countsPerStudyAndConcept.keySet() == countsPerStudy.keySet()
        countsPerStudyAndConcept['EHR']['EHR:VSIGN:HR'].patientCount == 3
        countsPerStudyAndConcept['EHR']['EHR:VSIGN:HR'].observationCount == 9

        then: "the total observation count should be equal to the sum of observation counts of the returned map"
        observationCount == countsPerStudyAndConcept.values().sum { Map<String, Counts> countsMap ->
            countsMap.values().sum { Counts counts -> counts.observationCount }
        }
    }

    void 'test numerical value aggregates'() {
        def user = User.findByUsername('test-public-user-1')
        def userWithAccessToMoreData = User.findByUsername('test-public-user-2')

        when:
        def heartRate = new ConceptConstraint(path: '\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\')
        def result = multiDimService.numericalValueAggregatesPerConcept(heartRate, user)
        then: 'expected aggregates are calculated'
        result.size() == 1
        'EHR:VSIGN:HR' in result
        result['EHR:VSIGN:HR'].min == 56d
        result['EHR:VSIGN:HR'].max == 102d
        result['EHR:VSIGN:HR'].avg.round(2) == 74.78d
        result['EHR:VSIGN:HR'].count == 9
        result['EHR:VSIGN:HR'].stdDev.round(2) == 14.7d

        when: 'numerical aggregates run on categorical measures'
        def categoricalConceptConstraint = new ConceptConstraint(
                path: '\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\')
        def emptyResult = multiDimService.numericalValueAggregatesPerConcept(categoricalConceptConstraint, user)
        then: 'no numerical aggregates returned'
        emptyResult.isEmpty()

        when: 'aggregate runs for dataset with one value and one missing value'
        def missingValuesConstraint = new ConceptConstraint(path: '\\Demographics\\Height\\')
        def oneValueResult = multiDimService.numericalValueAggregatesPerConcept(missingValuesConstraint, user)
        then: 'aggregates calculates on the single value'
        oneValueResult.size() == 1
        'height' in oneValueResult
        oneValueResult['height'].min == 169d
        oneValueResult['height'].max == 169d
        oneValueResult['height'].avg == 169d
        oneValueResult['height'].count == 1
        oneValueResult['height'].stdDev == null

        when: 'we calculate aggregates for shared concept with user that don\'t have access to one study'
        def crossStudyHeartRate = new ConceptConstraint(path: '\\Vital Signs\\Heart Rate\\')
        def excludingSecuredRecords = multiDimService
                .numericalValueAggregatesPerConcept(crossStudyHeartRate, user)
        then: 'only values user have access are taken to account'
        excludingSecuredRecords.size() == 1
        'VSIGN:HR' in excludingSecuredRecords
        excludingSecuredRecords['VSIGN:HR'].count == 5

        when: 'we calculate the same aggregates with user that have access right to the protected study'
        def includingSecuredRecords = multiDimService
                .numericalValueAggregatesPerConcept(crossStudyHeartRate, userWithAccessToMoreData)
        then: 'the protected study numerical observations are taken to account'
        includingSecuredRecords.size() == 1
        'VSIGN:HR' in includingSecuredRecords
        includingSecuredRecords['VSIGN:HR'].count == 7
    }

    void 'test categorical value aggregates'() {
        def user = User.findByUsername('test-public-user-1')
        def userWithAccessToMoreData = User.findByUsername('test-public-user-2')

        when:
        def race = new ConceptConstraint(path: '\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\')
        def result = multiDimService.categoricalValueAggregatesPerConcept(race, user)
        then: 'expected categorical values counts have been returned'
        result.size() == 1
        'CV:DEM:RACE' in result
        result['CV:DEM:RACE'].valueCounts == [ Caucasian: 2, Latino: 1 ]
        result['CV:DEM:RACE'].nullValueCounts == null

        when: 'categorical aggregates run on numerical measures'
        def heartRate = new ConceptConstraint(path: '\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\')
        def emptyResult = multiDimService.categoricalValueAggregatesPerConcept(heartRate, user)
        then: 'no categorical aggregates returned'
        emptyResult.isEmpty()

        when: 'aggregate runs for dataset with one value and one missing value'
        def gender = new ConceptConstraint(path: '\\Demographics\\Gender\\')
        def withMissingValueResult = multiDimService.categoricalValueAggregatesPerConcept(gender, user)
        then: 'answer contains count for the value and count for null value'
        withMissingValueResult.size() == 1
        'gender' in withMissingValueResult
        withMissingValueResult['gender'].valueCounts.size() == 1
        withMissingValueResult['gender'].valueCounts['Male'] == 1
        withMissingValueResult['gender'].nullValueCounts == 1

        when: 'categorical aggregates runs on crosstudy concept with user that have limite access'
        def placeOfBirth = new ConceptConstraint(path: '\\Demographics\\Place of birth\\')
        def excludingSecuredRecords = multiDimService.categoricalValueAggregatesPerConcept(placeOfBirth, user)
        then: 'answer excludes not visible observations for the user'
        excludingSecuredRecords.size() == 1
        'DEMO:POB' in excludingSecuredRecords
        excludingSecuredRecords['DEMO:POB'].valueCounts['Place1'] == 1
        excludingSecuredRecords['DEMO:POB'].valueCounts['Place2'] == 3
        excludingSecuredRecords['DEMO:POB'].nullValueCounts == null

        when: 'now by user who has access to the private study'
        def includingSecuredRecords = multiDimService.categoricalValueAggregatesPerConcept(placeOfBirth,
                userWithAccessToMoreData)
        then: 'answer includes observations from the private study'
        includingSecuredRecords.size() == 1
        'DEMO:POB' in includingSecuredRecords
        includingSecuredRecords['DEMO:POB'].valueCounts['Place1'] == 2
        includingSecuredRecords['DEMO:POB'].valueCounts['Place2'] == 4
        includingSecuredRecords['DEMO:POB'].nullValueCounts == null
    }

    void 'test missing values aggregates'() {
        def user = User.findByUsername('test-public-user-1')

        when: 'querying for null values for concept gender'
        Constraint constraint = new AndConstraint(
                args: [
                        new ConceptConstraint(conceptCode: 'gender'),
                        new ValueConstraint(Type.STRING, Operator.EQUALS, null)
                ])
        def result = multiDimService.counts(constraint, user)

        then: 'one observation is found'
        result.patientCount == 1
        result.observationCount == 1
    }

    void "test time values constraint"() {
        def user = User.findByUsername('test-public-user-1')
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')

        Constraint constraint = new AndConstraint(
                args: [
                        new ConceptConstraint(path: '\\Demographics\\Birth Date\\'),
                        new TimeConstraint(
                            field: new Field(
                                    dimension: VALUE,
                                    fieldName: 'numberValue',
                                    type: 'DATE'
                            ),
                            values: [sdf.parse('1986-10-21 00:00:00'), sdf.parse('1986-10-23 00:00:00')],
                            operator: Operator.BETWEEN
                        )
                ]
        )

        when:
        def result = multiDimService.retrieveClinicalData(constraint, user).asList()

        then:
        result.size() == 1
    }

}
