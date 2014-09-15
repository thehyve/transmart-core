package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.transmartproject.batch.model.ConceptNode

import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 *
 */
class GatherCurrentConceptsTasklet implements Tasklet, RowMapper<ConceptNode> {

    @Autowired
    ClinicalJobContext jobContext

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['studyId']}")
    String studyId

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        String sql = "select c_fullname, c_basecode  from i2b2metadata.i2b2 " +
                "where sourcesystem_cd = ? and c_hlevel > 0"

        jdbcTemplate.query(sql, [studyId] as Object[], [Types.VARCHAR] as int[], this)
        println "Found ${jobContext.conceptTree.root.allChildren.size()} concepts for this study"

        return RepeatStatus.FINISHED
    }

    @Override
    ConceptNode mapRow(ResultSet rs, int rowNum) throws SQLException {
        String path = rs.getString(1)
        ConceptNode node = jobContext.conceptTree.find(path)
        node.code = Long.parseLong(rs.getString(2))
        node.persisted = true
        node
    }
}
