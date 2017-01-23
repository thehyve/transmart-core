package org.transmartproject.batch.highdim.platform

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables

import javax.annotation.Resource

/**
 * Inserts the information in the platformObject bean in the database.
 */
@Component
@JobScopeInterfaced
class InsertGplInfoTasklet implements Tasklet {

    @Resource
    Platform platformObject

    @Value(Tables.GPL_INFO)
    SimpleJdbcInsert jdbcInsert

    @Value('#{jobExecution.startTime}')
    Date jobStartTime

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int i = jdbcInsert.execute([
                platform       : platformObject.id,
                title          : platformObject.title,
                organism       : platformObject.organism,
                marker_type    : platformObject.markerType,
                genome_build   : platformObject.genomeRelease,
                annotation_date: jobStartTime,
        ])
        contribution.incrementWriteCount(i)

        RepeatStatus.FINISHED
    }
}
