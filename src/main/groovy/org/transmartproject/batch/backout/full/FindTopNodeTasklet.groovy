package org.transmartproject.batch.backout.full

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.backout.BackoutContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath

/**
 * Detects the existence of the top node.
 */
@BackoutComponent
@JobScope
class FindTopNodeTasklet implements Tasklet {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    private BackoutContext backoutContext

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        Integer found = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ${Tables.I2B2}
                WHERE c_fullname = :name""",
                [name: topNode.toString()], Integer)

        found.times { contribution.incrementReadCount() }

        if (found) {
            // prepare for deletion step
            backoutContext.conceptsToDeleteBeforePromotion = [topNode] as Set
        }

        RepeatStatus.FINISHED
    }
}
