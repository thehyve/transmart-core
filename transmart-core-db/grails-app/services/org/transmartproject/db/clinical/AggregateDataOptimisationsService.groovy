/* (c) Copyright 2018  The Hyve B.V. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.hibernate.SessionFactory
import org.hibernate.internal.StatelessSessionImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

import java.sql.Connection
import java.util.stream.Collectors

@CompileStatic
class AggregateDataOptimisationsService {

    @Autowired
    MDStudiesResource studiesResource

    @Autowired
    MultidimensionalDataResourceService multidimensionalDataResourceService

    @Autowired
    SystemResource systemResource

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate

    /**
     * Checks if the bitset view is available and can be used for counts per study and concept.
     * @return
     */
    @Memoized
    boolean isCountsPerStudyAndConceptForPatientSetEnabled() {
        dbViewExists('biomart_user', 'study_concept_patient_set_bitset')
    }

    /**
     * Refreshes biomart_user.study_concept_bitset
     */
    void clearPatientSetBitset() {
        refreshMaterializedView('biomart_user', 'study_concept_bitset')
    }

    /**
     * Computes patient counts per study and concept using bit sets.
     * Returns a map from study id to the map from concept code to
     * a counts object that only containts a patient count, not an observation count.
     *
     * @param resultInstanceId the id of the patient set to compute the counts for.
     * @param user the user to compute the counts for, used to filter studies on.
     * @return the map from study id to a map from concept code to counts.
     */
    Map<String, Map<String, Counts>> countsPerStudyAndConceptForPatientSet(Long resultInstanceId, User user) {
        log.info "Counts per study and concept for patient set ${resultInstanceId} using bitsets ..."

        Set<String> studyIds = studiesResource.getStudies(user, PatientDataAccessLevel.SUMMARY).stream()
                .map({ MDStudy study -> study.name })
                .collect(Collectors.toSet())

        def t1 = new Date()

        List<Map<String, Object>> rows = namedParameterJdbcTemplate
                .queryForList(
                '''select 
                    study_id,
                    concept_cd,
                    patient_count
                   from biomart_user.study_concept_patient_set_bitset
                   where result_instance_id = :result_instance_id and study_id in (:study_ids)''',
                [result_instance_id: resultInstanceId, study_ids: studyIds])

        Map<String, Map<String, Counts>> result = [:]
        for (Map<String, Object> row : rows) {
            String studyId = row.get('study_id')
            Map<String, Counts> countsPerConcept = result.get(studyId)
            if (!countsPerConcept) {
                countsPerConcept = [:]
                result.put(studyId, countsPerConcept)
            }
            countsPerConcept.put((String) row.get('concept_cd'), new Counts(-1L, (long) row.get('patient_count')))
        }
        def t2 = new Date()
        log.info "Counts per study and concept for patient set done. (took ${t2.time - t1.time} ms.)"
        return result
    }

    /**
     * Checks if the bitset view for patient sets is available and can be used for computing
     * patient counts for patient sets.
     * @return
     */
    @Memoized
    boolean isCountPatientSetsIntersectionEnabled() {
        dbViewExists('biomart_user', 'patient_set_bitset')
    }

    /**
     * For using efficient bit counting, install the
     * <a href="https://github.com/thehyve/pg_bitcount">pg_bitcount</a> extension.
     */
    @Memoized
    boolean isBitCountFunctionAvailable() {
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            def connection = session.connection()
            def rs = connection.metaData.getFunctions(null, 'public', 'pg_bitcount')
            def result = rs.next()
            if (!result) {
                log.info "Function public.pg_bitcount not found."
            }
            return result
        } finally {
            session.close()
        }
    }

    String bitcount_function(String arguments) {
        if (bitCountFunctionAvailable) {
            "public.pg_bitcount(${arguments})"
        } else {
            "length(replace((${arguments})::text, '0', ''))"
        }
    }

    /**
     * Counts the number of patients in the intersection of a number of patient sets.
     * Uses the bitset representation of the biomart_user.patient_set_bitset view.
     *
     * Can only be used if {@link #isCountPatientSetsIntersectionEnabled} returns true.
     *
     * Uses the public.pg_bitcount function if available, or a slower alternative otherwise.
     *
     * @param patientSets the patient sets to compute the intersection for.
     * @return the number of patients that occur in every set.
     */
    Long countPatientSetsIntersection(Collection<QueryResult> patientSets) {
        if (patientSets.empty) {
            return 0L
        }
        Long result = null
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            log.info "Start counting the intersection between patient sets using bit sets ..."
            def t1 = new Date()
            def parameterTemplate = (['?'] * patientSets.size()).join(', ')
            def statement = session.connection().prepareStatement(
                    """select ${bitcount_function('bit_and(sets.patient_set)')} as patient_count
                         from (
                             select *
                             from biomart_user.patient_set_bitset
                             where result_instance_id in (${parameterTemplate})) sets;""")
            int i = 1
            for (QueryResult queryResult: patientSets) {
                statement.setLong(i, queryResult.id)
                i++
            }
            def rs = statement.executeQuery()
            if (rs.next()) {
                result = rs.getLong('patient_count')
            }
            def t2 = new Date()
            log.info "Counting the intersection between patient sets done. (took ${t2.time - t1.time} ms.)"
        } finally {
            session.close()
        }
        result
    }


    private Connection getConnection() {
        ((JdbcTemplate) namedParameterJdbcTemplate.jdbcOperations).dataSource.connection
    }

    private boolean dbViewExists(String schema, String function) {
        getConnection().metaData.getTables(null, schema, function, ['VIEW'].toArray(new String[0])).next()
    }

    private void refreshMaterializedView(String schema, String function) {
        def connection = getConnection()
        try {
            log.info "Refreshing $schema.$function materialized view ..."
            getConnection().prepareStatement("refresh materialized VIEW $schema.$function").executeUpdate()
        } catch (Exception e) {
            log.warn "$schema.$function materialized view not updated. $e.message"
        } finally {
            connection?.close()
        }
    }
}


