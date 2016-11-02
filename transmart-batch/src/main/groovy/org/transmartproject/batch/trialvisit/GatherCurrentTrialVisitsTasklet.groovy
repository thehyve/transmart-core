package org.transmartproject.batch.trialvisit

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.secureobject.Study

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Gets the current trial visits from database.
 */
@Component
@Slf4j
class GatherCurrentTrialVisitsTasklet implements Tasklet, RowMapper<TrialVisit> {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    Study study

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!study.studyNum) {
            log.info("No study with id ${study.studyId} found. Skipping gathering visit trials.")
            return RepeatStatus.FINISHED
        }

        List<TrialVisit> trialVisits = jdbcTemplate.query(
                """
                    SELECT
                        trial_visit_num,
                        study_num,
                        rel_time_label,
                        rel_time_unit_cd,
                        rel_time_num
                    FROM ${Tables.TRIAL_VISIT_DIMENSION} WHERE study_num = :study_num
                """,
                [
                        study_num: study.studyNum
                ],
                this)

        trialVisits.each {
            contribution.incrementWriteSkipCount()
            study << it
        }

        RepeatStatus.FINISHED
    }

    @Override
    TrialVisit mapRow(ResultSet rs, int rowNum) throws SQLException {
        new TrialVisit(
                id: rs.getLong('trial_visit_num'),
                studyNum: rs.getLong('study_num'),
                relTimeLabel: rs.getString('rel_time_label'),
                relTimeUnit: rs.getString('rel_time_unit_cd'),
                relTime: rs.getInt('rel_time_num')
        )
    }
}
