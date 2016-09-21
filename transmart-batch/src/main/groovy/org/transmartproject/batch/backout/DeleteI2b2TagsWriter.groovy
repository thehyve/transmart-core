package org.transmartproject.batch.backout

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath

/**
 * Deletes tags associated with the passed concepts.
 */
@Slf4j
@Component
class DeleteI2b2TagsWriter implements ItemWriter<ConceptPath> {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Override
    void write(List<? extends ConceptPath> items) throws Exception {
        log.debug "About to delete tags for paths: $items"
        List<Integer> counts = jdbcTemplate.batchUpdate("""
                DELETE FROM $Tables.I2B2_TAGS
                WHERE path = :path""",
                items.collect { [path: it.toString()] } as Map[]) as List

        int sum = counts.sum()
        if (log.isDebugEnabled()) {
            log.debug "Resulting counts were ${counts} (total: $sum)"
        } else if (sum > 0) {
            log.info "Deleted total of $sum tags in this chunk"
        }
    }
}
