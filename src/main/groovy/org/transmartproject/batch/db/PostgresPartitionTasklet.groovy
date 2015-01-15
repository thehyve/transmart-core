package org.transmartproject.batch.db

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.UnexpectedJobExecutionException
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

import java.util.regex.Pattern

/**
 * Partitions a table, if necessary, and fills a job context parameter with the
 * number of the partition.
 */
@Slf4j
class PostgresPartitionTasklet implements Tasklet {

    public final static String PARTITION_ID_JOB_CONTEXT_KEY = 'partitionId'

    /**
     * The table to partition, qualified with schema.
     */
    String tableName

    /**
     * The column to partition on.
     */
    String partitionByColumn

    /**
     * The value of the partition column.
     */
    String partitionByColumnValue

    /**
     * The column where to store the partition id.
     */
    String partitionIdColumn = 'partition_id'

    /**
     * The sequence from which to extract the partition id.
     */
    String sequence

    /**
     * The indexes to create on new partitions. List of lists of column names.
     */
    List<List<String>> indexes

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    private SequenceReserver sequenceReserver

    private String getCommentKey() {
        "$partitionByColumn = $partitionByColumnValue"
    }

    private String getUnqualifiedTable() {
        tableName.replaceFirst(/.+\./, '')
    }

    private String getSchema() {
        (tableName =~ /[^.]+/)[0]
    }

    private String currentPartition() {
        def q = '''
            SELECT C.relname
            FROM
                pg_catalog.pg_class C
                INNER JOIN pg_catalog.pg_namespace N ON (C.relnamespace = N.oid)
                INNER JOIN pg_catalog.pg_inherits I ON (C.oid = I.inhrelid)
            WHERE
                C.relkind = 'r'
                AND N.nspname = :schema
                AND obj_description(C.oid) = :comment_key
                AND I.inhparent = (
                    SELECT C2.oid
                    FROM
                        pg_catalog.pg_class C2
                        INNER JOIN  pg_catalog.pg_namespace N2 ON (C2.relnamespace = N2.oid)
                        WHERE
                            C2.relname = :unqualified_table
                            AND N2.nspname = :schema)'''

        List<String> result = jdbcTemplate.queryForList(q,
                [
                        comment_key: commentKey,
                        unqualified_table: unqualifiedTable,
                        schema: schema,
                ],
                String)

        if (result.size() == 0) {
            log.info("No partition of $tableName found for " +
                    "$partitionByColumn = $partitionByColumnValue")
            null
        } else if (result.size() == 1) {
            log.info("Found partition table: ${result[0]}")
            "$schema.${result[0]}"
        } else {
            throw new IncorrectResultSizeDataAccessException(
                    "Expected to get at most one result")
        }

    }

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {

        String partitionTable = currentPartition() // qualified
        int partitionId

        if (!partitionTable) {
            partitionId = sequenceReserver.getNext(sequence)
            partitionTable = createPartition(partitionId)
        } else {
            def matcher = partitionTable =~ "(?<=${Pattern.quote(tableName)}_)\\d+\\z"
            if (!matcher) {
                throw new UnexpectedJobExecutionException(
                        "Got a partition table name from which a partition " +
                                "id could not be extracted")
            }
            partitionId = matcher[0] as int
        }

        savePartitionId chunkContext, partitionId

        RepeatStatus.FINISHED
    }

    private String createPartition(int partitionId) {
        String partitionTable = "${tableName}_${partitionId}"

        log.info "Creating table $partitionTable"
        jdbcTemplate.update """
                CREATE TABLE $partitionTable(
                    CHECK ($partitionByColumn = '$partitionByColumnValue')
                ) INHERITS ($tableName)""",
                [partitionByColumnValue: partitionByColumnValue]

        jdbcTemplate.update("COMMENT ON TABLE $partitionTable IS " +
                "'${commentKey.replaceAll("'", "''")}'", [:])

        indexes.eachWithIndex { List<String> columns, i ->
            def indexName = "${partitionTable.replaceFirst(/.+\./, '')}_${i + 1}"
            log.info "Creating index $indexName on columns $columns"

            jdbcTemplate.update """
                CREATE INDEX $indexName ON $partitionTable(
                    ${columns.join(', ')})
            """, [:]
        }

        partitionTable
    }

    void savePartitionId(ChunkContext context, Integer partitionId) {
        log.info "Will use partition id $partitionId"
        ExecutionContext jobExecutionContext =
                context.stepContext.stepExecution.jobExecution.executionContext

        jobExecutionContext.putInt(PARTITION_ID_JOB_CONTEXT_KEY, partitionId)
    }
}
