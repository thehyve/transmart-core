package org.transmartproject.batch.biodata

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Access bio markers dictionary.
 */
@Component
class FillUniprotIdToUniprotNameMappingTasklet implements Tasklet {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    BioMarkerDictionary bioMarkerDictionary

    private Map<String, String> fetchUniprotIdToUniprotName() {
        jdbcTemplate.queryForList(
                """SELECT
                  primary_external_id,
                  bio_marker_name
                 FROM ${Tables.BIO_MARKER}
                 WHERE bio_marker_type = :marker_type""",
                [marker_type: 'PROTEIN'])
                .collectEntries { [it.primary_external_id, it.bio_marker_name] }
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        bioMarkerDictionary.with {
            uniprotIdToUniprotName = fetchUniprotIdToUniprotName()
            contribution.incrementWriteCount(uniprotIdToUniprotName.size())
        }
    }
}
