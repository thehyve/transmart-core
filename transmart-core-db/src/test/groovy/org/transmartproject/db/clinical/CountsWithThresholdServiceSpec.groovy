package org.transmartproject.db.clinical

import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.DimensionImpl
import spock.lang.Specification
import spock.lang.Unroll

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD
import static org.transmartproject.core.users.PatientDataAccessLevel.SUMMARY

class CountsWithThresholdServiceSpec extends Specification {

    AggregateDataResource aggregateDataResourceMock = Mock(AggregateDataResource)
    CrossTableResource crossTableResourceMock = Mock(CrossTableResource)

    Constraint constraintMock = Mock(Constraint)

    CountsWithThresholdService testee = new CountsWithThresholdService(
            aggregateDataResource: aggregateDataResourceMock,
            crossTableResource: crossTableResourceMock)

    def 'counts below threshold'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.counts(_, { it.username == user.username }) >>> [
                new Counts(patientCount: patientCounts, observationCount: 17),
                new Counts(patientCount: patientCounts, observationCount: 17),
        ]

        when:
        Counts counts = testee.counts(constraintMock, user)

        then: 'patient and observation counts get hidden together when patient count is below the threshold'
        counts.patientCount == -2
        counts.observationCount == -2
    }

    def 'counts below threshold for admin user'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        User user = new SimpleUser(username: 'test',
                admin: true)
        aggregateDataResourceMock.counts(constraintMock, { it.username == user.username }) >> new Counts(
                patientCount: patientCounts, observationCount: 17)

        when:
        Counts counts = testee.counts(constraintMock, user)

        then: 'admin gets counts despite patient count being below the threshold'
        counts.patientCount == patientCounts
        counts.observationCount == 17
    }

    def 'counts below threshold for user with higher patient data access level'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: SUMMARY])
        aggregateDataResourceMock.counts(constraintMock, { it.username == user.username }) >> new Counts(
                patientCount: patientCounts, observationCount: 17)

        when:
        Counts counts = testee.counts(constraintMock, user)

        then:
        counts.patientCount == patientCounts
        counts.observationCount == 17
    }

    @Unroll
    def 'patient count for threshold = #threshold'() {
        testee.patientCountThreshold = patientCountThreshold
        long patientCounts = 5
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.counts(_, { it.username == user.username }) >>> [
                new Counts(patientCount: patientCounts),
                new Counts(patientCount: patientCounts),
        ]

        when:
        Counts counts = testee.counts(constraintMock, user)

        then:
        counts.patientCount == returnedPatientCount

        where:
        patientCountThreshold | returnedPatientCount
        6                     | -2
        5                     | 5
        0                     | 5
    }

    @Unroll
    def 'patient count below threshold if there are #patientsFromStudy2 patients from protected study in the end result'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: SUMMARY, study2: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.counts(_, { it.username == user.username }) >>> [
                new Counts(patientCount: patientCounts),
                new Counts(patientCount: patientsFromStudy2) //on the second call
        ]

        when:
        Counts counts = testee.counts(constraintMock, user)

        then:
        counts.patientCount == returnedPatientCount

        where:
        patientsFromStudy2 | returnedPatientCount
        0                  | 5
        1                  | -2
    }

    def 'constraint limited to patients of studies'() {
        expect:
        testee.getConstraintLimitedToStudyPatients(constraintMock, ['study1', 'study2'] as Set) ==
                new AndConstraint([
                        constraintMock,
                        new SubSelectionConstraint(
                                'patient',
                                new OrConstraint([
                                        new StudyNameConstraint('study1'),
                                        new StudyNameConstraint('study2'),
                                ])
                        )]
                )
    }

    def 'patient count per concept'() {
        testee.patientCountThreshold = 10
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.countsPerConcept(_, { it.username == user.username }) >>> [
                [concept1: new Counts(patientCount: 1), concept2: new Counts(patientCount: 5), concept3: new Counts(patientCount: 10)],
                [concept1: new Counts(patientCount: 1), concept2: new Counts(patientCount: 0), concept3: new Counts(patientCount: 1)],
        ]

        when:
        Map<String, Counts> counts = testee.countsPerConcept(constraintMock, user)

        then:
        counts
        counts.concept1
        counts.concept1.patientCount == -2
        counts.concept2
        counts.concept2.patientCount == 5
        counts.concept3
        counts.concept3.patientCount == 10
    }

    def 'patient count per concept when no threshold check needed'() {
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: SUMMARY])
        def countsPerConcept = [concept1: new Counts(patientCount: 1), concept2: new Counts(patientCount: 5), concept3: new Counts(patientCount: 10)]
        aggregateDataResourceMock.countsPerConcept(constraintMock, { it.username == user.username }) >> countsPerConcept

        expect:
        testee.countsPerConcept(constraintMock, user) == countsPerConcept
    }

    def 'patient count per study'() {
        testee.patientCountThreshold = 10
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD, study2: SUMMARY, study3: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.countsPerStudy(constraintMock, {
            it.username == user.username
        }) >> [study1: new Counts(patientCount: 1), study2: new Counts(patientCount: 5), study3: new Counts(patientCount: 10)]

        when:
        Map<String, Counts> counts = testee.countsPerStudy(constraintMock, user)

        then:
        counts
        counts.study1
        counts.study1.patientCount == -2
        counts.study2
        counts.study2.patientCount == 5
        counts.study3
        counts.study3.patientCount == 10
    }

    def 'patient count per study when no threshold check needed'() {
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: SUMMARY])
        def countsPerStudy = [study1: new Counts(patientCount: 1)]
        aggregateDataResourceMock.countsPerStudy(constraintMock, { it.username == user.username }) >> countsPerStudy

        expect:
        testee.countsPerStudy(constraintMock, user) == countsPerStudy
    }

    def 'patient count per study and concept'() {
        testee.patientCountThreshold = 10
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD, study2: SUMMARY, study3: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.countsPerStudyAndConcept(constraintMock, { it.username == user.username }) >> [
                study1: [concept1: new Counts(patientCount: 1)],
                study2: [concept2: new Counts(patientCount: 5)],
                study3: [concept3: new Counts(patientCount: 10)]]

        when:
        Map<String, Map<String, Counts>> counts = testee.countsPerStudyAndConcept(constraintMock, user)

        then:
        counts
        counts.study1
        counts.study1.concept1
        counts.study1.concept1.patientCount == -2
        counts.study2
        counts.study2.concept2
        counts.study2.concept2.patientCount == 5
        counts.study3
        counts.study3.concept3
        counts.study3.concept3.patientCount == 10
    }

    def 'patient count per study and concept when no threshold check needed'() {
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: SUMMARY])
        def countsPerStudyAndConcept = [study1: [concept1: new Counts(patientCount: 1)]]
        aggregateDataResourceMock.countsPerStudyAndConcept(constraintMock, {
            it.username == user.username
        }) >> countsPerStudyAndConcept

        expect:
        testee.countsPerStudyAndConcept(constraintMock, user) == countsPerStudyAndConcept
    }

    def 'count patient dimension elements'() {
        def userMock = Mock(User)
        aggregateDataResourceMock.counts(constraintMock, {
            it.username == userMock.username
        }) >> new Counts(patientCount: 100)

        expect: 'patient dimension elements are return'
        testee.getDimensionElementsCount(DimensionImpl.PATIENT, constraintMock, userMock) == 100L
    }

    def 'count other dimension elements'() {
        def userMock = Mock(User)
        aggregateDataResourceMock.getDimensionElementsCount(DimensionImpl.CONCEPT, constraintMock, {
            it.username == userMock.username
        }) >> 50

        expect:
        testee.getDimensionElementsCount(DimensionImpl.CONCEPT, constraintMock, userMock) == 50L
    }

    def 'cross table counts respect threshold'() {
        testee.patientCountThreshold = 10
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD])
        List<Constraint> rowConstraints = Mock(List)
        List<Constraint> columnConstraints = Mock(List)
        crossTableResourceMock.retrieveCrossTable(rowConstraints, columnConstraints, _, {
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
        crossTableResourceMock.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, {
            it.username == user.username
        }) >> crossTable

        expect:
        testee.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, user) == crossTable
    }
}
