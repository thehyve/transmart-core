/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.BiomarkerConstraint
import org.transmartproject.core.multidimquery.query.Combination
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.core.multidimquery.query.ModifierConstraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.OrConstraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.SubSelectionConstraint
import org.transmartproject.core.multidimquery.query.TimeConstraint
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.multidimquery.query.ValueConstraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.clinical.PatientSetService
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.user.User
import spock.lang.Specification

import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.multidimquery.DimensionImpl.*
import static spock.util.matcher.HamcrestSupport.that

@Rollback
@Integration
class QueryServicePgSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    AggregateDataResource aggregateDataResource

    @Autowired
    PatientSetService patientSetResource

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat('yyyy-MM-dd hh:mm:ss')
    private static final DateFormat UTC_DATE_FORMAT
    static {
        UTC_DATE_FORMAT = new SimpleDateFormat('yyyy-MM-dd hh:mm:ss')
        UTC_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone('UTC'))
    }

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
        Constraint combinationConstraint = new AndConstraint([conceptConstraint, assayConstraint])

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
                        dimension: TRIAL_VISIT.name,
                        fieldName: 'relTimeLabel',
                        type: 'STRING'
                ),
                operator: Operator.EQUALS
        )
        def combination

        when:
        trialVisitConstraint.value = 'Baseline'
        combination = new AndConstraint([conceptConstraint, trialVisitConstraint])
        Hypercube hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:
        hypercube.dimensionElements(BIOMARKER).size() == 3
        hypercube.dimensionElements(ASSAY).size() == 4
        hypercube.dimensionElements(PROJECTION).size() == 10

        when:
        trialVisitConstraint.value = 'Week 1'
        combination = new Combination(Operator.AND, [conceptConstraint, trialVisitConstraint])
        hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:

        hypercube.dimensionElements(ASSAY).size() == 1
        def patient = hypercube.dimensionElement(PATIENT, 0)
        patient.subjectIds.get('SUBJ_ID') == 'CTHD:601'
        patient.age == 26

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get hd data for selected time constraint'() {
        def user = User.findByUsername('test-public-user-2')
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def startDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: START_TIME.name,
                        fieldName: 'startDate',
                        type: 'DATE'
                ),
                values: [DATE_FORMAT.parse('2016-03-29 10:30:30')],
                operator: Operator.AFTER
        )
        def startDateTimeInclusiveConstraint = new TimeConstraint(
                field: new Field(
                        dimension: START_TIME.name,
                        fieldName: 'startDate',
                        type: 'DATE'
                ),
                values: [DATE_FORMAT.parse('2016-03-29 10:30:30')],
                operator: Operator.GREATER_THAN_OR_EQUALS
        )

        def endDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: END_TIME.name,
                        fieldName: 'endDate',
                        type: 'DATE'
                ),
                values: [DATE_FORMAT.parse('2016-04-02 00:00:00')],
                operator: Operator.BEFORE
        )
        def endDateTimeInclusiveConstraint = new TimeConstraint(
                field: new Field(
                        dimension: END_TIME.name,
                        fieldName: 'endDate',
                        type: 'DATE'
                ),
                values: [DATE_FORMAT.parse('2016-04-02 00:00:00')],
                operator: Operator.LESS_THAN_OR_EQUALS
        )

        def combination
        Hypercube hypercube
        when:
        combination = new Combination(Operator.AND, [conceptConstraint, startDateTimeConstraint])
        hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:
        hypercube.dimensionElements(ASSAY).size() == 3

        when:
        combination = new Combination(Operator.AND, [conceptConstraint, endDateTimeConstraint])
        hypercube = multiDimService.highDimension(combination, user, 'autodetect')

        then:
        hypercube.toList().empty

        when:
        combination = new Combination(Operator.AND, [conceptConstraint, startDateTimeInclusiveConstraint])
        hypercube = multiDimService.highDimension(combination, user, 'autodetect')
        hypercube.toList()

        then:
        hypercube.dimensionElements(ASSAY).size() == 4

        when:
        combination = new Combination(Operator.AND, [conceptConstraint, endDateTimeInclusiveConstraint])
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
        def minDate = DATE_FORMAT.parse('2016-04-01 10:00:00')
        def visitStartConstraint = new FieldConstraint(
                operator: Operator.AFTER,
                value: minDate,
                field: new Field(
                        dimension: VISIT.name,
                        fieldName: 'startDate',
                        type: 'DATE'
                )
        )
        def studyNameConstraint = new StudyNameConstraint(
                studyId: 'EHR'
        )
        def combination = new Combination(Operator.AND, [visitStartConstraint, studyNameConstraint])
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
        def timeDimensionConstraint = new FieldConstraint(
                operator: Operator.AFTER,
                value: DATE_FORMAT.parse('2016-05-05 10:00:00'),
                field: new Field(
                        dimension: VISIT.name,
                        fieldName: 'endDate',
                        type: 'DATE'
                )

        )
        def conceptConstraint = new ConceptConstraint(
                path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\'
        )
        def combination = new Combination(
                Operator.AND,
                [timeDimensionConstraint, conceptConstraint]
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
                Operator.AND,
                [modifierConstraint, conceptConstraint]
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
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def endDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: END_TIME.name,
                        fieldName: 'endDate',
                        type: 'DATE'
                ),
                values: [DATE_FORMAT.parse('2016-04-02 01:00:00')],
                operator: Operator.AFTER //only exist one before, none after
        )
        when:
        Combination combination = new Combination(Operator.AND, [conceptConstraint, endDateTimeConstraint])
        Hypercube hypercube = multiDimService.highDimension(combination, user, 'autodetect')


        then:
        hypercube.toList().empty

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get data for the patient with given id'() {
        def user = User.findByUsername('test-public-user-1')
        Constraint subjectIdConstraint = new PatientSetConstraint(subjectIds: ['2'])


        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(subjectIdConstraint, user)
        then:
        hypercube.dimensionElements(PATIENT).size() == 1
        hypercube.dimensionElements(PATIENT).first().subjectIds.get('SUBJ_ID') == '2'

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
                Operator.AND,
                [
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
                dimension: VISIT.name,
                constraint: new AndConstraint([
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
        QueryResult patientSetQueryResult = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

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
        patientSetEntries*.patient.id != null
    }

    void "reuse patient set query if one with similar constraints already exists for the user"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        QueryResult patientSetQueryResult1 = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

        when:
        QueryResult patientSetQueryResult2 = patientSetResource.createPatientSetQueryResult("Test set 2",
                constraint,
                user,
                'v2',
                true)

        then:
        patientSetQueryResult1
        patientSetQueryResult1 == patientSetQueryResult2
    }

    void "test a new patient set created even there is possibility to reuse"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        QueryResult patientSetQueryResult1 = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

        when:
        QueryResult patientSetQueryResult2 = patientSetResource.createPatientSetQueryResult("Test set 2",
                constraint,
                user,
                'v2',
                false)

        then:
        patientSetQueryResult1
        patientSetQueryResult1 != patientSetQueryResult2
    }

    void "test reusing patient set query after the patient set ids cache cleanup"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        QueryResult patientSetQueryResult1 = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

        when:
        patientSetResource.clearPatientSetIdsCache()
        QueryResult patientSetQueryResult2 = patientSetResource.createPatientSetQueryResult("Test set 2",
                constraint,
                user,
                'v2',
                true)

        then:
        patientSetQueryResult1
        patientSetQueryResult1 != patientSetQueryResult2
    }

    void "test large text values (raw data type)"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(conceptCode: 'favouritebook')

        when:
        Counts counts = aggregateDataResource.counts(constraint, user)
        Hypercube data = multiDimService.retrieveClinicalData(constraint, user)
        def values = data.collect { HypercubeValue value -> value.value as String }

        then:
        counts.observationCount == 2
        that values, hasSize(2)
        that values, hasItems(containsString('The Brothers Karamazov'), containsString('funny dialogues'))
    }

    void "test searching for large text values (raw data type)"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new AndConstraint([
                new ConceptConstraint(conceptCode: 'favouritebook'),
                new ValueConstraint(Type.TEXT, Operator.CONTAINS, 'Karamazov')
        ])

        when:
        Counts counts = aggregateDataResource.counts(constraint, user)
        Hypercube data = multiDimService.retrieveClinicalData(constraint, user)
        def values = data.collect { HypercubeValue value -> value.value as String }

        then:
        counts.observationCount == 1
        that values, hasSize(1)
        that values, hasItems(containsString('The Brothers Karamazov'))
    }

    void "test time values constraint"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new AndConstraint(
                [
                        new ConceptConstraint(path: '\\Demographics\\Birth Date\\'),
                        new TimeConstraint(
                            field: new Field(
                                    dimension: VALUE.name,
                                    //TODO Has to fail after TMT-420
                                    fieldName: 'numberValue',
                                    type: Type.DATE
                            ),
                            values: [DATE_FORMAT.parse('1986-10-21 00:00:00'), DATE_FORMAT.parse('1986-10-23 00:00:00')],
                            operator: Operator.BETWEEN
                        )
                ]
        )

        when:
        def values = multiDimService.retrieveClinicalData(constraint, user).asList()*.value

        then:
        values.size() == 1
        values[0] instanceof Date
        values[0] == UTC_DATE_FORMAT.parse('1986-10-22 00:00:00')
    }

    void "test values of date type"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new ValueConstraint(valueType: Type.DATE, operator: Operator.AFTER,
                value: DATE_FORMAT.parse('1986-10-21 00:00:00'))

        when:
        def values = multiDimService.retrieveClinicalData(constraint, user).asList()*.value

        then:
        values.size() == 2
        values[0] instanceof Date
        values[0] == UTC_DATE_FORMAT.parse('1986-10-22 00:00:00')
        values[1] instanceof Date
        values[1] == UTC_DATE_FORMAT.parse('2001-09-01 05:30:05')
    }

    void 'test multiple subselect constraints'() {
        given: 'a query with two subselect subqueries'
        def user = User.findByUsername('test-public-user-1')

        Constraint subConstraint1 = new AndConstraint([
                new StudyNameConstraint('SURVEY1'),
                new ConceptConstraint('favouritebook')
        ])

        Constraint subselectConstraint1 = new SubSelectionConstraint('patient', subConstraint1)

        Constraint subConstraint2 = new ConceptConstraint('twin')

        Constraint subselectConstraint2 = new SubSelectionConstraint('patient', subConstraint2)

        Constraint multipleSubselectConstraint = new OrConstraint([
                subselectConstraint1,
                subselectConstraint2
        ])

        when: 'executing the query and the subqueries of which it is are composed'
        def subselectResult1 = multiDimService.retrieveClinicalData(subselectConstraint1, user).asList()
        def subselectResult2 = multiDimService.retrieveClinicalData(subselectConstraint2, user).asList()
        def multipleSubselectResult = multiDimService.retrieveClinicalData(multipleSubselectConstraint, user).asList()

        then: 'the combined subselect result match the results of the separate subselect queries'
        subselectResult1.size() == 17
        subselectResult2.size() == 15
        // in this case the selected patient sets (and, hence, the observation sets)
        // happen to be disjoint, so the result should equal to the sum of the separate queries
        multipleSubselectResult.size() == subselectResult1.size() + subselectResult2.size()
    }

    void 'test numerical constraints'() {
        given: 'a constraint for temperature readings'
        def user = User.findByUsername('test-public-user-2')

        Constraint temperature = new ConceptConstraint('VSIGN:TEMP')

        when: 'retrieving aggregates for the concept'
        def aggregates = aggregateDataResource.numericalValueAggregatesPerConcept(temperature, user)

        then: 'the aggregates match expected values'
        aggregates['VSIGN:TEMP'].count == 7
        aggregates['VSIGN:TEMP'].min == 55
        aggregates['VSIGN:TEMP'].max == 89

        when: 'restricting to values < 82'
        Constraint lessThan = new AndConstraint([temperature, new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN, 82)])
        def aggregates2 = aggregateDataResource.numericalValueAggregatesPerConcept(lessThan, user)

        then: 'the aggregates differ from the previous accordingly'
        aggregates2['VSIGN:TEMP'].count == 4
        aggregates2['VSIGN:TEMP'].min == 55
        aggregates2['VSIGN:TEMP'].max == 81

        when: 'restricting to values <= 82'
        Constraint lessThanOrEquals = new AndConstraint([temperature, new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN_OR_EQUALS, 82)])
        def aggregates3 = aggregateDataResource.numericalValueAggregatesPerConcept(lessThanOrEquals, user)

        then: 'the aggregates differ from the previous accordingly'
        aggregates3['VSIGN:TEMP'].count == 6
        aggregates3['VSIGN:TEMP'].min == 55
        aggregates3['VSIGN:TEMP'].max == 82
    }

}
