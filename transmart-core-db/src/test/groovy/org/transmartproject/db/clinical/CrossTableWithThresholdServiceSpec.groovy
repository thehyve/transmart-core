package org.transmartproject.db.clinical

import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.User
import spock.lang.Specification

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD
import static org.transmartproject.core.users.PatientDataAccessLevel.SUMMARY

class CrossTableWithThresholdServiceSpec extends Specification {

    LowerThreshold lowerThreshold = new LowerThreshold()
    
    CrossTableService crossTableServiceMock = Mock(CrossTableService)

    Constraint constraintMock = Mock(Constraint)

    CrossTableWithThresholdService testee = new CrossTableWithThresholdService(
            crossTableService: crossTableServiceMock,
            lowerThreshold: lowerThreshold)

    def 'cross table counts respect threshold'() {
        lowerThreshold.patientCountThreshold = 10
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD])
        List<Constraint> rowConstraints = Mock(List)
        List<Constraint> columnConstraints = Mock(List)
        crossTableServiceMock.retrieveCrossTable(rowConstraints, columnConstraints, _, {
            it.username == user.username
        }) >>> [
                new CrossTable([
                        [0, 10, 5],
                        [20, 9, 1],
                ]),
                new CrossTable([
                        [1, 1, 1],
                        [0, 0, 0],
                ])
        ]

        expect:
        testee.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, user) == new CrossTable([
                [-2, 10, -2],
                [20, 9, 1],
        ])
    }

    def 'cross table counts when no threshold check needed'() {
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: SUMMARY])
        List<Constraint> rowConstraints = Mock(List)
        List<Constraint> columnConstraints = Mock(List)
        def crossTable = Mock(CrossTable)
        crossTableServiceMock.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, {
            it.username == user.username
        }) >> crossTable

        expect:
        testee.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, user) == crossTable
    }

}
