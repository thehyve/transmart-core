package org.transmartproject.batch.backout

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath

/**
 * Delete facts based on concept paths.
 */
@Slf4j
@Component
class DeleteFactsWriter implements ItemWriter<ConceptPath> {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Override
    void write(List<? extends ConceptPath> items) throws Exception {
        List<Integer> counts = jdbcTemplate.batchUpdate("""
                DELETE FROM $Tables.OBSERVATION_FACT OFT
                WHERE EXISTS(SELECT CD.* FROM $Tables.CONCEPT_DIMENSION CD
                WHERE OFT.concept_cd = CD.concept_cd
                    AND CD.concept_path = :path)""",
                items.collect { [path: it.toString()] } as Map[])

        log.info "Deleted a total of ${counts.sum()} facts for " +
                "${items.size()} items in this chunk"
    }
}
