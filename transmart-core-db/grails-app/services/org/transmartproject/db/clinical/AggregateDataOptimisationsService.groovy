/* (c) Copyright 2018  The Hyve B.V. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.Memoized
import org.hibernate.SessionFactory
import org.hibernate.internal.StatelessSessionImpl
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.User

import java.util.stream.Collectors

import static org.transmartproject.db.clinical.AggregateDataService.*

class AggregateDataOptimisationsService {

    @Autowired
    MDStudiesResource studiesResource

    @Autowired
    MultidimensionalDataResourceService multidimensionalDataResourceService

    @Autowired
    SystemResource systemResource

    @Autowired
    SessionFactory sessionFactory

    /**
     * Checks if the summary view is available and can be used for concept counts per study.
     * @return
     */
    @Memoized
    boolean isCountsPerConceptForStudyEnabled() {
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            def connection = session.connection()

            log.debug "Check if view i2b2demodata.observation_fact_summary exists ..."
            def resultSet = connection.metaData.getTables(null, 'i2b2demodata', 'observation_fact_summary', null)
            resultSet.next()
        } finally {
            session.close()
        }
    }

    @CompileStatic
    Map<String, Counts> countsPerConceptForStudyAndPatientSet(String studyId, PatientSetConstraint constraint, User user) {
        def id = studiesResource.getStudyByStudyId(studyId)?.id
        def resultInstanceId = constraint.patientSetId
        def patientSet = multidimensionalDataResourceService.findQueryResult(resultInstanceId, user)
        long size = patientSet.setSize
        int chunkSize = systemResource.runtimeConfig.patientSetChunkSize
        int numTasks = Math.ceil((float)(size / chunkSize)).intValue()

        log.info "Counts per concept for study ${studyId} (${id}) and patient set ${resultInstanceId} (${size} subjects) ..."
        def t1 = new Date()
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            def statement = session.connection().prepareStatement(
                    '''select concept_cd, count(distinct(patient_num)) as patient_count, sum(observation_count) as observation_count
                         from i2b2demodata.observation_fact_summary
                         where patient_num in (
                             select patient_num
                             from i2b2demodata.qt_patient_set_collection
                             where result_instance_id = ?
                             order by patient_num
                             offset ? limit ?)
                         and study_num = ?
                         group by concept_cd''')

            List<ConceptCountRow> result = []
            for (int i = 1; i <= numTasks; i++) {
                def start = new Date()
                int offset = chunkSize * (i - 1)
                statement.setLong(1, resultInstanceId)
                statement.setInt(2, offset)
                statement.setInt(3, chunkSize)
                statement.setLong(4, id)
                def rs = statement.executeQuery()
                while (rs.next()) {
                    result.add(new ConceptCountRow(rs.getString('concept_cd'),
                            new Counts(rs.getLong('observation_count'), rs.getLong('patient_count'))))
                }
                log.info "Counts per concept subtask ${i} done. (took ${new Date().time - start.time} ms.)"
            }
            def t2 = new Date()
            log.info "Counts per concept done. (took ${t2.time - t1.time} ms.)"
            mergeConceptCounts(result)
        } finally {
            session.close()
        }
    }

    @Immutable
    @CompileStatic
    static class Bounds {
        int min
        int max
    }

    /**
     * Minimum patient num needs to be the same as the one used for creating the view,
     * for determining the offset used for the bit sets.
     */
    @Memoized
    Bounds getPatientNumBounds() {
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            def statement = session.connection().prepareStatement(
                    'select min_patient_num, max_patient_num from i2b2demodata.patient_num_bounds;')
            def rs = statement.executeQuery()
            rs.next()
            new Bounds(rs.getInt('min_patient_num'), rs.getInt('max_patient_num'))
        } finally {
            session.close()
        }
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

    /**
     * Checks if the bitset view is available and can be used for counts per study and concept.
     * @return
     */
    @Memoized
    boolean isCountsPerStudyAndConceptForPatientSetEnabled() {
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            def connection = session.connection()

            log.info "Check if view i2b2demodata.study_concept_sets exists ..."
            def resultSet = connection.metaData.getTables(null, 'i2b2demodata', 'study_concept_sets', null)
            if (resultSet.next()) {
                log.info "Check if view i2b2demodata.patient_num_bounds exists ..."
                def rs = connection.metaData.getTables(null, 'i2b2demodata', 'patient_num_bounds', null)
                def result = rs.next()
                if (!result) {
                    log.info "View i2b2demodata.patient_num_bounds not found."
                }
                return result
            }
            log.info "View i2b2demodata.study_concept_sets not found."
            return false
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
     * Implementation using bit sets. Only returns patient counts, not observation counts.
     * @param constraint
     * @param user
     * @return
     */
    @CompileStatic
    Map<String, Map<String, Counts>> countsPerStudyAndConceptForPatientSet(PatientSetConstraint constraint, User user) {
        def resultInstanceId = constraint.patientSetId
        def patientSet = multidimensionalDataResourceService.findQueryResult(resultInstanceId, user)
        long size = patientSet.setSize
        log.info "Counts per study and concept for patient set ${resultInstanceId} (${size} subjects) using bitsets ..."

        Set<String> studyIds = studiesResource.getStudies(user).stream()
                .map({MDStudy study -> study.name})
                .collect(Collectors.toSet())

        def t1 = new Date()
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false

            // Drop temporary table if it exists
            session.connection().prepareStatement('drop table if exists patient_set_bitset;').execute()

            def bounds = patientNumBounds // minimum patient num needs to be the same as the one used for creating the view
            log.info "Create temporary table for patient set ${resultInstanceId}. Patient nums: min=${bounds.min}, max=${bounds.max}"

            int number_of_bits = bounds.max + 1 - bounds.min
            def tempTableStatement = session.connection().prepareStatement(
                    """create temporary table patient_set_bitset as
                          select
                                collection.result_instance_id as result_instance_id,
                                (bit_or(1::bit(${number_of_bits}) << (collection.patient_num - bounds.min_patient_num)::integer)) as patient_set
                          from i2b2demodata.qt_patient_set_collection collection, i2b2demodata.patient_num_bounds bounds 
                          where collection.result_instance_id = ?
                          group by collection.result_instance_id;""")
            tempTableStatement.setLong(1, resultInstanceId)
            tempTableStatement.execute()
            session.connection().commit()

            def t2 = new Date()
            log.info "Temporary table created. (${t2.time - t1.time} ms.)"

            log.info "Start querying counts per study and concept using bit sets ..."
            def t3 = new Date()

            def statement = session.connection().prepareStatement(
                    """select 
                              scs.study_num as study_num,
                              scs.concept_cd as concept_cd,
                              ${bitcount_function('scs.patient_set_bits & psb.patient_set')} as patient_count
                          from i2b2demodata.study_concept_sets scs, patient_set_bitset psb
                          where psb.result_instance_id = ?;""")

            List<ConceptStudyCountRow> result = []
            statement.setLong(1, resultInstanceId)
            def rs = statement.executeQuery()

            def t4 = new Date()
            log.info "Processing results ... (query took ${t4.time - t3.time} ms.)"

            while (rs.next()) {
                def row = new ConceptStudyCountRow(rs.getString('concept_cd'),
                        studiesResource.getStudyIdById(rs.getLong('study_num')),
                        new Counts(-1L, rs.getLong('patient_count')))
                if (studyIds.contains(row.studyId)) {
                    result.add(row)
                }
            }
            def t5 = new Date()
            log.info "Counts per study and concept for patient set done. (took ${t5.time - t1.time} ms.)"
            mergeSummaryTaskResults(result)
        } finally {
            session.close()
        }
    }

}
