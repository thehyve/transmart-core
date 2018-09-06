package org.transmartproject.db.clinical

import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.User
import spock.lang.Specification

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD

class CrossTableResourceImplServiceSpec extends Specification {

    CrossTableService crossTableServiceMock = Mock(CrossTableService)
    MDStudiesResource mdStudiesResourceMock = Mock(MDStudiesResource)
    User userMock = Mock(User)
    MDStudy study1Mock = Mock(MDStudy)

    def setup() {
        study1Mock.name >> 'study1'
        userMock.username >> 'test-user'
    }

    Constraint constraintMock = Mock(Constraint)

    CrossTableResourceImpService testee = new CrossTableResourceImpService(
            crossTableService: crossTableServiceMock,
            mdStudiesResource: mdStudiesResourceMock)

    def 'cross table counts respect threshold'() {
        testee.patientCountThreshold = 10
        List<Constraint> rowConstraints = Mock(List)
        List<Constraint> columnConstraints = Mock(List)
        crossTableServiceMock.retrieveCrossTable(rowConstraints, columnConstraints, _, {
            it.username == userMock.username
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
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        expect:
        testee.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, userMock) == new CrossTable([
                [-2, 10, -2],
                [20, 9, 1],
        ])
    }

    def 'cross table counts when no counts below threshold'() {
        testee.patientCountThreshold = 5
        List<Constraint> rowConstraints = Mock(List)
        List<Constraint> columnConstraints = Mock(List)
        def crossTable = new CrossTable([
                [10, 5, 10],
        ])
        crossTableServiceMock.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, {
            it.username == userMock.username
        }) >> crossTable
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        expect:
        testee.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, userMock) == crossTable
    }

    def 'cross table counts when no counts with threshold studies'() {
        testee.patientCountThreshold = 5
        List<Constraint> rowConstraints = Mock(List)
        List<Constraint> columnConstraints = Mock(List)
        def crossTable = new CrossTable([
                [1, 1, 1],
        ])
        crossTableServiceMock.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, {
            it.username == userMock.username
        }) >> crossTable
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, userMock) == crossTable
    }
}
