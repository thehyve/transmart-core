package org.transmartproject.batch.tasklet

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Generic tasklet to delete the contents of one table
 */
class DeleteTableTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    String table

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String sql = "delete from $table"
        int count = jdbcTemplate.update(sql)
        contribution.incrementWriteCount(count)

        RepeatStatus.FINISHED
    }
}
