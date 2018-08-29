package org.transmartproject.db.clinical

import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.DimensionImpl
import spock.lang.Specification
import spock.lang.Unroll

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD
import static org.transmartproject.core.users.PatientDataAccessLevel.SUMMARY

class CountsWithThresholdServiceSpec extends Specification {

    LowerThreshold lowerThreshold = new LowerThreshold()
    
    AggregateDataService aggregateDataResourceMock = Mock(AggregateDataService)

    Constraint constraintMock = Mock(Constraint)

    CountsWithThresholdService testee = new CountsWithThresholdService(
            aggregateDataService: aggregateDataResourceMock,
            lowerThreshold: lowerThreshold)

    def 'counts below threshold'() {
        lowerThreshold.patientCountThreshold = 10
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
        lowerThreshold.patientCountThreshold = 10
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
        lowerThreshold.patientCountThreshold = 10
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
        lowerThreshold.patientCountThreshold = patientCountThreshold
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
    def 'zero patient count for threshold = #threshold'() {
        lowerThreshold.patientCountThreshold = patientCountThreshold
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.counts(_, { it.username == user.username }) >>> [
                new Counts(patientCount: 0),
                new Counts(patientCount: 0),
        ]

        when:
        Counts counts = testee.counts(constraintMock, user)

        then:
        counts.patientCount == returnedPatientCount

        where:
        patientCountThreshold | returnedPatientCount
        1                     | -2
        0                     | 0
    }

    @Unroll
    def 'patient count below threshold if there are #patientsFromStudy2 patients from protected study in the end result'() {
        lowerThreshold.patientCountThreshold = 10
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

    def 'patient count per concept'() {
        lowerThreshold.patientCountThreshold = 10
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

    @Unroll
    def 'zero patient count per concept for threshold = #threshold'() {
        lowerThreshold.patientCountThreshold = patientCountThreshold
        User user = new SimpleUser(username: 'test', admin: false,
                studyToPatientDataAccessLevel: [study1: COUNTS_WITH_THRESHOLD])
        aggregateDataResourceMock.countsPerConcept(_, { it.username == user.username }) >>> [
                [concept1: new Counts(patientCount: 0)],
                [concept1: new Counts(patientCount: 0)],
        ]

        when:
        Map<String, Counts> counts = testee.countsPerConcept(constraintMock, user)

        then:
        counts.concept1
        counts.concept1.patientCount == returnedPatientCount

        where:
        patientCountThreshold | returnedPatientCount
        1                     | -2
        0                     | 0
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
        lowerThreshold.patientCountThreshold = 10
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
        lowerThreshold.patientCountThreshold = 10
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

}
