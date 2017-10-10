/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.converters.JSON
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.multidimquery.AggregateType
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.user.User
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.transmartproject.db.multidimquery.DimensionImpl.*

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


    void 'HD data selected based on sample type (modifier)'(){
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
                path:'\\Public Studies\\TUMOR_NORMAL_SAMPLES\\HD\\Breast\\'
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

    void 'Test for empty set of assayIds'(){
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
        def expectedResult = results.collect { it[VISIT] }.findAll{it} as Set

        when:"I query for all visits for a constraint"
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

    void "test generic query"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        QueryResult patientSetQueryResult = multiDimService.createObservationSetQueryResult(
                "Test generic query without patient set.",
                user,
                (constraint as JSON).toString(),
                'v2')

        then:
        patientSetQueryResult.id > 0
        patientSetQueryResult.queryResultType.id == QueryResultType.GENERIC_QUERY_RESULT_ID
        patientSetQueryResult.setSize == -1L
        patientSetQueryResult.status == QueryStatus.FINISHED
        patientSetQueryResult.username == user.name
        !patientSetQueryResult.description.empty
        patientSetQueryResult.errorMessage == null
        def patientSetEntries = QtPatientSetCollection
                .findAllByResultInstance(QtQueryResultInstance.load(patientSetQueryResult.id))
        !patientSetEntries
    }

    void "test find query results per type"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        def qr1 = multiDimService.createPatientSetQueryResult('Patient set query result', constraint,
                user, (constraint as JSON).toString(), 'v2')
        def qr2 = multiDimService.createObservationSetQueryResult('Generic query result',
                user, (constraint as JSON).toString(), 'v2')

        when:
        def patientSetQueryResults = multiDimService.findPatientSetQueryResults(user).toList()
        then:
        qr1 in patientSetQueryResults
        !(qr2 in patientSetQueryResults)

        when:
        def queryResults = multiDimService.findObservationSetQueryResults(user).toList()
        then:
        !(qr1 in queryResults)
        qr2 in queryResults
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
        def observationCount = multiDimService.count(new TrueConstraint(), user)

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

    void 'test aggregates'() {
        def expectedMax = 102
        def expectedMin = 56
        def expectedAverage = 74.78
        def expectedCount = 9
        def expectedPatientCount = 3
        def user = User.findByUsername('test-public-user-1')
        def heartRate = new ConceptConstraint(
                path: '\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\')

        when:
        def maxMap = multiDimService.aggregate([AggregateType.MAX], heartRate, user)
        then:
        maxMap == [ max: expectedMax ]

        when:
        def minMap = multiDimService.aggregate([AggregateType.MIN], heartRate, user)
        then:
        minMap == [ min: expectedMin ]

        when:
        def avgMap = multiDimService.aggregate([AggregateType.AVERAGE], heartRate, user)
        then:
        avgMap.size() == 1
        'average' in avgMap
        avgMap.average.round(2) == expectedAverage

        //FIXME
        /*when:
        def countMap = multiDimService.aggregate([AggregateType.COUNT], heartRate, user)
        then:
        countMap == [ count: expectedCount ]*/

        when:
        def patientCountMap = multiDimService.aggregate([AggregateType.PATIENT_COUNT], heartRate, user)
        then:
        patientCountMap == [ patient_count: expectedPatientCount ]

        when:
        def compositeMap = multiDimService.aggregate([AggregateType.MAX, AggregateType.MIN, AggregateType.AVERAGE,
                                                      AggregateType.COUNT, AggregateType.PATIENT_COUNT], heartRate, user)
        then:
        compositeMap.size() == 5
        compositeMap.max == expectedMax
        compositeMap.min == expectedMin
        compositeMap.average.round(2) == expectedAverage
        compositeMap.count == expectedCount
        compositeMap.patient_count == expectedPatientCount
    }

    void 'test categorical value frequencies'() {
        def user = User.findByUsername('test-public-user-1')
        def race = new ConceptConstraint(
                path: '\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\')
        when:
        def categoricalValueFreqMap = multiDimService.categoricalValueFrequencies(race, user)
        then:
        categoricalValueFreqMap == [ Caucasian: 2L, Latino: 1L ]

        when: 'only numerical observations selected'
        def emptyMap = multiDimService.categoricalValueFrequencies(new ConceptConstraint(
                path: '\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\'), user)
        then: 'map is empty'
        emptyMap.isEmpty()
    }

}
