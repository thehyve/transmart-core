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

@Slf4j
@CompileStatic
class Studies {

    static final Table study_table = new Table('i2b2demodata', 'study')
    static final Table trial_visit_table = new Table('i2b2demodata', 'trial_visit_dimension')
    static final Table study_dimensions_table = new Table('i2b2metadata', 'study_dimension_descriptions')


    final Database database
    final Dimensions dimensions

    final LinkedHashMap<String, Class> study_columns
    final LinkedHashMap<String, Class> trial_visit_columns
    final LinkedHashMap<String, Class> study_dimensions_columns

    final Map<String, Long> studyIdToStudyNum = [:]
    final List<Long> indexToStudyNum = []
    final List<Long> indexToTrialVisitNum = []

    Studies(Database database, Dimensions dimensions) {
        this.database = database
        this.dimensions = dimensions
        this.study_columns = this.database.getColumnMetadata(study_table)
        this.trial_visit_columns = this.database.getColumnMetadata(trial_visit_table)
        this.study_dimensions_columns = this.database.getColumnMetadata(study_dimensions_table)
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
                "select * from ${study_table} where study_id = :studyId".toString(),
                [studyId: studyId],
                new StudyRowMapper()
        )
        if (studies.size() == 0) {
            return null
        } else {
            return studies[0]
        }
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
                "select trial_visit_num from ${trial_visit_table} where study_num = :studyNum".toString(),
                [studyNum: studyNum],
                trialVisitHandler
        )
        trialVisitHandler.trialVisitNums
    }

    void delete(String studyId) {
        def tx = database.beginTransaction()
        def study = findStudy(studyId)
        if (!study) {
            throw new IllegalStateException("Study not found: ${studyId}.")
        }
        log.info "Deleting observations for study ${studyId} ..."
        int observationCount = database.namedParameterJdbcTemplate.update(
                """delete from ${Observations.table} where trial_visit_num in 
                    (select trial_visit_num from ${trial_visit_table} where study_num = :studyNum)""".toString(),
                [studyNum: study.studyNum]
        )
        log.info "${observationCount} observations deleted."

        log.info "Deleting trial visits for study ${studyId} ..."
        int trialVisitCount = database.namedParameterJdbcTemplate.update(
                "delete from ${trial_visit_table} where study_num = :studyNum".toString(),
                [studyNum: study.studyNum]
        )
        log.info "${trialVisitCount} trial visits deleted."

        log.info "Deleting study dimensions for study ${studyId} ..."
        int studyDimensionCount = database.namedParameterJdbcTemplate.update(
                "delete from ${study_dimensions_table} where study_id = :studyNum".toString(),
                [studyNum: study.studyNum]
        )
        log.info "${studyDimensionCount} study dimensions deleted."

        log.info "Deleting tags for study ${studyId} ..."
        int tagCount = database.namedParameterJdbcTemplate.update(
                """delete from ${Tags.table} where path in 
                    (select c_fullname from ${TreeNodes.table} where sourcesystem_cd = :studyId)""".toString(),
                [studyId: studyId]
        )
        log.info "${tagCount} tags deleted."

        log.info "Deleting tree nodes for study ${studyId} ..."
        int treeNodeCount = database.namedParameterJdbcTemplate.update(
                "delete from ${TreeNodes.table} where sourcesystem_cd = :studyId".toString(),
                [studyId: studyId]
        )
        log.info "${treeNodeCount} tree nodes deleted."

        log.info "Deleting study object for study ${studyId} ..."
        database.namedParameterJdbcTemplate.update(
                "delete from ${study_table} where study_num = :studyNum".toString(),
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
        def studiesFile = new File(rootPath, study_table.fileName)
        studiesFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(study_table.fileName, data, study_columns)
                    return
                }
                def studyData = Util.asMap(study_columns, data)
                def studyIndex = studyData['study_num'] as long
                if (i != studyIndex + 1) {
                    throw new IllegalStateException("The studies ${study_table.fileName} are not in order. (Found ${studyIndex} on line ${i}.)")
                }
                def studyId = studyData['study_id'] as String
                checkIfStudyExists(studyId)
            }
        }
    }

    void clean() {
        def studiesFile = new File(study_table.fileName)
        studiesFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(study_table.fileName, data, study_columns)
                    return
                }
                def studyData = Util.asMap(study_columns, data)
                def studyId = studyData['study_id'] as String
                def study = findStudy(studyId)
                if (study) {
                    delete(studyId)
                }
            }
        }
    }

    void load(String rootPath) {
        def tx = database.beginTransaction()
        // Insert study records
        def studiesFile = new File(rootPath, study_table.fileName)
        studiesFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(study_table.fileName, data, study_columns)
                    return
                }
                try {
                    def studyData = Util.asMap(study_columns, data)
                    def studyId = studyData['study_id'] as String
                    def studyNum = database.insertEntry(study_table, study_columns, 'study_num', studyData)
                    log.info "Study ${studyId} inserted [study_num: ${studyNum}]."
                    indexToStudyNum.add(studyNum)
                    studyIdToStudyNum[studyId] = studyNum
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${study_table.fileName}: ${e.message}."
                    throw e
                }
            }
        }
        database.commit(tx)

        tx = database.beginTransaction()
        // Insert trial visits
        def trialVisitsFile = new File(rootPath, trial_visit_table.fileName)
        trialVisitsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(trial_visit_table.fileName, data, trial_visit_columns)
                    return
                }
                try {
                    def trialVisitData = Util.asMap(trial_visit_columns, data)
                    def studyIndex = trialVisitData['study_num'] as int
                    if (studyIndex >= indexToStudyNum.size()) {
                        throw new IllegalStateException("Invalid study index (${studyIndex}). Only ${indexToStudyNum.size()} studies found.")
                    }
                    def studyNum = indexToStudyNum[studyIndex]
                    trialVisitData['study_num'] = studyNum
                    def trialVisitNum = database.insertEntry(trial_visit_table, trial_visit_columns, 'trial_visit_num', trialVisitData)
                    log.info "Trial visit inserted [trial_visit_num: ${trialVisitNum}]."
                    indexToTrialVisitNum.add(trialVisitNum)
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${trial_visit_table.fileName}: ${e.message}."
                    throw e
                }
            }
        }

        // Insert study dimension descriptions
        def studyDimensionsFile = new File(rootPath, study_dimensions_table.fileName)
        studyDimensionsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            log.info "Reading study dimensions from file ..."
            def insertCount = 0
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(study_dimensions_table.fileName, data, study_dimensions_columns)
                    return
                }
                try {
                    def studyDimensionData = Util.asMap(study_dimensions_columns, data)
                    def studyIndex = studyDimensionData['study_id'] as int
                    if (studyIndex >= indexToStudyNum.size()) {
                        throw new IllegalStateException("Invalid study index (${studyIndex}). Only ${indexToStudyNum.size()} studies found.")
                    }
                    def studyNum = indexToStudyNum[studyIndex]
                    studyDimensionData['study_id'] = studyNum
                    def dimensionIndex = studyDimensionData['dimension_description_id'] as int
                    if (dimensionIndex >= dimensions.indexToDimensionId.size()) {
                        throw new IllegalStateException("Invalid dimension index (${dimensionIndex}). Only ${dimensions.indexToDimensionId.size()} dimensions found.")
                    }
                    def dimensionId = dimensions.indexToDimensionId[dimensionIndex]
                    studyDimensionData['dimension_description_id'] = dimensionId
                    database.insertEntry(study_dimensions_table, study_dimensions_columns, studyDimensionData)
                    insertCount++
                    log.debug "Study dimension inserted [study_id: ${studyNum}, dimension_description_id: ${dimensionId}]."
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${study_dimensions_table.fileName}: ${e.message}."
                    throw e
                }
            }
            log.info "${insertCount} study dimensions inserted."
        }
        database.commit(tx)
    }

}
