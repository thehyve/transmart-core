/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.transmartproject.copy.Database
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Fetching and loading studies.
 */
@Slf4j
@CompileStatic
class Studies {

    static final Table STUDY_TABLE = new Table('i2b2demodata', 'study')
    static final Table TRIAL_VISIT_TABLE = new Table('i2b2demodata', 'trial_visit_dimension')
    static final Table STUDY_DIMENSIONS_TABLE = new Table('i2b2metadata', 'study_dimension_descriptions')


    final Database database
    final Dimensions dimensions

    final LinkedHashMap<String, Class> studyColumns
    final LinkedHashMap<String, Class> trialVisitColumns
    final LinkedHashMap<String, Class> studyDimensionsColumns

    final Map<String, Long> studyIdToStudyNum = [:]
    final Map<Long, Long> indexToStudyNum = [:]
    final Map<Long, Long> indexToTrialVisitNum = [:]

    Studies(Database database, Dimensions dimensions) {
        this.database = database
        this.dimensions = dimensions
        this.studyColumns = this.database.getColumnMetadata(STUDY_TABLE)
        this.trialVisitColumns = this.database.getColumnMetadata(TRIAL_VISIT_TABLE)
        this.studyDimensionsColumns = this.database.getColumnMetadata(STUDY_DIMENSIONS_TABLE)
    }

    @CompileStatic
    @Immutable
    static class Study {
        final String studyId
        final Long studyNum
        @Override
        String toString() {
            "[studyId: ${studyId}, studyNum: ${studyNum}]"
        }
    }

    @CompileStatic
    static class StudyRowMapper implements RowMapper<Study> {
        @Override
        Study mapRow(ResultSet rs, int rowNum) throws SQLException {
            new Study(
                studyNum: rs.getLong('study_num'),
                studyId: rs.getString('study_id')
            )
        }
    }

    Study findStudy(String studyId) {
        def studies = database.namedParameterJdbcTemplate.query(
                "select * from ${STUDY_TABLE} where study_id = :studyId".toString(),
                [studyId: studyId],
                new StudyRowMapper()
        )
        if (studies.size() == 0) {
            return null
        }
        studies[0]
    }

    @CompileStatic
    static class TrialVisitRowHandler implements RowCallbackHandler {
        final List<Long> trialVisitNums = []

        @Override
        void processRow(ResultSet rs) throws SQLException {
            trialVisitNums.add(rs.getLong('trial_visit_num'))
        }
    }

    List<Long> findTrialVisitNumsForStudy(Long studyNum) {
        def trialVisitHandler = new TrialVisitRowHandler()
        database.namedParameterJdbcTemplate.query(
                "select trial_visit_num from ${TRIAL_VISIT_TABLE} where study_num = :studyNum".toString(),
                [studyNum: studyNum],
                trialVisitHandler
        )
        trialVisitHandler.trialVisitNums
    }

    void removeObservationsForTrials(Set<Long> trialVisitNums) {
        if (!trialVisitNums) {
            return
        }
        Set<Table> posibleCandidateTablesToDrop = trialVisitNums.collect { Observations.getChildTable(it) } as Set
        Set<Table> childTables = database.getChildTables(Observations.TABLE)
        for (Table dropTableCandidate : posibleCandidateTablesToDrop) {
            if (dropTableCandidate in childTables) {
                database.dropTable(dropTableCandidate)
                log.info "${dropTableCandidate} child table has been dropped."
            }
        }
        int observationCount = database.namedParameterJdbcTemplate.update(
                """delete from ${Observations.TABLE} where trial_visit_num in 
                (:trialVisitNums)""".toString(),
                [trialVisitNums: trialVisitNums]
        )
        log.info "${observationCount} observations deleted from ${Observations.TABLE}."
    }

    void deleteById(String studyId, boolean failOnNoStudy = true) {
        def tx = database.beginTransaction()
        def study = findStudy(studyId)
        if (!study) {
            if (failOnNoStudy) {
                throw new IllegalStateException("Study not found: ${studyId}.")
            } else {
                log.info("No ${studyId} found.")
                return
            }
        }
        log.info "Deleting observations for study ${studyId} ..."
        List<Long> trialVisitNums = findTrialVisitNumsForStudy(study.studyNum)
        removeObservationsForTrials(trialVisitNums as Set)

        log.info "Deleting trial visits for study ${studyId} ..."
        int trialVisitCount = database.namedParameterJdbcTemplate.update(
                "delete from ${TRIAL_VISIT_TABLE} where study_num = :studyNum".toString(),
                [studyNum: study.studyNum]
        )
        log.info "${trialVisitCount} trial visits deleted."

        log.info "Deleting study dimensions for study ${studyId} ..."
        int studyDimensionCount = database.namedParameterJdbcTemplate.update(
                "delete from ${STUDY_DIMENSIONS_TABLE} where study_id = :studyNum".toString(),
                [studyNum: study.studyNum]
        )
        log.info "${studyDimensionCount} study dimensions deleted."

        log.info "Deleting tags for study ${studyId} ..."
        int tagCount = database.namedParameterJdbcTemplate.update(
                """delete from ${Tags.TABLE} where path in 
                    (select c_fullname from ${TreeNodes.TABLE} where sourcesystem_cd = :studyId)""".toString(),
                [studyId: studyId]
        )
        log.info "${tagCount} tags deleted."

        log.info "Deleting tree nodes for study ${studyId} ..."
        int treeNodeCount = database.namedParameterJdbcTemplate.update(
                "delete from ${TreeNodes.TABLE} where sourcesystem_cd = :studyId".toString(),
                [studyId: studyId]
        )
        log.info "${treeNodeCount} tree nodes deleted."

        log.info "Deleting study object for study ${studyId} ..."
        database.namedParameterJdbcTemplate.update(
                "delete from ${STUDY_TABLE} where study_num = :studyNum".toString(),
                [studyNum: study.studyNum]
        )
        log.info "Study object deleted."
        database.commit(tx)
    }

