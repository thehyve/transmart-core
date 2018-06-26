package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.PaginationParameters
import org.transmartproject.core.dataquery.TableConfig
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.DataTableColumn
import org.transmartproject.core.multidimquery.FullDataTableRow
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.PagingDataTable
import org.transmartproject.core.multidimquery.StreamingDataTable
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.user.User
import spock.lang.Ignore
import spock.lang.Specification

@Rollback
@Integration
class DataTablePgSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    def 'test nullable dimensions handling'() {
        User user = User.findByUsername('admin')
        StudyNameConstraint studyConstraint = new StudyNameConstraint('EHR')
        def tableConfig = new TableConfig(
                rowDimensions: ['study', 'patient', 'start time', 'end time'],
                columnDimensions: ['visit', 'concept']
        )
        def pagination = new PaginationParameters(offset: 0, limit: 10)

        when: 'fetching data table with nullable dimensions: "visit", "end time", "start time"'
        PagingDataTable table = multiDimService.retrieveDataTablePage(
                tableConfig, pagination, 'clinical', studyConstraint, user)

        then: 'row and column keys and elements contain null values'
        table.rowKeys*.keys.collect{rows -> rows.getAt(0)}.contains(null) == false // study dimension keys
        table.rowKeys*.keys.collect{rows -> rows.getAt(1)}.contains(null) == false // patient dimension keys
        table.rowKeys*.keys.collect{rows -> rows.getAt(2)}.contains(null) == true  // start time dimension keys
        table.rowKeys*.keys.collect{rows -> rows.getAt(3)}.contains(null) == true  // end time dimension keys

        table.rowKeys*.elements.collect{rows -> rows.getAt(0)}.contains(null) == false // study dimension elements
        table.rowKeys*.elements.collect{rows -> rows.getAt(1)}.contains(null) == false // patient dimension elements
        table.rowKeys*.elements.collect{rows -> rows.getAt(2)}.contains(null) == true  // start time dimension elements
        table.rowKeys*.elements.collect{rows -> rows.getAt(3)}.contains(null) == true  // end time dimension elements

        table.columnKeys*.keys.collect{columns -> columns.getAt(0)}.contains(null) == true // visit dimension keys
        table.columnKeys*.keys.collect{columns -> columns.getAt(1)}.contains(null) == false // concept dimension keys

        table.columnKeys*.elements.collect{columns -> columns.getAt(0)}.contains(null) == true // visit dimension elements
        table.columnKeys*.elements.collect{columns -> columns.getAt(1)}.contains(null) == false // concept dimension elements
    }

    // FIXME: only the dimension elements contained in the current page needs to be sent, currently
    // all dimensions elements for the complete query are sent.
    @Ignore
    def 'test data table pagination'() {
        given: 'the admin user, study constraint for 1000 patients'
        User user = User.findByUsername('admin')
        StudyNameConstraint studyConstraint = new StudyNameConstraint('ORACLE_1000_PATIENT')
        def tableConfig = new TableConfig(
                rowDimensions: ['study', 'patient'],
                columnDimensions: ['concept']
        )
        def pagination = new PaginationParameters(limit: 10)

        when: 'fetching data table with patient, study and concept dimensions, limited to 10 rows'
        PagingDataTable page = multiDimService.retrieveDataTablePage(
                tableConfig, pagination, 'clinical', studyConstraint, user)

        then: 'the result should be limited to 10 rows and associated dimension elements'
        page.rowDimensions.collect { it.name } == ['study', 'patient']
        page.columnDimensions.collect { it.name } == ['concept']
        page.rowKeys.size() == 10
        def patientDimension = page.rowDimensions.find { it.name == 'patient'}
        page.hypercube.dimensionElements(patientDimension).size() <= 10
    }

    def 'test data table does not allow modifier dimensions as row dimensions'() {
        given: 'the admin user, study constraint for tumor/normal samples'
        User user = User.findByUsername('admin')
        StudyNameConstraint studyConstraint = new StudyNameConstraint('TUMOR_NORMAL_SAMPLES')
        def tableConfig = new TableConfig(
                rowDimensions: ['patient', 'sample_type'],
                columnDimensions: ['study', 'concept']
        )

        when: 'fetching data table with patient and sample_type as row dimensions'
        multiDimService.retrieveStreamingDataTable(tableConfig, 'clinical', studyConstraint, user)

        then: 'an exception is thrown'
        def e = thrown(InvalidArgumentsException)
        e.message == 'Sorting over these dimensions is not supported: sample_type'
    }

    def 'test data table with modifiers'() {
        given: 'the admin user, study constraint for tumor/normal samples'
        User user = User.findByUsername('admin')
        StudyNameConstraint studyConstraint = new StudyNameConstraint('TUMOR_NORMAL_SAMPLES')
        def tableConfig = new TableConfig(
                rowDimensions: ['patient'],
                columnDimensions: ['concept', 'sample_type']
        )

        when: 'fetching data table with patient, concept and sample_type dimensions'
        StreamingDataTable stream = multiDimService.retrieveStreamingDataTable(
                tableConfig, 'clinical', studyConstraint, user)
        List<FullDataTableRow> rows = []
        for (FullDataTableRow row: stream) {
            rows.add(row)
        }

        then: 'the result should contain patients as rows and concepts as columns'
        stream.rowDimensions.collect { it.name } == ['patient']
        stream.columnDimensions.collect { it.name } == ['concept', 'sample_type']

        and: 'the result should contain a row for each patient'
        rows.size() == 3
        def patientDimension = stream.rowDimensions.find { it.name == 'patient'}
        def patients = stream.hypercube.dimensionElements(patientDimension) as
                List<org.transmartproject.db.i2b2data.PatientDimension>
        patients.size() == 3
        patients.collect { patient -> patient.subjectIds['SUBJ_ID'] } == ['TNS:63', 'TNS:53', 'TNS:43']
        rows.collect { row -> row.rowHeader.elements.subjectIds['SUBJ_ID'] } == [['TNS:63'], ['TNS:53'], ['TNS:43']]

        and: 'the result should contain a column for each concept, sample_type combination'
        stream.columnKeys.collect { columnKey ->
            [concept: columnKey.elements[0].conceptCode, sampleType: columnKey.elements[1]]
        } as Set ==
                [[concept: 'TNS:DEM:AGE', sampleType: null],
                 [concept: 'TNS:LAB:CELLCNT', sampleType: 'Normal'],
                 [concept: 'TNS:LAB:CELLCNT', sampleType: 'Tumor'],
                 [concept: 'TNS:HD:EXPBREAST', sampleType: 'Normal'],
                 [concept: 'TNS:HD:EXPBREAST', sampleType: 'Tumor'],
                 [concept: 'TNS:HD:EXPLUNG', sampleType: 'Normal'],
                 [concept: 'TNS:HD:EXPLUNG', sampleType: 'Tumor']
                ] as Set

        and: 'the result should contain have a value for a patient, for a combination of concept and sample_type'
        DataTableColumn lungNormalColumn = stream.columnKeys.find { it.elements[0].conceptCode == 'TNS:HD:EXPLUNG' && it.elements[1] == 'Normal' }
        def lungNormalValue = rows[0].multimap.get(lungNormalColumn)
        lungNormalValue[0].availableDimensions.collect { it.name } as Set == ['patient', 'concept', 'sample_type'] as Set
        def sampleTypeDimension = lungNormalValue[0].availableDimensions.find { it.name == 'sample_type' }
        sampleTypeDimension != null
        lungNormalValue.size() == 1
        lungNormalValue[0].value == 'sample1'
    }

}
