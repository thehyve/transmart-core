package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.dataquery.SortSpecification
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.db.clinical.SurveyTableColumnService
import org.transmartproject.db.user.User
import spock.lang.Specification

import static org.hamcrest.CoreMatchers.containsString
import static org.transmartproject.core.ontology.Measure.NOMINAL
import static org.transmartproject.core.ontology.Measure.SCALE
import static org.transmartproject.core.ontology.VariableDataType.*
import static spock.util.matcher.HamcrestSupport.that

@Rollback
@Integration
class SurveyTableViewSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    MDStudiesResource studiesResource

    @Autowired
    ConceptsResource conceptsResource

    @Autowired
    SurveyTableColumnService surveyTableColumnService

    @Autowired
    PatientSetResource patientSetResource


    private final UTC = TimeZone.getTimeZone('UTC')

    def 'survey 1'() {
        setup:
        def user = User.findByUsername('test-public-user-1')
        Constraint constraint = new StudyNameConstraint(studyId: "SURVEY1")
        def args = new DataRetrievalParameters(constraint: constraint, sort: [new SortSpecification(dimension: 'patient')])
        Hypercube hypercube = multiDimService.retrieveClinicalData(args, user)
        boolean includeMeasurementDateColumns = true

        when:
        List<HypercubeDataColumn> hypercubeColumns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(constraint, user)
        List<MetadataAwareDataColumn> columnList = surveyTableColumnService.getMetadataAwareColumns(hypercubeColumns, includeMeasurementDateColumns)
        def transformedView = new SurveyTableView(columnList, hypercube)
        then: 'header matches expectations'
        def columns = transformedView.indicesList
        columns*.label == ['FISNumber', 'birthdate1', 'birthdate1.date', 'favouritebook', 'favouritebook.date',
                           'gender1', 'gender1.date', 'nmultbab', 'nmultbab.date', 'nmultfam', 'nmultfam.date',
                           'sport', 'sport.date', 'twin', 'twin.date']
        then: 'columns metadata matches expectations'
        def metadata = columns*.metadata
        metadata*.type == [NUMERIC, DATE, DATE, STRING, DATE, NUMERIC, DATE, STRING, DATE, STRING, DATE, STRING, DATE, STRING, DATE]
        metadata*.measure == [SCALE, SCALE, SCALE, NOMINAL, SCALE, NOMINAL, SCALE, NOMINAL, SCALE, NOMINAL, SCALE, NOMINAL, SCALE, NOMINAL, SCALE]
        metadata*.description == ['FIS Number', 'Birth Date', 'Date of measurement', 'Favourite Book',
                                  'Date of measurement', 'Gender', 'Date of measurement', 'Number of children that are multiplet',
                                  'Date of measurement', 'Numbers of multiples in family', 'Date of measurement',
                                  'How often do you do sport activities?', 'Date of measurement', 'Is a Twin', 'Date of measurement']
        metadata*.width == [12, 22, 22, 400, 22, 12, 22, 25, 22, 25, 22, null, 22, 25, 22]
        metadata*.decimals == [0, null, null, null, null, null, null, null, null, null, null, null, null, null, null]
        metadata*.columns == [12, 22, 22, 400, 22, 14, 22, 25, 22, 25, 22, null, 22, 25, 22]
        def height1Metadata = columns.find { it.label == 'gender1' }.metadata
        height1Metadata.valueLabels == [(new BigDecimal(1)): 'Female', (new BigDecimal(2)): 'Male', (new BigDecimal(-2)): 'Not Specified']
        height1Metadata.missingValues.values == [new BigDecimal(-2)]

        when: 'get row'
        def rows = transformedView.rows.toList()
        then: 'content matches expectations'
        rows.size() == 14
        def secondSubjRow = rows.find { row ->  row[columns[0]] == '2' }
        secondSubjRow
        secondSubjRow[columns[0]] == '2'
        secondSubjRow[columns[1]] == Date.parse('yyyy-MM-dd hh:mm:ss', '1986-10-22 00:00:00', UTC)
        secondSubjRow[columns[2]] == Date.parse('yyyy-MM-dd hh:mm:ss', '2010-12-16 20:23:15')
        that secondSubjRow[columns[3]] as String, containsString('Dostoyevsky')
        secondSubjRow[columns[4]] == Date.parse('yyyy-MM-dd hh:mm:ss', '2016-03-21 10:36:01')
        secondSubjRow[columns[5]] == -2
        secondSubjRow[columns[6]] == null
        secondSubjRow[columns[11]] == '3'
        def firstSubjRow = rows.find { row ->  row[columns[0]] == '1' }
        firstSubjRow[columns[0]] == '1'
        firstSubjRow[columns[1]] == Date.parse('yyyy-MM-dd hh:mm:ss', '1980-08-12 00:00:00', UTC)
        firstSubjRow[columns[2]] == Date.parse('yyyy-MM-dd hh:mm:ss', '2015-11-14 19:05:00')
        that firstSubjRow[columns[3]] as String, containsString('Hofstadter')
        firstSubjRow[columns[4]] == Date.parse('yyyy-MM-dd hh:mm:ss', '2016-03-21 10:36:01')
        firstSubjRow[columns[5]] == 2
        firstSubjRow[columns[6]] == null
        firstSubjRow[columns[11]] == 10

        when: 'do not include MeasurementDateColumn'
        includeMeasurementDateColumns = false
        if(transformedView) transformedView.close()
        columnList = surveyTableColumnService.getMetadataAwareColumns(hypercubeColumns, includeMeasurementDateColumns)
        transformedView = new SurveyTableView(columnList, hypercube)
        then: 'header matches expectations'
        def columns2 = transformedView.indicesList
        columns2*.label == ['FISNumber', 'birthdate1', 'favouritebook', 'gender1', 'nmultbab', 'nmultfam', 'sport', 'twin']
        then: 'columns metadata matches expectations'
        def metadata2 = columns2*.metadata
        metadata2*.type == [NUMERIC, DATE, STRING, NUMERIC, STRING, STRING, STRING, STRING]
        metadata2*.measure == [SCALE, SCALE, NOMINAL, NOMINAL, NOMINAL, NOMINAL, NOMINAL, NOMINAL]
        metadata2*.description == ['FIS Number', 'Birth Date', 'Favourite Book', 'Gender', 'Number of children that are multiplet',
                                  'Numbers of multiples in family', 'How often do you do sport activities?', 'Is a Twin']

        cleanup:
        if(transformedView) transformedView.close()
    }

    def 'survey 2'() {
        setup:
        def user = User.findByUsername('test-public-user-1')
        Constraint constraint = new StudyNameConstraint(studyId: "SURVEY2")
        def args = new DataRetrievalParameters(constraint: constraint, sort: [new SortSpecification(dimension: 'patient')])
        Hypercube hypercube = multiDimService.retrieveClinicalData(args, user)
        boolean includeMeasurementDateColumns = true

        when:
        List<HypercubeDataColumn> hypercubeColumns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(constraint, user)
        List<MetadataAwareDataColumn> columnList = surveyTableColumnService.getMetadataAwareColumns(hypercubeColumns, includeMeasurementDateColumns)
        def transformedView = new SurveyTableView(columnList, hypercube)
        then: 'header matches expectations'
        def columns = transformedView.indicesList
        columns*.label == ['FISNumber', 'description', 'description.date', 'height1', 'height1.date']
        then: 'columns metadata matches expectations'
        def metadata = columns*.metadata
        metadata*.type == [NUMERIC, STRING, DATE, NUMERIC, DATE]
        metadata*.measure == [SCALE, NOMINAL, SCALE, SCALE, SCALE]
        metadata*.description == ['FIS Number', 'Description', 'Date of measurement', 'Height', 'Date of measurement']
        metadata*.width == [12, 200, 22, 14, 22]
        metadata*.decimals == [0, null, null, 2, null]
        metadata*.columns == [12, 210, 22, 15, 22]
        def height1Metadata = columns.find { it.label == 'height1' }.metadata
        height1Metadata.valueLabels == [(new BigDecimal(-1)): 'Asked, but not answered']
        height1Metadata.missingValues.values == [new BigDecimal(-1)]

        when: 'get row'
        def rows = transformedView.rows.toList()

        then: 'content matches expectations'

        def firstSubjRow = rows.find { row ->  row[columns[0]] == '1' }
        firstSubjRow
        firstSubjRow[columns[0]] == '1'
        firstSubjRow[columns[1]] == 'Description about subject 1'
        firstSubjRow[columns[2]] == Date.parse('yyyy-MM-dd hh:mm:ss', '2016-03-21 10:36:01')
        firstSubjRow[columns[3]] == -1
        firstSubjRow[columns[4]] == Date.parse('yyyy-MM-dd hh:mm:ss', '2005-05-24 13:40:00')

        def secondSubjRow = rows.find { row ->  row[columns[0]] == '2' }
        secondSubjRow
        secondSubjRow[columns[0]] == '2'
        secondSubjRow[columns[1]] == 'No description'
        secondSubjRow[columns[2]] == null
        secondSubjRow[columns[3]] <=> 169 == 0
        secondSubjRow[columns[4]] == Date.parse('yyyy-MM-dd hh:mm:ss', '2004-08-27 10:45:32')

        when: 'do not include MeasurementDateColumn'
        includeMeasurementDateColumns = false
        if(transformedView) transformedView.close()
        columnList = surveyTableColumnService.getMetadataAwareColumns(hypercubeColumns, includeMeasurementDateColumns)
        transformedView = new SurveyTableView(columnList, hypercube)
        then: 'header matches expectations'
        def columns2 = transformedView.indicesList
        columns2*.label == ['FISNumber', 'description', 'height1']
        then: 'columns metadata matches expectations'
        def metadata2 = columns2*.metadata
        metadata2*.type == [NUMERIC, STRING, NUMERIC]
        metadata2*.measure == [SCALE, NOMINAL, SCALE]
        metadata2*.description == ['FIS Number', 'Description', 'Height']

        cleanup:
        if (transformedView) transformedView.close()
    }

    def 'number of column for the patient set constraint and study'() {
        def user = User.findByUsername('test-public-user-1')
        def survey1 = new StudyNameConstraint(studyId: 'SURVEY1')
        def survey1PatientSet = patientSetResource.createPatientSetQueryResult("Test set",
                survey1,
                user,
                'v2',
                false)
        List<HypercubeDataColumn> survey1Columns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                survey1, user)

        when: 'we get data columns for the patient set and study'
        List<HypercubeDataColumn> survey1PatientSetColumns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                new AndConstraint([
                        new PatientSetConstraint(patientSetId: survey1PatientSet.id),
                        survey1
                ]), user)

        then: 'we get exactly the same columns as for the survey1 only'
        (survey1PatientSetColumns - survey1Columns).empty
    }

    def 'number of column for the patient set constraint'() {
        def user = User.findByUsername('test-public-user-1')
        def ora1000 = new StudyNameConstraint(studyId: 'ORACLE_1000_PATIENT')
        def ora1000PatientSet = patientSetResource.createPatientSetQueryResult("Test set",
                ora1000,
                user,
                'v2',
                false)
        List<HypercubeDataColumn> ora1000Columns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                ora1000, user)

        when: 'we get data columns for the patient set'
        List<HypercubeDataColumn> survey1PatientSetColumns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                new PatientSetConstraint(patientSetId: ora1000PatientSet.id), user)

        then: 'we get exactly 1000 columns'
        (survey1PatientSetColumns - ora1000Columns).empty
    }

    def 'number of column for study/concept column constraint'() {
        def user = User.findByUsername('test-public-user-1')
        def survey1 = new StudyNameConstraint(studyId: 'SURVEY1')
        def survey1PatientSet = patientSetResource.createPatientSetQueryResult("Test set",
                survey1,
                user,
                'v2',
                false)

        when: 'we get data columns for the patient set and unexisting study/concept'
        List<HypercubeDataColumn> survey1PatientSetColumns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                new AndConstraint([
                        new PatientSetConstraint(patientSetId: survey1PatientSet.id),
                        new AndConstraint([
                                new ConceptConstraint(conceptCodes: ['a', 'b', 'c']),
                                survey1
                        ])
                ]), user)

        then: 'we get nothing as there is no such study/concept'
        survey1PatientSetColumns.empty


        when: 'we get data columns for the patient set and study/concept constraint'
        List<HypercubeDataColumn> survey1PatientSetColumns2 = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                new AndConstraint([
                        new PatientSetConstraint(patientSetId: survey1PatientSet.id),
                        new AndConstraint([
                                new ConceptConstraint(conceptCodes: ['favouritebook']),
                                survey1
                        ])
                ]), user)

        then: 'we get one column'
        survey1PatientSetColumns2.size() == 1
    }

}
