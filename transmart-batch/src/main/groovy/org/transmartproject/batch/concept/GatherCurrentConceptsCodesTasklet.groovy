package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

import java.sql.ResultSet

/**
 * Gets the current concepts from database, populating the ConceptTree.
 * It will always load the TOP_NODE for the study.
 *
 * If the job context variable
 * {@link GatherCurrentTreeNodesTasklet#LIST_OF_CONCEPTS_KEY}
 * is set, then the concepts with full names in that list (and their
 * parents will be loaded). Otherwise, all the concepts for the study will be
 * loaded.
 *
 * This should be on an allowStartIfComplete step, as the ConceptTree is not
 * persisted on the job context.
 */
@Component
@JobScopeInterfaced
@Slf4j
class GatherCurrentConceptCodesTasklet implements Tasklet {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    ConceptTree conceptTree

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<String> conceptCodes = jdbcTemplate.query(
                "select CONCEPT_CD from I2B2DEMODATA.CONCEPT_DIMENSION",
                { ResultSet rs, int rowNum ->
                    contribution.incrementReadCount()
                    rs.getString('CONCEPT_CD')
                } as RowMapper<String>
        )

        conceptTree.addToSavedConceptCodes conceptCodes

        RepeatStatus.FINISHED
    }

 }
