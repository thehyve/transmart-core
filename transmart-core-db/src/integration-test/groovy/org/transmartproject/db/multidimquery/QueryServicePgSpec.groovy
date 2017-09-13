/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.db.multidimquery.query.AndConstraint
import org.transmartproject.db.multidimquery.query.BiomarkerConstraint
import org.transmartproject.db.multidimquery.query.Combination
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.Field
import org.transmartproject.db.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.ModifierConstraint
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.multidimquery.query.SubSelectionConstraint
import org.transmartproject.db.multidimquery.query.TimeConstraint
import org.transmartproject.db.multidimquery.query.Type
import org.transmartproject.db.multidimquery.query.ValueConstraint
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
}
