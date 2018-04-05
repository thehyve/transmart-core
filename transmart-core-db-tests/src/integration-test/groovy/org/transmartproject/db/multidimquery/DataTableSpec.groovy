package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
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

    void testSimple() {
        setupData()

        Constraint constraint = new AndConstraint([
                new StudyObjectConstraint(study: clinicalData.longitudinalStudy),
                new FieldConstraint(field:
                        new Field(dimension: 'trial visit', type: Type.NUMERIC, fieldName: 'relTime'),
                            operator: Operator.EQUALS, value: 0)
        ])

        when:
        DataTable table = queryResource.retrieveDataTable('clinical', constraint, adminUser,
                rowDimensions: ['patient'], columnDimensions: ['concept'], offset: 0, limit: 10)

        then:
        table.rowDimensions == [DimensionImpl.PATIENT]
        table.columnDimensions == [DimensionImpl.CONCEPT]
        table.rowKeys*.elements*.getAt(0) as Set == (clinicalData.longitudinalClinicalFacts*.patient as Set)
        table.columnKeys*.elements*.getAt(0)*.conceptCode as Set == clinicalData.longitudinalClinicalFacts*.conceptCode as Set


    }

}
