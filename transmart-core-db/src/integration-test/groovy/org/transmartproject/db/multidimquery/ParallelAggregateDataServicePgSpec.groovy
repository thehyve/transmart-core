/* (c) Copyright 2018  The Hyve B.V. */

package org.transmartproject.db.multidimquery

import grails.testing.mixin.integration.Integration
import grails.util.Holders
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.RuntimeConfigRepresentation
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.clinical.AggregateDataService
import spock.lang.Specification

@Integration
class ParallelAggregateDataServicePgSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource

    @Autowired
    AggregateDataService aggregateDataService

    @Autowired
    SystemResource systemResource

    @Autowired
    UsersResource usersResource

    @Autowired
    PatientSetResource patientSetResource

    /**
     * Test the parallel implementation for counting patients and observations grouped by
     * concept and study.
     */
    void 'test parallel counts per study, concept'() {
        def user = usersResource.getUserFromUsername('test-public-user-1')
        QueryResult patientSet = patientSetResource.createPatientSetQueryResult(
                'test', new TrueConstraint(), user, 'v2', true)

        Constraint patientSetConstraint = new PatientSetConstraint(patientSetId: patientSet.id)

        when: 'fetching all counts per study and concept for the patient set containing all patients with 4 workers'
        Holders.config['org.transmartproject.system.numberOfWorkers'] = 4
        systemResource.updateRuntimeConfig(new RuntimeConfigRepresentation(4, 10))
        def countsPerStudyAndConcept = aggregateDataService.parallelCountsPerStudyAndConcept(patientSetConstraint, user)
        def counts = aggregateDataService.counts(new TrueConstraint(), user)
        def patientSetCounts = aggregateDataService.counts(patientSetConstraint, user)

        then: "the result should match the counts for the patient set"
        counts.patientCount == patientSet.setSize
        counts == patientSetCounts
        patientSet.setSize > 0
        !countsPerStudyAndConcept.keySet().empty
        'EHR' in countsPerStudyAndConcept.keySet()
        !countsPerStudyAndConcept['EHR'].keySet().empty
        countsPerStudyAndConcept['EHR']['EHR:VSIGN:HR'].patientCount == 3
        countsPerStudyAndConcept['EHR']['EHR:VSIGN:HR'].observationCount == 9

        then: "the total observation count should be equal to the sum of observation counts of the returned map"
        counts.observationCount == (long)countsPerStudyAndConcept.values().sum { Map<String, Counts> countsMap ->
            countsMap.values().sum { Counts c -> c.observationCount }
        }

        when: "fetching the counts with 1 worker"
        systemResource.updateRuntimeConfig(new RuntimeConfigRepresentation(1, 1000))
        def countsPerStudyAndConcept2 = aggregateDataService.parallelCountsPerStudyAndConcept(patientSetConstraint, user)

        then: "gives the same results"
        countsPerStudyAndConcept2
        countsPerStudyAndConcept2 == countsPerStudyAndConcept
    }

}
