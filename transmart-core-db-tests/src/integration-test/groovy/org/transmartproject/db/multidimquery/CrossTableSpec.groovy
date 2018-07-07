package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.User
import org.transmartproject.db.TestData
import spock.lang.Specification
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.user.AccessLevelTestData

@Integration
@Rollback
class CrossTableSpec extends Specification {

    @Autowired
    SessionFactory sessionFactory

    TestData testData
    ClinicalTestData clinicalData
    AccessLevelTestData accessLevelTestData
    User adminUser

    @Autowired
    CrossTableResource crossTableResource

    @Autowired
    PatientSetResource patientSetResource

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

    void 'test basic crosstable retrieval'() {
        setupData()
        String apiVersion = "2.1-tests"
        def adminUser = accessLevelTestData.users[0]

        Constraint constraint = new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN_OR_EQUALS, 100)
        def patientSet = patientSetResource.createPatientSetQueryResult("Test crosstable set ",
                constraint,
                adminUser,
                apiVersion,
                true)
        Constraint subjectConstraint = new PatientSetConstraint(patientSet.id)
        List<Constraint> rowConstraints = [
                new StudyNameConstraint(testData.clinicalData.multidimsStudy.name),
                new TrueConstraint(),
                new StudyNameConstraint(testData.clinicalData.multidimsStudy.name),
                new ValueConstraint(Type.NUMERIC, Operator.GREATER_THAN, 100)
        ]
        List<Constraint> columnConstraints = [
                new TrueConstraint(),
                new ValueConstraint(Type.NUMERIC, Operator.GREATER_THAN, 60)
        ]

        when: 'passing patient set constraint'
        def result = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, adminUser)

        then: 'proper counts are returned'
        result.rows.size() == rowConstraints.size()
        result.rows.every { it.counts.size() == columnConstraints.size() }

        result.rows[1].counts[0] == patientSet.setSize // intersection of patient constraints and 2x true constraints
        result.rows[0].counts == result.rows[2].counts // same row constraints
        result.rows[3].counts.every { it == 0 } // conflicting constraints (row AND patientSet constraints)

        result.rows[0].counts == [1, 0]
        result.rows[1].counts == [3, 1]
        result.rows[2].counts == [1, 0]
        result.rows[3].counts == [0, 0]

        when: 'passing arbitrary constraints instead of a patient set constraint'
        def subjectConstraint2 = constraint
        def result2 = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint2, adminUser)

        then: 'the same result is returned'
        result2.rows[0].counts == result.rows[0].counts
        result2.rows[1].counts == result.rows[1].counts
        result2.rows[2].counts == result.rows[2].counts
        result2.rows[3].counts == result.rows[3].counts

    }

    void 'test cross table retrieval on non-existing patient set'() {
        setupData()
        def adminUser = accessLevelTestData.users[0]
        // not existing patient set id
        Constraint subjectConstraint = new PatientSetConstraint(-99999)
        List<Constraint> rowConstraints = [
                new StudyNameConstraint(testData.clinicalData.multidimsStudy.name),
                new TrueConstraint(),
                new StudyNameConstraint(testData.clinicalData.multidimsStudy.name),
                new ValueConstraint(Type.NUMERIC, Operator.GREATER_THAN, "invalid constraint")
        ]
        List<Constraint> columnConstraints = [
                new TrueConstraint(),
                new ValueConstraint(Type.NUMERIC, Operator.GREATER_THAN, 60)
        ]

        when:
        def result = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, adminUser)

        then:
        assert result.rows.every { it.counts.every { it == 0 } }
    }

    void 'test empty cross table'() {
        setupData()
        String apiVersion = "2.1-tests"
        def adminUser = accessLevelTestData.users[0]

        Constraint subjectConstraint = new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN_OR_EQUALS, 100)
        List<Constraint> rowConstraints = []
        List<Constraint> columnConstraints = []

        when:
        def result = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, adminUser)

        then:
        result.rows.size() == 0
    }
}
