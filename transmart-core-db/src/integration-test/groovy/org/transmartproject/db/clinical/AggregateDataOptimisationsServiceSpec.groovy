package org.transmartproject.db.clinical

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.UsersResource
import spock.lang.Specification

@Rollback
@Integration
class AggregateDataOptimisationsServiceSpec extends Specification {

    @Autowired
    UsersResource usersResource

    @Autowired
    AggregateDataOptimisationsService aggregateDataOptimisationsService

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    StudiesResource studiesResource

    @Autowired
    PatientSetResource patientSetResource

    def 'counts per study and concept has been enabled'() {
        expect:
        aggregateDataOptimisationsService.isCountsPerStudyAndConceptForPatientSetEnabled()
    }

    def 'test counts pers concept and study'() {
        def admin = usersResource.getUserFromUsername('admin')
        Set<String> allStudyids = studiesResource.studySet*.id as Set<String>

        QueryResult patientSetQueryResult = patientSetResource.createPatientSetQueryResult('All patients',
                new TrueConstraint(), admin, 'v2', false)

        when: 'count all subjects per concept and study'
        Map<String, Map<String, Counts>> counts = aggregateDataOptimisationsService
                .countsPerStudyAndConceptForPatientSet(patientSetQueryResult.id, admin)

        then: 'counts present for all studies in the system'
        (allStudyids - counts.keySet()).empty
        then: 'for the given study all concept are represented'
        counts['SURVEY1'].size() == 7
        then: 'patient count per study per concept is correct'
        counts['SURVEY1']['gender'].patientCount == 14
    }

    def 'test counts for studies user does not have access do not show up'() {
        def publicUser1 = usersResource.getUserFromUsername('test-public-user-1')
        Set<String> allStudyids = studiesResource.studySet*.id as Set<String>

        QueryResult patientSetQueryResult = patientSetResource.createPatientSetQueryResult(
                'All patients available for test-public-user-1',
                new TrueConstraint(), publicUser1, 'v2', false)

        when: 'count all subjects per concept and study'
        Map<String, Map<String, Counts>> counts = aggregateDataOptimisationsService
                .countsPerStudyAndConceptForPatientSet(patientSetQueryResult.id, publicUser1)

        then: 'there are counts'
        !counts.isEmpty()
        then: 'not for all studies'
        !(allStudyids - counts.keySet()).empty
        then: 'the private test study is not among the list'
        !('SHARED_CONCEPTS_STUDY_C_PRIV' in counts)
    }


}
