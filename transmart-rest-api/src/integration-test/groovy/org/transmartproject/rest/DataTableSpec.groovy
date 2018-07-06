/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.PaginationParameters
import org.transmartproject.core.dataquery.SortSpecification
import org.transmartproject.core.dataquery.TableConfig
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.users.User
import org.transmartproject.db.TestData
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.user.AccessLevelTestData
import org.transmartproject.rest.serialization.DataTableSerializer
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
@Slf4j
class DataTableSpec extends Specification {

    @Autowired
    MultidimensionalDataResourceService queryResource


    TestData testData
    ClinicalTestData clinicalData
    AccessLevelTestData accessLevelTestData
    User adminUser


    void setupData() {
        TestData.clearAllDataInTransaction()

        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        accessLevelTestData = new AccessLevelTestData()
        accessLevelTestData.saveAuthorities()
        adminUser = accessLevelTestData.users[0]
    }

    void testDataTableSerialization() {
        setupData()
        def dataType = 'clinical'
        def rowDimensions = ['patient', 'study']
        def columnDimensions = ['concept', 'trial visit']
        def limit = 10
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def tableConfig = new TableConfig(
                rowSort: [new SortSpecification(dimension: 'patient')],
                rowDimensions: rowDimensions,
                columnDimensions: columnDimensions
        )
        def pagination = new PaginationParameters(limit: limit)
        def dataTable = queryResource.retrieveDataTablePage(tableConfig, pagination, dataType, constraint, adminUser)

        when:
        def out = new ByteArrayOutputStream()
        DataTableSerializer.write(dataTable, out)
        out.flush()
        def result = new JsonSlurper().parse(out.toByteArray())
        def rows = result.rows
        def offset = result.offset
        def sorting = result.sort
        def columnHeaders = result.columnHeaders
        def columnDim = result.columnDimensions
        def rowDim = result.rowDimensions

        then:
        rows.size() < clinicalData.longitudinalClinicalFacts.size()
        rows.size() <= limit
        offset == 0

        sorting.size() == 4
        sorting[0] == [dimension: 'patient', sortOrder: 'asc', userRequested: true]
        sorting[1] == [dimension: 'study', sortOrder: 'asc']
        sorting[2] == [dimension: 'concept', sortOrder: 'asc']
        sorting[3] == [dimension: 'trial visit', sortOrder: 'asc']

        columnHeaders*.dimension == columnDimensions
        columnHeaders[0].keys == ['c5', 'c5', 'c5', 'c6', 'c6', 'c6']
        // [4, 5, 6, 4, 5, 6] or something similar, but the id values can change between runs
        columnHeaders[1].keys == clinicalData.longitudinalClinicalFacts*.trialVisit*.id.unique().sort() * 2

        columnDim*.name == columnDimensions
        columnDim[0].elements.size() == (columnHeaders[0].keys as Set).size()
        columnDim[1].elements.size() == (columnHeaders[1].keys as Set).size()

        rowDim*.name == rowDimensions
        rowDim[0].elements.size() == 3
        rowDim[1].elements.size() == 1

        that rows*.rowHeaders, everyItem(hasSize(2))
        that rows*.rowHeaders*.dimension, everyItem(contains('patient', 'study'))
        (rows*.rowHeaders.collect{it[1].key} as Set).size() == rowDim[1].elements.size()
        that rows*.cells, everyItem(hasSize(6))
        (rows*.rowHeaders.collect{it[0].key} as Set).size() == rowDim[0].elements.size()
    }

}
