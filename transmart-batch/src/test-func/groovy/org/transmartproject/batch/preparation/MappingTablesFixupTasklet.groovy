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
 * Update mapping tables to include project_id, like i2b2 1.7 has.
 */
@Component
class MappingTablesFixupTasklet implements Tasklet {
    @Autowired
    private JdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        jdbcTemplate.with {
            /* visit mapping */
            execute """
                ALTER TABLE $Tables.ENCOUNTER_MAPPING
                ADD project_id varchar(50) NOT NULL"""

            execute """
                ALTER TABLE $Tables.ENCOUNTER_MAPPING
                DROP CONSTRAINT encounter_mapping_pk"""

            execute """
                ALTER TABLE $Tables.ENCOUNTER_MAPPING
                ADD PRIMARY KEY
                (encounter_ide, encounter_ide_source, project_id, patient_ide, patient_ide_source)"""

            /* patient mapping */
            execute """
                ALTER TABLE $Tables.PATIENT_MAPPING
                ADD project_id varchar(50) NOT NULL"""

            execute """
                ALTER TABLE $Tables.PATIENT_MAPPING
                DROP CONSTRAINT patient_mapping_pk"""

            execute """
                ALTER TABLE $Tables.PATIENT_MAPPING
                ADD PRIMARY KEY
                (patient_ide, patient_ide_source, project_id)"""
        }

        RepeatStatus.FINISHED
    }
}
