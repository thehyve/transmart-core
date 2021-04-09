package org.transmartproject.db.multidimquery

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Lists
import com.google.common.collect.Table
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.datatable.PaginationParameters
import org.transmartproject.core.multidimquery.SortOrder
import org.transmartproject.core.multidimquery.SortSpecification
import org.transmartproject.core.multidimquery.datatable.TableConfig
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.FullDataTableRow
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.PagingDataTable
import org.transmartproject.core.multidimquery.StreamingDataTable
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.StudyObjectConstraint
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.users.User
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.user.AccessLevelTestData
import spock.lang.Specification


@Integration
@Rollback
class DataTableSpec extends Specification {

    @Autowired
    SessionFactory sessionFactory

    TestData testData
    ClinicalTestData clinicalData
    AccessLevelTestData accessLevelTestData
    User adminUser

    @Autowired
    MultiDimensionalDataResource queryResource

    void setupData() {
        TestData.prepareCleanDatabase()

        testData = TestData.createHypercubeDefault()
        testData.saveAll()

        clinicalData = testData.clinicalData

        accessLevelTestData = new AccessLevelTestData()
        accessLevelTestData.saveAuthorities()
        adminUser = accessLevelTestData.users[0]

        sessionFactory.currentSession.flush()
    }

    Constraint getConstraint() {
        new AndConstraint([
                new StudyObjectConstraint(study: clinicalData.longitudinalStudy),
                new FieldConstraint(field:
                        new Field(dimension: 'trial visit', type: Type.NUMERIC, fieldName: 'relTime'),
                        operator: Operator.EQUALS, value: 0)
        ])
    }

    void testSimple() {
        setupData()

        when:
        def tableConfig = new TableConfig(
                rowDimensions: ['study', 'patient'],
                columnDimensions: ['trial visit', 'concept']
        )
        def pagination = new PaginationParameters(
                offset: 0,
                limit: 10
        )
        PagingDataTable table = queryResource.retrieveDataTablePage(tableConfig, pagination, 'clinical', constraint, adminUser)

        def sortDims = table.sort.keySet().withIndex().collectEntries()


        then:
        table.rowDimensions == [DimensionImpl.STUDY, DimensionImpl.PATIENT]
        table.columnDimensions == [DimensionImpl.TRIAL_VISIT, DimensionImpl.CONCEPT]
        table.rowKeys*.elements*.getAt(1) as Set == (clinicalData.longitudinalClinicalFacts*.patient as Set)
        table.columnKeys*.elements*.getAt(1)*.conceptCode as Set == clinicalData.longitudinalClinicalFacts*.conceptCode as Set
        table.requestedSort == [:]
        sortDims[DimensionImpl.TRIAL_VISIT] < sortDims[DimensionImpl.CONCEPT]
        sortDims[DimensionImpl.STUDY] < sortDims[DimensionImpl.PATIENT]
        table.columnKeys*.keys*.getAt(0) == table.columnKeys*.keys*.getAt(0).sort(false)
        table.rowKeys*.keys*.getAt(0) == table.rowKeys*.keys*.getAt(0).sort(false)
        table.columnKeys == table.columnKeys.sort(false)

        when:
        def secondRow = table.row(table.rowKeys[1])
        pagination.offset = 1
        pagination.limit = 1
        PagingDataTable subTable = queryResource.retrieveDataTablePage(tableConfig, pagination, 'clinical', constraint, adminUser)

        then:
        subTable.rowKeys.size() == 1
        subTable.rowKeys[0] == table.rowKeys[1]
        subTable.row(subTable.rowKeys[0]).collectEntries { col, vals -> [col, vals[0].value] } ==
                secondRow.collectEntries { col, vals -> [col, vals[0].value] }

        when:
        tableConfig = new TableConfig(
                rowDimensions: ['study', 'patient'],
                columnDimensions: ['trial visit', 'concept']
        )
        StreamingDataTable sTable = queryResource.retrieveStreamingDataTable(tableConfig, 'clinical', constraint, adminUser)
        List<FullDataTableRow> rows = Lists.newArrayList(sTable)

        Table sTab = HashBasedTable.create()
        rows.each { row ->
            row.multimap.entries().each { entry ->
                sTab.put(row.rowHeader, entry.key, entry.value.value)
            }
        }

        Table tab = HashBasedTable.create()
        table.cellSet().each { cell ->
            tab.put(cell.rowKey, cell.columnKey, cell.value[0].value)
        }

        then:
        sTable.requestedSort == [:]
        sTable.columnKeys == table.columnKeys
        rows*.rowHeader == table.rowKeys
        sTab == tab
    }

