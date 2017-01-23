package org.transmartproject.batch.tag

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptPath

/**
 * Database writer of tags
 */
@Slf4j
class TagWriter implements ItemWriter<Tag> {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Value(Tables.I2B2_TAGS)
    SimpleJdbcInsert insert

    @Override
    void write(List<? extends Tag> items) throws Exception {
        List<Integer> i = jdbcTemplate.batchUpdate(
                "DELETE FROM ${Tables.I2B2_TAGS} WHERE path=? AND tag_type=?".toString(),
                items.collect { Tag tag ->
                    [getWithPrependedStudyFolder(tag.conceptFragment), tag.tagTitle].toArray()
                }) as List

        log.info("Deleted ${i.sum()} current tag entries")

        insert.executeBatch(
                items.collect { Tag tag ->
                    [path    : getWithPrependedStudyFolder(tag.conceptFragment),
                     tag     : tag.tagDescription,
                     tag_type: tag.tagTitle,
                     tags_idx: tag.index]
                }.toArray() as Map<String, Object>[])
    }

    private String getWithPrependedStudyFolder(ConceptFragment conceptFragment) {
        (topNode + conceptFragment).path
    }

}
