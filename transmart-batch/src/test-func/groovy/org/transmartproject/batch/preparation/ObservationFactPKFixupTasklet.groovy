package org.transmartproject.batch.preparation

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Changes observation_fact's PK, so it matches i2b2 1.7's.
 */
@Component
class ObservationFactPKFixupTasklet implements Tasklet {

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        jdbcTemplate.execute """
                ALTER TABLE $Tables.OBSERVATION_FACT
                DROP CONSTRAINT observation_fact_pkey """

        jdbcTemplate.execute """
                ALTER TABLE $Tables.OBSERVATION_FACT
                ADD PRIMARY KEY
                (patient_num, concept_cd, modifier_cd, start_date,
                 encounter_num, instance_num, provider_id)"""

        RepeatStatus.FINISHED
    }
}
