package org.transmartproject.db.clinical

import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.DimensionImpl
import spock.lang.Specification
import spock.lang.Unroll

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD

class AggregateDataResourceImplServiceSpec extends Specification {

    AggregateDataService aggregateDataServiceMock = Mock(AggregateDataService)
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

    AggregateDataResourceImplService testee = new AggregateDataResourceImplService(
            aggregateDataService: aggregateDataServiceMock,
            mdStudiesResource: mdStudiesResourceMock)

    def 'counts below threshold'() {
        testee.patientCountThreshold = 10
        long patientCounts = 5
        aggregateDataServiceMock.counts(_, { it.username == userMock.username }) >>> [
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
        aggregateDataServiceMock.counts(constraintMock, { it.username == userMock.username }) >> new Counts(
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
        aggregateDataServiceMock.counts(_, { it.username == userMock.username }) >>> [
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
        aggregateDataServiceMock.counts(_, { it.username == userMock.username }) >>> [
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
        aggregateDataServiceMock.counts(_, { it.username == userMock.username }) >>> [
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

    def 'patient count per concept'() {
        testee.patientCountThreshold = 10
        aggregateDataServiceMock.countsPerConcept(_, { it.username == userMock.username }) >>> [
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
        aggregateDataServiceMock.countsPerConcept(_, { it.username == userMock.username }) >>> [
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
        aggregateDataServiceMock.countsPerConcept(constraintMock, {
            it.username == userMock.username
        }) >> countsPerConcept
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.countsPerConcept(constraintMock, userMock) == countsPerConcept
    }

    def 'patient count per study'() {
        testee.patientCountThreshold = 10
        aggregateDataServiceMock.countsPerStudy(constraintMock, {
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
        aggregateDataServiceMock.countsPerStudy(constraintMock, { it.username == userMock.username }) >> countsPerStudy
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.countsPerStudy(constraintMock, userMock) == countsPerStudy
    }

    def 'patient count per study and concept'() {
        testee.patientCountThreshold = 10
        aggregateDataServiceMock.countsPerStudyAndConcept(constraintMock, { it.username == userMock.username }) >> [
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
        aggregateDataServiceMock.countsPerStudyAndConcept(constraintMock, {
            it.username == userMock.username
        }) >> countsPerStudyAndConcept
        mdStudiesResourceMock.getStudiesWithPatientDataAccessLevel(userMock, COUNTS_WITH_THRESHOLD) >> []

        expect:
        testee.countsPerStudyAndConcept(constraintMock, userMock) == countsPerStudyAndConcept
    }

    def 'count patient dimension elements'() {
        aggregateDataServiceMock.counts(constraintMock, {
            it.username == userMock.username
        }) >> new Counts(patientCount: 100)

        expect: 'patient dimension elements are return'
        testee.getDimensionElementsCount(DimensionImpl.PATIENT, constraintMock, userMock) == 100L
    }

    def 'count other dimension elements'() {
        aggregateDataServiceMock.getDimensionElementsCount(DimensionImpl.CONCEPT, constraintMock, {
            it.username == userMock.username
        }) >> 50

        expect:
        testee.getDimensionElementsCount(DimensionImpl.CONCEPT, constraintMock, userMock) == 50L
    }

}
