package org.transmartproject.batch.backout

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.db.DatabaseUtil

/**
 * Deletes a concept from i2b2, i2b2_secure and concept_dimension.
 *
 * Doesn't delete dependent objects like tags, facts, assays or counts.
 */
@Slf4j
@Component
@JobScope
class DeleteConceptWriter implements ItemWriter<ConceptPath> {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Autowired
    BackoutContext backoutContext

    @Override
    void write(List<? extends ConceptPath> items) throws Exception {
        log.debug "Deleting concepts: $items"

        List<Integer> counts

        counts = jdbcTemplate.batchUpdate("""
                DELETE FROM ${Tables.CONCEPT_DIMENSION}
                WHERE concept_path = :path""",
                items.collect { [path: it.toString()] } as Map[]) as List
        DatabaseUtil.checkUpdateCountsPermissive counts,
                "Delete from $Tables.CONCEPT_DIMENSION", items

        counts = jdbcTemplate.batchUpdate("""
                DELETE FROM ${Tables.I2B2}
                WHERE c_fullname = :path""",
                items.collect { [path: it.toString()] } as Map[]) as List
        DatabaseUtil.checkUpdateCountsPermissive counts,
                "Delete from $Tables.I2B2", items

        counts = jdbcTemplate.batchUpdate("""
                DELETE FROM ${Tables.I2B2_SECURE}
                WHERE c_fullname = :path""",
                items.collect { [path: it.toString()] } as Map[]) as List
        DatabaseUtil.checkUpdateCountsPermissive counts,
                "Delete from $Tables.I2B2_SECURE", items

        items.collect {
            it == topNode ? it : it.parent
        }.unique().each {
            backoutContext.markFactCountDirty(it)
        }
    }
}
