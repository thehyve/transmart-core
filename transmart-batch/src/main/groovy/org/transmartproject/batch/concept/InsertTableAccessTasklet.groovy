package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Inserts a an entry in table_access, if necessary.
 *
 * It will try to insert the root node parent of the job's top node. Though
 * table_access can contain entries at deeper levels, this is not supported.
 */
@Slf4j
@Component
@JobScope
class InsertTableAccessTasklet implements Tasklet {
    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNodePath

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Value(Tables.TABLE_ACCESS)
    private SimpleJdbcInsert tableAccessInsert

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ConceptPath rootNode = new ConceptPath('\\' + topNodePath[0])

        if (exists(rootNode)) {
            log.info "$Tables.TABLE_ACCESS already has an entry with c_fullname = $rootNode"
            return RepeatStatus.FINISHED
        }

        int res = tableAccessInsert.execute(
                c_table_cd: rootNode[0], // transmart needs key the same as first element of path
                c_table_name: (Tables.I2B2 - ~/^[^.]+\./).toUpperCase(), // 'I2B2'
                c_protected_access: 'N',
                c_hlevel: 0,
                c_fullname: rootNode.toString(),
                c_name: rootNode[0],
                c_synonym_cd: 'N',
                c_visualattributes: 'CAE',
                c_facttablecolumn: 'concept_cd',
                c_dimtablename: 'concept_dimension',
                c_columnname: 'concept_path',
                c_columndatatype: 'T',
                c_operator: 'LIKE',
                c_dimcode: rootNode.toString(),
        )

        assert res == 1
        log.info("Inserted $rootNode into $Tables.TABLE_ACCESS")
        contribution.incrementWriteCount(1)

        RepeatStatus.FINISHED
    }

    private boolean exists(ConceptPath rootNode) {
        jdbcTemplate.queryForList("SELECT c_fullname FROM $Tables.TABLE_ACCESS WHERE c_fullname = :fullName",
                [fullName: rootNode.toString()], String).size() > 0
    }
}
