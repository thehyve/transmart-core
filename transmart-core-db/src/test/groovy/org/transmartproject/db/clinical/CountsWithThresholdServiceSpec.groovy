package org.transmartproject.db.clinical

import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.DimensionImpl
import spock.lang.Specification
import spock.lang.Unroll

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD

class CountsWithThresholdServiceSpec extends Specification {

    AggregateDataResource aggregateDataResourceMock = Mock(AggregateDataResource)
    CrossTableResource crossTableResourceMock = Mock(CrossTableResource)
    MDStudiesResource mdStudiesResourceMock = Mock(MDStudiesResource)
    User userMock = Mock(User)
    MDStudy study1Mock = Mock(MDStudy)
    MDStudy study3Mock = Mock(MDStudy)

    def setup() {
        study1Mock.name >> 'study1'
        study3Mock.name >> 'study3'
        userMock.username >> 'test-user'
    }

    Constraint constraintMock = Mock(Constraint)

    CountsWithThresholdService testee = new CountsWithThresholdService(
            aggregateDataResource: aggregateDataResourceMock,
            crossTableResource: crossTableResourceMock,
            mdStudiesResource: mdStudiesResourceMock)

    def 'counts below threshold'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        aggregateDataResourceMock.counts(_, { it.username == userMock.username }) >>> [
                new Counts(patientCount: patientCounts, observationCount: 17),
                new Counts(patientCount: patientCounts, observationCount: 17),
        ]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        when:
        Counts counts = testee.counts(constraintMock, userMock)

