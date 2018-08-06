/* (c) Copyright 2018  The Hyve B.V. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

import java.sql.Connection
import java.util.function.Function
import java.util.stream.Collectors

@CompileStatic
class AggregateDataOptimisationsService {

    @Autowired
    MDStudiesResource studiesResource

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
        dbFunctionExists('public', 'pg_bitcount')
    }

    /**
     * Counts the number of patients in the intersection of a number of patient sets.
     * Uses the bitset representation of the biomart_user.patient_set_bitset view.
     *
     * Can only be used if {@link #isCountPatientSetsIntersectionEnabled} returns true.
     *
     * Uses the public.pg_bitcount function if available, or a slower alternative otherwise.
     *
     * @param rowSubjectSets the rows patient sets.
     * @param columnSubjectSets the columns patient sets.
     * @param subjectSet the cell patient set.
     * @return the numbers of patients that occur in every set.
     */
    List<List<Long>> countPatientSetsIntersection(List<QueryResult> rowSubjectSets, List<QueryResult> columnSubjectSets, QueryResult subjectSet, User user) {
        if (rowSubjectSets.empty || columnSubjectSets.empty || !subjectSet) {
            log.debug('Not enough patient sets to calculate intersection. Hence return empty list.')
            return []
        }
        List<Long> rowSubjectSetIds = rowSubjectSets*.id
        List<Long> columnSubjectSetIds = columnSubjectSets*.id
        log.info "Start counting the intersection between patient sets using bit sets ..."
        long t1 = System.currentTimeMillis()
        List<Map<String, Object>> dbResults = namedParameterJdbcTemplate.queryForList(
                """with 
                    row_bitset as (select * from patient_set_bitset where result_instance_id in (:row_set_ids)),
                    column_bitset as (select * from patient_set_bitset where result_instance_id in (:column_set_ids)),
                    cell_bitset as (select * from patient_set_bitset where result_instance_id = :cell_set_id)
                    select 
                      row_bitset.result_instance_id as row_set_id,
                      column_bitset.result_instance_id as column_set_id,
                      ${bitcountDbFunction('row_bitset.patient_set & column_bitset.patient_set & cell_bitset.patient_set')} as subject_count
                    from row_bitset, column_bitset, cell_bitset;""",
                [
                        row_set_ids: rowSubjectSetIds,
                        column_set_ids: columnSubjectSetIds,
                        cell_set_id: subjectSet.id
                ])
        log.info "Counting the intersection between patient sets done. (took ${System.currentTimeMillis() - t1} ms.)"
        return buildCountsTable(dbResults, rowSubjectSetIds, columnSubjectSetIds)
    }

    private List<List<Long>> buildCountsTable(List<Map<String, Object>> dbResults, List<Long> rowSubjectSetIds, List<Long> columnSubjectSetIds) {
        log.debug('Creating hash table out of the database result.')
        Map<List<Long>, Long> columnRowIdsToSubjectCount = dbResults.stream().collect(
                Collectors.toMap(
                        { Map<String, Object> dbResultRow -> [(Long) dbResultRow.get('row_set_id'), (Long) dbResultRow.get('column_set_id')] } as Function<Map<String, Object>, List<Long>>,
                        { Map<String, Object> dbResultRow -> (Long) dbResultRow.get('subject_count') } as Function<Map<String, Object>, Long>))

        List<List<Long>> result = []
        log.debug('Building result table.')
        for (Long rowSubjectSetId : rowSubjectSetIds) {
            List<Long> rowCounts = []
            for (Long columnSubjectSetId : columnSubjectSetIds) {
                Long subjectCount = columnRowIdsToSubjectCount.get([rowSubjectSetId, columnSubjectSetId])
                rowCounts.add(subjectCount ?: 0L)
            }
            result.add(rowCounts)
        }
        return result
    }

    private String bitcountDbFunction(String arguments) {
        if (bitCountFunctionAvailable) {
            "public.pg_bitcount(${arguments})"
        } else {
            "length(replace((${arguments})::text, '0', ''))"
        }
    }

    private Connection getConnection() {
        ((JdbcTemplate) namedParameterJdbcTemplate.jdbcOperations).dataSource.connection
    }

    private boolean dbViewExists(String schema, String view) {
        getConnection().metaData.getTables(null, schema, view, ['VIEW'].toArray(new String[0])).next()
    }

    private boolean dbFunctionExists(String schema, String function) {
        getConnection().metaData.getFunctions(null, schema, function).next()
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


