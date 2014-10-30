package org.transmartproject.batch.clinical

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.transmartproject.batch.model.ConceptNode
import org.transmartproject.batch.model.ConceptTree

import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 * Gets the current concepts (for the study) from database, populating the ConceptTree
 */
@Slf4j
class GatherCurrentConceptsTasklet implements Tasklet, RowMapper<ConceptNode> {

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{clinicalJobContext.conceptTree}")
    ConceptTree conceptTree

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        String rootPath = conceptTree.root.path

        String sql = "select c_fullname, c_basecode, record_id from i2b2metadata.i2b2 " +
                "where sourcesystem_cd = ? or c_fullname = ?"

        List<ConceptNode> concepts = jdbcTemplate.query(
                sql,
                [studyId, rootPath] as Object[],
                [Types.VARCHAR, Types.VARCHAR] as int[],
                this)

        concepts.each {
            log.debug('Found existing concept {}', it)
            contribution.incrementReadCount() //increment reads. unfortunately we have to do this in some loop
        }

        RepeatStatus.FINISHED
    }

    @Override
    ConceptNode mapRow(ResultSet rs, int rowNum) throws SQLException {
        String path = rs.getString(1)
        ConceptNode node = conceptTree.find(path)
        String code = rs.getString(2)
        if (code) {
            node.code = Long.parseLong(code)
        }
        node.isNew = false
        node
    }
}
