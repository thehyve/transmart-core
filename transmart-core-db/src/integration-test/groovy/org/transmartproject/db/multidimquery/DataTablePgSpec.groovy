package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.PaginationParameters
import org.transmartproject.core.dataquery.TableConfig
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.PagingDataTable
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.user.User
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
}
