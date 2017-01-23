package org.transmartproject.batch.db

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Partitions a table, if necessary, and fills a job context parameter with the
 * name of the partition table.
 */
@Slf4j
abstract class AbstractPartitionTasklet implements Tasklet {

    public final static String PARTITION_TABLE_NAME = 'partitionTableName'

    /**
     * The table to partition, qualified with schema.
     */
    String tableName

    /**
     * The value of the partition column.
     */
    String partitionByColumnValue

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate

    protected String getUnqualifiedTable() {
        Tables.tableName(tableName)
    }

    protected String getSchema() {
        Tables.schemaName(tableName)
    }

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {

        String partitionTableName = createPartitionIfNeeded()
        savePartitionTableName(chunkContext, partitionTableName)

        RepeatStatus.FINISHED
    }

    protected abstract String createPartitionIfNeeded()

    protected void savePartitionTableName(ChunkContext context, String partitionTableName) {
        log.info "Will use partition table $partitionTableName"
        ExecutionContext jobExecutionContext =
                context.stepContext.stepExecution.jobExecution.executionContext

        jobExecutionContext.putString(PARTITION_TABLE_NAME, partitionTableName)
    }
}
