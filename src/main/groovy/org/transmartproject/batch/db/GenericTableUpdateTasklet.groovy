package org.transmartproject.batch.db

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter

/**
 * Generic update of a table that is somehow related with a study.
 */
abstract class GenericTableUpdateTasklet implements Tasklet, PreparedStatementSetter {

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int count = jdbcTemplate.update(sql, this)
        contribution.incrementWriteCount(count)
        RepeatStatus.FINISHED
    }

    abstract String getSql()

}
