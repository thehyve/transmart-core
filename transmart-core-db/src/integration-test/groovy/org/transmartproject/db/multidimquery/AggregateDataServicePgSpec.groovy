/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.clinical.AggregateDataService
import spock.lang.Specification

@Rollback
@Integration
class AggregateDataServicePgSpec extends Specification {

    @Autowired
    AggregateDataService aggregateDataService

    @Autowired
    UsersResource usersResource

    @Autowired
    CacheManager cacheManager

    /**
     * Test the functionality to count patients and observations grouped by
     * study, concept, or concept and study.
     */
    void "test counts per study, concept"() {
        def user = usersResource.getUserFromUsername('test-public-user-1')
        def admin = usersResource.getUserFromUsername('admin')
        Constraint studyConstraint = new StudyNameConstraint(studyId: "EHR")

        when: "fetching all counts per concept for study EHR"
        def countsPerConcept = aggregateDataService.countsPerConcept(studyConstraint, user)

        then: "the result should contain entries for both concepts in the study"
        !countsPerConcept.isEmpty()
        countsPerConcept.keySet() == ['EHR:DEM:AGE', 'EHR:VSIGN:HR'] as Set

        then: "the result should contain the correct counts for both concepts"
        countsPerConcept['EHR:DEM:AGE'].patientCount == 3
        countsPerConcept['EHR:DEM:AGE'].observationCount == 3
        countsPerConcept['EHR:VSIGN:HR'].patientCount == 3
        countsPerConcept['EHR:VSIGN:HR'].observationCount == 9

        when: "fetching all counts per study"
        def countsPerStudy = aggregateDataService.countsPerStudy(new TrueConstraint(), user)

        then: "the result should contain all study ids as key"
        !countsPerStudy.isEmpty()
        countsPerStudy.keySet() == [
                'CATEGORICAL_VALUES',
                'CLINICAL_TRIAL',
                'CLINICAL_TRIAL_HIGHDIM',
                'EHR',
                'EHR_HIGHDIM',
                'MIX_HD',
                'ORACLE_1000_PATIENT',
                'RNASEQ_TRANSCRIPT',
                'SHARED_CONCEPTS_STUDY_A',
                'SHARED_CONCEPTS_STUDY_B',
                'SHARED_HD_CONCEPTS_STUDY_A',
                'SHARED_HD_CONCEPTS_STUDY_B',
                'TUMOR_NORMAL_SAMPLES',
                'SURVEY1',
                'SURVEY2',
                '100_CATS',
                'CSR',
                'IMAGES',
                'MULTI-DWH-TEST-STUDY',
                'ONTOLOGY-OVERLAP-STUDY'
        ] as Set

        then: "the result should have the correct counts for study EHR"
        countsPerStudy['EHR'].patientCount == 3
        countsPerStudy['EHR'].observationCount == 12

        when: "fetching all counts per study and concept"
        def countsPerStudyAndConcept = aggregateDataService.countsPerStudyAndConcept(new TrueConstraint(), user)
        def counts = aggregateDataService.counts(new TrueConstraint(), user)

        then: "the result should contain the counts for study EHR and concept EHR:VSIGN:HR"
        !countsPerStudyAndConcept.isEmpty()
        countsPerStudyAndConcept.keySet() == countsPerStudy.keySet()
        countsPerStudyAndConcept['EHR']['EHR:VSIGN:HR'].patientCount == 3
        countsPerStudyAndConcept['EHR']['EHR:VSIGN:HR'].observationCount == 9

        then: "the total observation count should be equal to the sum of observation counts of the returned map"
        counts.observationCount == (long)countsPerStudyAndConcept.values().sum { Map<String, Counts> countsMap ->
            countsMap.values().sum { Counts c -> c.observationCount }
        }

        when: "fetching all counts per study and concept for admin"
        def countsPerStudyAndConceptForAdmin = aggregateDataService.countsPerStudyAndConcept(new TrueConstraint(), admin)

        then: "gives the same results"
        countsPerStudyAndConceptForAdmin
        (countsPerStudyAndConceptForAdmin.keySet() - countsPerStudyAndConcept.keySet()) == ["SHARED_CONCEPTS_STUDY_C_PRIV", "SHARED_HD_CONCEPTS_STUDY_C_PR"] as Set
    }