    void testSort() {
        setupData()

        when:
        def tableConfig = new TableConfig(
                rowDimensions: ['study', 'patient'],
                columnDimensions: ['trial visit', 'concept'],
                rowSort: [SortSpecification.asc('patient'), SortSpecification.asc('study')],
                columnSort: [SortSpecification.asc('concept'), SortSpecification.asc('trial visit')]
        )
        def pagination = new PaginationParameters(limit: 10)
        PagingDataTable reverseSortTable = queryResource.retrieveDataTablePage(tableConfig, pagination, 'clinical', constraint, adminUser)
        def reverseSortDims = reverseSortTable.sort.keySet().withIndex().collectEntries()

        then:
        reverseSortTable.requestedSort == ['patient', 'study', 'concept', 'trial visit'].collectEntries {
            [queryResource.getDimension(it), SortOrder.ASC]
        }
        reverseSortDims[DimensionImpl.TRIAL_VISIT] > reverseSortDims[DimensionImpl.CONCEPT]
        reverseSortDims[DimensionImpl.STUDY] > reverseSortDims[DimensionImpl.PATIENT]
        reverseSortTable.columnKeys*.keys*.getAt(0) == reverseSortTable.columnKeys*.keys*.getAt(0).sort(false)
        reverseSortTable.rowKeys*.keys*.getAt(0) == reverseSortTable.rowKeys*.keys*.getAt(0).sort(false)

        when:
        def descTableConfig = new TableConfig(
                rowDimensions: ['study', 'patient'],
                columnDimensions: ['trial visit', 'concept'],
                rowSort: [SortSpecification.desc('patient'), SortSpecification.desc('study')],
                columnSort: [SortSpecification.asc('concept'), SortSpecification.asc('trial visit')]
        )
        PagingDataTable descSortTable = queryResource.retrieveDataTablePage(descTableConfig, pagination, 'clinical', constraint, adminUser)
        def descSortDims = reverseSortTable.sort.keySet().withIndex().collectEntries()

        then:
        descSortTable.requestedSort == [(DimensionImpl.PATIENT): SortOrder.DESC, (DimensionImpl.STUDY): SortOrder.DESC,
                               (DimensionImpl.CONCEPT): SortOrder.ASC, (DimensionImpl.TRIAL_VISIT): SortOrder.ASC]
        descSortTable.requestedSort.each { dim, sort ->
            assert descSortTable.sort[dim] == sort
        }
        descSortDims[DimensionImpl.TRIAL_VISIT] > descSortDims[DimensionImpl.CONCEPT]
        descSortDims[DimensionImpl.STUDY] > descSortDims[DimensionImpl.PATIENT]
    }

    void testInvalidSort() {
        setupData()

        when:
        def tableConfig = new TableConfig(
                rowDimensions: ['study', 'patient'],
                columnDimensions: ['trial visit', 'concept'],
                rowSort: [SortSpecification.asc('patient'), SortSpecification.asc('concept')],
                columnSort: [SortSpecification.asc('trial visit')]
        )
        def pagination = new PaginationParameters(limit: 10)
        PagingDataTable table = queryResource.retrieveDataTablePage(tableConfig, pagination, 'clinical', constraint, adminUser)

        then:
        thrown InvalidArgumentsException

        when:
        tableConfig.rowSort = [SortSpecification.asc('patient')]
        tableConfig.columnSort = [SortSpecification.asc('study'), SortSpecification.asc('trial visit')]
        table = queryResource.retrieveDataTablePage(tableConfig, pagination, 'clinical', constraint, adminUser)

        then:
        thrown InvalidArgumentsException
    }

}
