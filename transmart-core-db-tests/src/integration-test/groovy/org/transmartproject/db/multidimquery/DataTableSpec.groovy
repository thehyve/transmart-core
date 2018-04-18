package org.transmartproject.db.multidimquery

import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.DataTableRow
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.StreamingDataTable
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.StudyObjectConstraint
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.users.User
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.user.AccessLevelTestData

@Integration
@Rollback
class DataTableSpec extends TransmartSpecification {

    @Autowired
    SessionFactory sessionFactory

    TestData testData
    ClinicalTestData clinicalData
    AccessLevelTestData accessLevelTestData
    User adminUser

    @Autowired
    MultiDimensionalDataResource queryResource

    void setupData() {
        TestData.clearAllData()

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
        DataTable table = queryResource.retrieveDataTable('clinical', constraint, adminUser,
                rowDimensions: ['study', 'patient'], columnDimensions: ['trial visit', 'concept'], offset: 0, limit: 10)
        def sortDims = table.sort.keySet().withIndex().collectEntries()


        then:
        table.rowDimensions == [DimensionImpl.STUDY, DimensionImpl.PATIENT]
        table.columnDimensions == [DimensionImpl.TRIAL_VISIT, DimensionImpl.CONCEPT]
        table.rowKeys*.elements*.getAt(1) as Set == (clinicalData.longitudinalClinicalFacts*.patient as Set)
        table.columnKeys*.elements*.getAt(1)*.conceptCode as Set == clinicalData.longitudinalClinicalFacts*.conceptCode as Set
        sortDims[DimensionImpl.TRIAL_VISIT] < sortDims[DimensionImpl.CONCEPT]
        sortDims[DimensionImpl.STUDY] < sortDims[DimensionImpl.PATIENT]
        table.columnKeys*.keys*.getAt(0) == table.columnKeys*.keys*.getAt(0).sort(false)
        table.rowKeys*.keys*.getAt(0) == table.rowKeys*.keys*.getAt(0).sort(false)
        table.columnKeys == table.columnKeys.sort(false)

        when:
        def secondRow = table.row(table.rowKeys[1])
        DataTable subTable = queryResource.retrieveDataTable('clinical', constraint, adminUser,
                rowDimensions: ['study', 'patient'], columnDimensions: ['trial visit', 'concept'], offset: 1, limit: 1)

        then:
        subTable.rowKeys.size() == 1
        subTable.rowKeys[0] == table.rowKeys[1]
        subTable.row(subTable.rowKeys[0]).collectEntries { col, vals -> [col, vals[0].value] } ==
                secondRow.collectEntries { col, vals -> [col, vals[0].value] }

    }

    void testStreaming() {
        setupData()

        when:
        StreamingDataTable table = queryResource.retrieveStreamingDataTable('clinical', constraint, adminUser,
                rowDimensions: ['study', 'patient'], columnDimensions: ['trial visit', 'concept'])
        List<DataTableRow> rows = Lists.newArrayList(table)

        then:
        true
    }

    void testSort() {
        setupData()

        when:
        DataTable reverseSortTable = queryResource.retrieveDataTable('clinical', constraint, adminUser, 
                rowDimensions: ['study', 'patient'], columnDimensions: ['trial visit', 'concept'], limit: 10,
                rowSort: ['patient', 'study'], columnSort: ['concept', 'trial visit'])
        def reverseSortDims = reverseSortTable.sort.keySet().withIndex().collectEntries()
        
        then:
        reverseSortDims[DimensionImpl.TRIAL_VISIT] > reverseSortDims[DimensionImpl.CONCEPT]
        reverseSortDims[DimensionImpl.STUDY] > reverseSortDims[DimensionImpl.PATIENT]
        reverseSortTable.columnKeys*.keys*.getAt(0) == reverseSortTable.columnKeys*.keys*.getAt(0).sort(false)
        reverseSortTable.rowKeys*.keys*.getAt(0) == reverseSortTable.rowKeys*.keys*.getAt(0).sort(false)
    }

    void testInvalidSort() {
        setupData()

        when:
        DataTable table = queryResource.retrieveDataTable('clinical', constraint, adminUser,
                rowDimensions: ['study', 'patient'], columnDimensions: ['trial visit', 'concept'], limit: 10,
                rowSort: ['patient', 'concept'], columnSort: ['trial visit'])

        then:
        thrown InvalidArgumentsException

        when:
        table = queryResource.retrieveDataTable('clinical', constraint, adminUser,
                rowDimensions: ['study', 'patient'], columnDimensions: ['trial visit', 'concept'], limit: 10,
                rowSort: ['patient'], columnSort: ['study', 'trial visit'])

        then:
        thrown InvalidArgumentsException

        when:
        table = queryResource.retrieveDataTable('clinical', constraint, adminUser,
                rowDimensions: ['study', 'patient'], columnDimensions: ['trial visit', 'concept'], limit: 10,
                rowSort: ['patient': 'foo'])

        then:
        thrown InvalidArgumentsException


    }

}