    void 'test numerical value aggregates'() {
        def user = usersResource.getUserFromUsername('test-public-user-1')
        def userWithAccessToMoreData = usersResource.getUserFromUsername('test-public-user-2')

        when:
        def heartRate = new ConceptConstraint(path: '\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\')
        def result = aggregateDataService.numericalValueAggregatesPerConcept(heartRate, user)
        then: 'expected aggregates are calculated'
        result.size() == 1
        'EHR:VSIGN:HR' in result
        result['EHR:VSIGN:HR'].min == 56d
        result['EHR:VSIGN:HR'].max == 102d
        result['EHR:VSIGN:HR'].avg.round(2) == 74.78d
        result['EHR:VSIGN:HR'].count == 9
        result['EHR:VSIGN:HR'].stdDev.round(2) == 14.7d

        when: 'numerical aggregates run on categorical measures'
        def categoricalConceptConstraint = new ConceptConstraint(
                path: '\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\')
        def emptyResult = aggregateDataService.numericalValueAggregatesPerConcept(categoricalConceptConstraint, user)
        then: 'no numerical aggregates returned'
        emptyResult.isEmpty()

        when: 'aggregate runs for dataset with one value and one missing value'
        def missingValuesConstraint = new ConceptConstraint(path: '\\Demographics\\Height\\')
        def oneValueResult = aggregateDataService.numericalValueAggregatesPerConcept(missingValuesConstraint, user)
        then: 'aggregates calculates on the single value'
        oneValueResult.size() == 1
        'height' in oneValueResult
        oneValueResult['height'].min == 169d
        oneValueResult['height'].max == 169d
        oneValueResult['height'].avg == 169d
        oneValueResult['height'].count == 1
        oneValueResult['height'].stdDev == null

        when: 'we calculate aggregates for shared concept with user that don\'t have access to one study'
        def crossStudyHeartRate = new ConceptConstraint(path: '\\Vital Signs\\Heart Rate\\')
        def excludingSecuredRecords = aggregateDataService
                .numericalValueAggregatesPerConcept(crossStudyHeartRate, user)
        then: 'only values user have access are taken to account'
        excludingSecuredRecords.size() == 1
        'VSIGN:HR' in excludingSecuredRecords
        excludingSecuredRecords['VSIGN:HR'].count == 5

        when: 'we calculate the same aggregates with user that have access right to the protected study'
        def includingSecuredRecords = aggregateDataService
                .numericalValueAggregatesPerConcept(crossStudyHeartRate, userWithAccessToMoreData)
        then: 'the protected study numerical observations are taken to account'
        includingSecuredRecords.size() == 1
        'VSIGN:HR' in includingSecuredRecords
        includingSecuredRecords['VSIGN:HR'].count == 7
    }

    void 'test categorical value aggregates'() {
        def user = usersResource.getUserFromUsername('test-public-user-1')
        def userWithAccessToMoreData = usersResource.getUserFromUsername('test-public-user-2')

        when:
        def race = new ConceptConstraint(path: '\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\')
        def result = aggregateDataService.categoricalValueAggregatesPerConcept(race, user)
        then: 'expected categorical values counts have been returned'
        result.size() == 1
        'CV:DEM:RACE' in result
        result['CV:DEM:RACE'].valueCounts == [ Caucasian: 2, Latino: 1 ]

        when: 'categorical aggregates run on numerical measures'
        def heartRate = new ConceptConstraint(path: '\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\')
        def emptyResult = aggregateDataService.categoricalValueAggregatesPerConcept(heartRate, user)
        then: 'no categorical aggregates returned'
        emptyResult.isEmpty()

        when: 'aggregate runs for dataset with one value and one missing value'
        def gender = new ConceptConstraint(path: '\\Demographics\\Gender\\')
        def withMissingValueResult = aggregateDataService.categoricalValueAggregatesPerConcept(gender, user)
        then: 'answer contains count for the value and count for null value'
        withMissingValueResult.size() == 1
        'gender' in withMissingValueResult
        withMissingValueResult['gender'].valueCounts['Male'] == 8
        withMissingValueResult['gender'].valueCounts['Female'] == 5
        withMissingValueResult['gender'].nullValueCounts == 1

        when: 'categorical aggregates runs on crosstudy concept with user that have limite access'
        def placeOfBirth = new ConceptConstraint(path: '\\Demographics\\Place of birth\\')
        def excludingSecuredRecords = aggregateDataService.categoricalValueAggregatesPerConcept(placeOfBirth, user)
        then: 'answer excludes not visible observations for the user'
        excludingSecuredRecords.size() == 1
        'DEMO:POB' in excludingSecuredRecords
        excludingSecuredRecords['DEMO:POB'].valueCounts['Place1'] == 1
        excludingSecuredRecords['DEMO:POB'].valueCounts['Place2'] == 3
        excludingSecuredRecords['DEMO:POB'].nullValueCounts == 0

        when: 'now by user who has access to the private study'
        def includingSecuredRecords = aggregateDataService.categoricalValueAggregatesPerConcept(placeOfBirth,
                userWithAccessToMoreData)
        then: 'answer includes observations from the private study'
        includingSecuredRecords.size() == 1
        'DEMO:POB' in includingSecuredRecords
        includingSecuredRecords['DEMO:POB'].valueCounts['Place1'] == 2
        includingSecuredRecords['DEMO:POB'].valueCounts['Place2'] == 4
        includingSecuredRecords['DEMO:POB'].nullValueCounts == 0
    }

    //TODO Expected to fail after TMT-418 fix
    def 'test date aggregates'() {
        def birthdate = new ConceptConstraint(conceptCode: 'birthdate')
        def user = new SimpleUser(username: 'admin', admin: true)

        when: 'ask for aggregates for the date'
        def aggregates = aggregateDataService.numericalValueAggregatesPerConcept(birthdate, user)

        then: 'get back numerical aggregates with min and max'
        'birthdate' in aggregates
        def aggregatesAgg = aggregates['birthdate']
        aggregatesAgg.min
        aggregatesAgg.max
        aggregatesAgg.min < aggregatesAgg.max
    }
    void 'test caching counts per concept'() {

        given: "all counts caches are empty"
        String countsPerConceptCacheName = 'org.transmartproject.db.clinical.AggregateDataService.countsPerConcept'
        String countsPerStudyCacheName = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudy'
        String countsPerStudyAndConceptCacheName = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudyAndConcept'
        cacheManager.getCache(countsPerStudyAndConceptCacheName).clear()
        cacheManager.getCache(countsPerStudyCacheName).clear()
        cacheManager.getCache(countsPerConceptCacheName).clear()

        def user = usersResource.getUserFromUsername('test-public-user-1')
        Constraint studyConstraint = new StudyNameConstraint(studyId: "EHR")

        when: "I call countsPerConcept for study EHR"
        aggregateDataService.countsPerConcept(studyConstraint, user)
        def countsPerConceptCache = cacheManager.getCache(countsPerConceptCacheName)

        then: "countsPerConcept cache contains a new entry"
        countsPerConceptCache.getNativeCache().size() == 1

        when: "I call countsPerStudy for study EHR"
        aggregateDataService.countsPerStudy(studyConstraint, user)
        def countsPerStudyCache = cacheManager.getCache(countsPerStudyCacheName)

        then: "countsPerStudy contains a new entry"
        countsPerStudyCache.getNativeCache().size() == 1

        when: "I call countsPerStudyAndConcept for study EHR"
        aggregateDataService.countsPerStudyAndConcept(studyConstraint, user)
        def countsPerStudyAndConceptCache = cacheManager.getCache(countsPerStudyAndConceptCacheName)

        then: "countsPerStudyAndConcept cache contains a new entr"
        countsPerStudyAndConceptCache.getNativeCache().size() == 1
    }
}
