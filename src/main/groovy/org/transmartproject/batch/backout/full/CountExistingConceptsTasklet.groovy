package org.transmartproject.batch.backout.full

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.support.StringUtils

/**
 * Trivial tasklet that merely counts the concepts that are
 * not the top node or its parents.
 */
@JobScope
@BackoutComponent
@Slf4j
class CountExistingConceptsTasklet implements Tasklet {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        def res = jdbcTemplate.queryForList("""
                SELECT c_fullname FROM ${Tables.I2B2}
                WHERE c_fullname LIKE :pattern ESCAPE '\\'""",
                [pattern: StringUtils.escapeForLike(topNode.toString()) + '_%'],
                String)

        if (res) {
            if (res.size() > 5) {
                log.error('Cannot continue to complete study deletion (study ' +
                        'top node and patients), there are still ' +
                        "${res.size()}  concepts, first 5 are: ${res[0..<5]}")
            } else {
                log.error('Cannot continue to complete study deletion (study ' +
                        'top node and patients), there are still ' +
                        "${res.size()}  concepts: ${res[0..<5]}")
            }
        }

        res.size().times { contribution.incrementReadCount() }

        RepeatStatus.FINISHED
    }
}
