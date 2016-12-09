package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.db.metadata.DimensionDescription
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
import org.transmartproject.db.multidimquery.query.TimeConstraint
import org.transmartproject.db.multidimquery.query.Type
import org.transmartproject.db.multidimquery.query.ValueConstraint
import org.transmartproject.db.user.User
import spock.lang.Specification
import java.text.SimpleDateFormat

@Rollback
@Integration
class QueryServiceSpec extends Specification {

    @Autowired
    QueryService queryService

    Dimension assayDim = DimensionDescription.dimensionsMap.assay
    Dimension biomarkerDim = DimensionDescription.dimensionsMap.biomarker
    Dimension projectionDim = DimensionDescription.dimensionsMap.projection
    Dimension patientDim = DimensionDescription.dimensionsMap.patient
    Dimension visitDim = DimensionDescription.dimensionsMap.visit

    void 'get whole hd data for single node'() {
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        Hypercube hypercube = queryService.highDimension(conceptConstraint, user)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 3
        hypercube.dimensionElements(assayDim).size() == 6
        hypercube.dimensionElements(projectionDim).size() == 10
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
        Hypercube hypercube = queryService.highDimension(combinationConstraint, user)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 3
        hypercube.dimensionElements(assayDim).size() == 2
        hypercube.dimensionElements(projectionDim).size() == 10
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
        Hypercube hypercube = queryService.highDimension(conceptConstraint, bioMarkerConstraint, user)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 2
        hypercube.dimensionElements(assayDim).size() == 6
        hypercube.dimensionElements(projectionDim).size() == 10
    }

    void 'get hd data for selected trial visit dimension'() {
        def user = User.findByUsername('test-public-user-1')
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def trialVisitConstraint = new FieldConstraint(
                field: new Field(
                        dimension: TrialVisitDimension,
                        fieldName: 'relTimeLabel',
                        type: 'STRING'
                ),
                operator: Operator.EQUALS
        )
        def combination

        when:
        trialVisitConstraint.value = 'Baseline'
        combination = new Combination(operator: Operator.AND, args: [conceptConstraint, trialVisitConstraint])
        Hypercube hypercube = queryService.highDimension(combination, user)
        hypercube.toList()

        then:
        hypercube.dimensionElements(biomarkerDim).size() == 3
        hypercube.dimensionElements(assayDim).size() == 4
        hypercube.dimensionElements(projectionDim).size() == 10

        when:
        trialVisitConstraint.value = 'Week 1'
        combination = new Combination(operator: Operator.AND, args: [conceptConstraint, trialVisitConstraint])
        hypercube = queryService.highDimension(combination, user)
        hypercube.toList()

        then:

        hypercube.dimensionElements(assayDim).size() == 1
        def patient = hypercube.dimensionElement(patientDim, 0)
        patient.id == -601
        patient.age == 26


    }
    void 'get hd data for selected time constraint'() {
        def user = User.findByUsername('test-public-user-2')
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def startDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: StartTimeDimension,
                        fieldName: 'startDate',
                        type: 'DATE'
                ),
                values: [sdf.parse('2016-03-29 10:30:30')],
                operator: Operator.AFTER
        )

        def endDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: EndTimeDimension,
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
        hypercube = queryService.highDimension(combination, user)
        hypercube.toList()

        then:
        hypercube.dimensionElements(assayDim).size() == 3

        when:
        combination = new Combination(operator: Operator.AND, args: [conceptConstraint, endDateTimeConstraint])
        hypercube = queryService.highDimension(combination, user)
        hypercube.toList()

        then:
        hypercube.dimensionElements(assayDim).size() == 1

    }

    void 'Clinical data selected on visit dimension'() {
        def user = User.findByUsername('test-public-user-1')
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def minDate = sdf.parse('2016-04-01 10:00:00')
        def visitStartConstraint = new FieldConstraint(
                operator: Operator.AFTER,
                value: minDate,
                field: new Field(
                        dimension: VisitDimension,
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
        Hypercube hypercube = queryService.retrieveClinicalData(combination, user)
        def observations = hypercube.toList()

        then:
        observations.size() == 2
        hypercube.dimensionElements(visitDim).each {
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
                        dimension: VisitDimension,
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
        Hypercube hypercube = queryService.highDimension(combination, user)
        hypercube.toList()

        then:
        hypercube.dimensionElements(assayDim).size() == 1
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
        Hypercube hypercube = queryService.retrieveClinicalData(combination, user)
        hypercube.toList()

        then:
        hypercube.dimensionElements(patientDim).size() == 2
    }

    void 'Test for empty set of assayIds'(){
        def user = User.findByUsername('test-public-user-1')
        def sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\EHR_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def endDateTimeConstraint = new TimeConstraint(
                field: new Field(
                        dimension: EndTimeDimension,
                        fieldName: 'endDate',
                        type: 'DATE'
                ),
                values: [sdf.parse('2016-04-02 01:00:00')],
                operator: Operator.AFTER //only exist one before, none after
        )
        when:
        Combination combination = new Combination(operator: Operator.AND, args: [conceptConstraint, endDateTimeConstraint])
        Hypercube hypercube = queryService.highDimension(combination, user)


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
        Hypercube hypercube = queryService.highDimension(user, combinationConstraint, bioMarkerConstraint)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 1
        hypercube.dimensionElements(assayDim).size() == 1
        hypercube.dimensionElements(projectionDim).size() == 13
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
        Hypercube hypercube = queryService.highDimension(user, conceptConstraint, bioMarkerConstraint)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 1
        hypercube.dimensionElements(assayDim).size() == 3
        hypercube.dimensionElements(projectionDim).size() == 13
    }
}