    void checkIfStudyExists(String studyId) {
        def study = findStudy(studyId)
        if (study) {
            log.error "Found existing study: ${study}."
            log.error "You can delete the study and associated data with: `transmart-copy --delete ${studyId}`."
            throw new IllegalStateException("Study already exists: ${studyId}.")
        }
        log.info "Study ${studyId} does not exists in the database yet."
    }

    void check(String rootPath) {
        LinkedHashMap<String, Class> header = studyColumns
        def studiesFile = new File(rootPath, STUDY_TABLE.fileName)
        studiesFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(STUDY_TABLE.fileName, data, studyColumns)
                    return
                }
                def studyData = Util.asMap(header, data)
                def studyId = studyData['study_id'] as String
                checkIfStudyExists(studyId)
            }
        }
    }

    void delete(String rootPath, boolean failOnNoStudy = true) {
        LinkedHashMap<String, Class> header = studyColumns
        def studiesFile = new File(rootPath, STUDY_TABLE.fileName)
        studiesFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(STUDY_TABLE.fileName, data, studyColumns)
                    return
                }
                def studyData = Util.asMap(header, data)
                def studyId = studyData['study_id'] as String
                deleteById(studyId, failOnNoStudy)
            }
        }
    }

    private void loadStudies(String rootPath) {
        LinkedHashMap<String, Class> studyHeader = studyColumns
        // Insert study records
        def studiesFile = new File(rootPath, STUDY_TABLE.fileName)
        studiesFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    studyHeader = Util.verifyHeader(STUDY_TABLE.fileName, data, studyColumns)
                    return
                }
                try {
                    def studyData = Util.asMap(studyHeader, data)
                    Long studyIndex = studyData['study_num'] as Long
                    def studyId = studyData['study_id'] as String
                    def studyNum = database.insertEntry(STUDY_TABLE, studyHeader, 'study_num', studyData)
                    log.info "Study ${studyId} inserted [study_num: ${studyNum}]."
                    indexToStudyNum.put(studyIndex, studyNum)
                    studyIdToStudyNum[studyId] = studyNum
                } catch(Throwable e) {
                    log.error "Error on line ${i} of ${STUDY_TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
        }
    }

    private void loadTrialVisits(String rootPath) {
        LinkedHashMap<String, Class> trialVisitHeader = trialVisitColumns
        // Insert trial visits
        def trialVisitsFile = new File(rootPath, TRIAL_VISIT_TABLE.fileName)
        trialVisitsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    trialVisitHeader = Util.verifyHeader(TRIAL_VISIT_TABLE.fileName, data, trialVisitColumns)
                    return
                }
                try {
                    def trialVisitData = Util.asMap(trialVisitHeader, data)
                    Long studyIndex = trialVisitData['study_num'] as Long
                    Long studyNum = indexToStudyNum[studyIndex]
                    trialVisitData['study_num'] = studyNum
                    Long trialVisitIndex = trialVisitData['trial_visit_num'] as Long
                    def trialVisitNum = database.insertEntry(
                            TRIAL_VISIT_TABLE, trialVisitHeader, 'trial_visit_num', trialVisitData)
                    log.info "Trial visit inserted [trial_visit_num: ${trialVisitNum}]."
                    indexToTrialVisitNum.put(trialVisitIndex, trialVisitNum)
                } catch(Throwable e) {
                    log.error "Error on line ${i} of ${TRIAL_VISIT_TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
        }
    }

    private void loadStudyDimensions(String rootPath) {
        LinkedHashMap<String, Class> studyDimensionsHeader = studyDimensionsColumns
        // Insert study dimension descriptions
        def studyDimensionsFile = new File(rootPath, STUDY_DIMENSIONS_TABLE.fileName)
        studyDimensionsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            log.info "Reading study dimensions from file ..."
            def insertCount = 0
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    studyDimensionsHeader =
                            Util.verifyHeader(STUDY_DIMENSIONS_TABLE.fileName, data, studyDimensionsColumns)
                    return
                }
                try {
                    def studyDimensionData = Util.asMap(studyDimensionsHeader, data)
                    Long studyIndex = studyDimensionData['study_id'] as Long
                    def studyNum = indexToStudyNum[studyIndex]
                    studyDimensionData['study_id'] = studyNum
                    Long dimensionIndex = studyDimensionData['dimension_description_id'] as Long
                    def dimensionId = dimensions.indexToDimensionId[dimensionIndex]
                    if (dimensionId == null) {
                        throw new IllegalStateException("Dimension with id = ${dimensionIndex} does not exist.")
                    }
                    studyDimensionData['dimension_description_id'] = dimensionId
                    database.insertEntry(STUDY_DIMENSIONS_TABLE, studyDimensionsHeader, studyDimensionData)
                    insertCount++
                    log.debug "Study dimension inserted [study_id: ${studyNum}, " +
                            "dimension_description_id: ${dimensionId}]."
                } catch(Throwable e) {
                    log.error "Error on line ${i} of ${STUDY_DIMENSIONS_TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
            log.info "${insertCount} study dimensions inserted."
        }
    }

    void load(String rootPath) {
        def tx = database.beginTransaction()
        loadStudies(rootPath)
        database.commit(tx)

        tx = database.beginTransaction()
        loadTrialVisits(rootPath)
        loadStudyDimensions(rootPath)
        database.commit(tx)
    }

}
