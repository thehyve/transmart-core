package org.transmartproject.batch.db

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.transmartproject.batch.beans.Postgresql

/**
 * Partitions a table, if necessary, and fills a job context parameter with the
 * number of the partition.
 */
@Slf4j
@Postgresql
class PostgresPartitionTasklet extends AbstractPartitionTasklet {

    /**
     * The column to partition on.
     */
    String partitionByColumn

    /**
     * The sequence from which to extract the partition id.
     */
    String sequence

    /**
     * The primary keys to create on new partitions. List of column names.
     */
    List<String> primaryKey

    /**
     * The foreign keys to create on new partitions.
     * format: [fk_column: [table: table_name, column: referenced_column, onDelete: 'CASCADE']]
     */
    Map<String, Map<String, String>> foreignKeys

    /**
     * The indexes to create on new partitions. List of lists of column names.
     */
    List<List<String>> indexes

    @Autowired
    private SequenceReserver sequenceReserver

    private String getCommentKey() {
        "$partitionByColumn = $partitionByColumnValue"
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
                        comment_key      : commentKey,
                        unqualified_table: unqualifiedTable,
                        schema           : schema,
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

    private String createPartition(int partitionId) {
        String partitionTable = "${tableName}_${partitionId}"
        String unqualifiedPartitionTable = "${unqualifiedTable}_${partitionId}"

        log.info "Creating table $partitionTable"
        jdbcTemplate.update """
                CREATE TABLE $partitionTable(
                    CHECK ($partitionByColumn = '$partitionByColumnValue')
                ) INHERITS ($tableName)""",
                [partitionByColumnValue: partitionByColumnValue]

        jdbcTemplate.update("COMMENT ON TABLE $partitionTable IS " +
                "'${commentKey.replaceAll("'", "''")}'", [:])

        if (primaryKey) {
            log.info "Creating primary key on column(s): ${primaryKey}"
            jdbcTemplate.update """
                ALTER TABLE ${partitionTable}
                ADD CONSTRAINT ${unqualifiedPartitionTable}_pk PRIMARY KEY (${primaryKey.join(', ')})
            """, [:]
        }

        foreignKeys.each { String fkColumn, Map<String, String> refDetails ->
            log.info "Creating foreign key: ${fkColumn} refers to ${refDetails.table}.${refDetails.column}" +
                    " ${refDetails.onDelete ? '(CASCADED)' : ''}"
            jdbcTemplate.update """
                ALTER TABLE ${partitionTable}
                ADD CONSTRAINT ${unqualifiedPartitionTable}_${fkColumn}_fk FOREIGN KEY (${fkColumn})
                REFERENCES ${refDetails.table}($refDetails.column)
                ${refDetails.onDelete ? ' ON DELETE ' + refDetails.onDelete : ''}
            """, [:]
        }

        indexes.eachWithIndex { List<String> columns, i ->
            def indexName = "${unqualifiedPartitionTable}_${i + 1}"
            log.info "Creating index $indexName on columns $columns"

            jdbcTemplate.update """
                CREATE INDEX $indexName ON $partitionTable(
                    ${columns.join(', ')})
            """, [:]
        }

        partitionTable
    }

    @Override
    protected String createPartitionIfNeeded() {
        String partitionTable = currentPartition() // qualified

        if (!partitionTable) {
            int partitionId = sequenceReserver.getNext(sequence)
            partitionTable = createPartition(partitionId)
        }

        partitionTable
    }
}