        then: 'patient and observation counts get hidden together when patient count is below the threshold'
        counts.patientCount == -2
        counts.observationCount == -2
    }

    def 'counts below threshold for user with no counts with threshold studies'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        aggregateDataResourceMock.counts(constraintMock, { it.username == userMock.username }) >> new Counts(
                patientCount: patientCounts, observationCount: 17)
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        when:
        Counts counts = testee.counts(constraintMock, userMock)

        then:
        counts.patientCount == patientCounts
        counts.observationCount == 17
    }

    @Unroll
    def 'patient count for threshold = #patientCountThreshold'() {
        testee.patientCountThreshold = patientCountThreshold
        long patientCounts = 5
        aggregateDataResourceMock.counts(_, { it.username == userMock.username }) >>> [
                new Counts(patientCount: patientCounts),
                new Counts(patientCount: patientCounts),
        ]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        when:
        Counts counts = testee.counts(constraintMock, userMock)

        then:
        counts.patientCount == returnedPatientCount

        where:
        patientCountThreshold | returnedPatientCount
        6                     | -2
        5                     | 5
        0                     | 5
    }

    @Unroll
    def 'zero patient count for threshold = #patientCountThreshold'() {
        testee.patientCountThreshold = patientCountThreshold
        aggregateDataResourceMock.counts(_, { it.username == userMock.username }) >>> [
                new Counts(patientCount: 0),
                new Counts(patientCount: 0),
        ]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        when:
        Counts counts = testee.counts(constraintMock, userMock)

        then:
        counts.patientCount == returnedPatientCount

        where:
        patientCountThreshold | returnedPatientCount
        1                     | -2
        0                     | 0
    }

    @Unroll
    def 'patient count below threshold if there are #patientsFromStudy2 patients from protected study in the end result'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        aggregateDataResourceMock.counts(_, { it.username == userMock.username }) >>> [
                new Counts(patientCount: patientCounts),
                new Counts(patientCount: patientsFromStudy2) //on the second call
        ]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        when:
        Counts counts = testee.counts(constraintMock, userMock)

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
        aggregateDataResourceMock.countsPerConcept(_, { it.username == userMock.username }) >>> [
                [concept1: new Counts(patientCount: 1), concept2: new Counts(patientCount: 5), concept3: new Counts(patientCount: 10)],
                [concept1: new Counts(patientCount: 1), concept2: new Counts(patientCount: 0), concept3: new Counts(patientCount: 1)],
        ]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        when:
        Map<String, Counts> counts = testee.countsPerConcept(constraintMock, userMock)

        then:
        counts
        counts.concept1
        counts.concept1.patientCount == -2
        counts.concept2
        counts.concept2.patientCount == 5
        counts.concept3
        counts.concept3.patientCount == 10
    }

    @Unroll
    def 'zero patient count per concept for threshold = #patientCountThreshold'() {
        testee.patientCountThreshold = patientCountThreshold
        aggregateDataResourceMock.countsPerConcept(_, { it.username == userMock.username }) >>> [
                [concept1: new Counts(patientCount: 0)],
                [concept1: new Counts(patientCount: 0)],
        ]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock]

        when:
        Map<String, Counts> counts = testee.countsPerConcept(constraintMock, userMock)

        then:
        counts.concept1
        counts.concept1.patientCount == returnedPatientCount

        where:
        patientCountThreshold | returnedPatientCount
        1                     | -2
        0                     | 0
    }


    def 'patient count per concept when no threshold check needed'() {
        def countsPerConcept = [concept1: new Counts(patientCount: 1), concept2: new Counts(patientCount: 5), concept3: new Counts(patientCount: 10)]
        aggregateDataResourceMock.countsPerConcept(constraintMock, { it.username == userMock.username }) >> countsPerConcept
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.countsPerConcept(constraintMock, userMock) == countsPerConcept
    }

    def 'patient count per study'() {
        testee.patientCountThreshold = 10
        aggregateDataResourceMock.countsPerStudy(constraintMock, {
            it.username == userMock.username
        }) >> [study1: new Counts(patientCount: 1), study2: new Counts(patientCount: 5), study3: new Counts(patientCount: 10)]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock, study3Mock]

        when:
        Map<String, Counts> counts = testee.countsPerStudy(constraintMock, userMock)

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
        def countsPerStudy = [study1: new Counts(patientCount: 1)]
        aggregateDataResourceMock.countsPerStudy(constraintMock, { it.username == userMock.username }) >> countsPerStudy
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.countsPerStudy(constraintMock, userMock) == countsPerStudy
    }

    def 'patient count per study and concept'() {
        testee.patientCountThreshold = 10
        aggregateDataResourceMock.countsPerStudyAndConcept(constraintMock, { it.username == userMock.username }) >> [
                study1: [concept1: new Counts(patientCount: 1)],
                study2: [concept2: new Counts(patientCount: 5)],
                study3: [concept3: new Counts(patientCount: 10)]]
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> [study1Mock, study3Mock]

        when:
        Map<String, Map<String, Counts>> counts = testee.countsPerStudyAndConcept(constraintMock, userMock)

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
        def countsPerStudyAndConcept = [study1: [concept1: new Counts(patientCount: 1)]]
        aggregateDataResourceMock.countsPerStudyAndConcept(constraintMock, {
            it.username == userMock.username
        }) >> countsPerStudyAndConcept
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.countsPerStudyAndConcept(constraintMock, userMock) == countsPerStudyAndConcept
    }

    def 'count patient dimension elements'() {
        aggregateDataResourceMock.counts(constraintMock, {
            it.username == userMock.username
        }) >> new Counts(patientCount: 100)

        expect: 'patient dimension elements are return'
        testee.getDimensionElementsCount(DimensionImpl.PATIENT, constraintMock, userMock) == 100L
    }

    def 'count other dimension elements'() {
        aggregateDataResourceMock.getDimensionElementsCount(DimensionImpl.CONCEPT, constraintMock, {
            it.username == userMock.username
        }) >> 50

        expect:
        testee.getDimensionElementsCount(DimensionImpl.CONCEPT, constraintMock, userMock) == 50L
    }

    def 'cross table counts respect threshold'() {
        testee.patientCountThreshold = 10
        List<Constraint> rowConstraints = Mock(List)
        List<Constraint> columnConstraints = Mock(List)
        crossTableResourceMock.retrieveCrossTable(rowConstraints, columnConstraints, _, {
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
        crossTableResourceMock.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, {
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
        crossTableResourceMock.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, {
            it.username == userMock.username
        }) >> crossTable
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.retrieveCrossTable(rowConstraints, columnConstraints, constraintMock, userMock) == crossTable
    }
}
