package org.transmartproject.batch.tag

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptTree

/**
 * Database writer of tags
 */
class TagWriter implements ItemWriter<Tag> {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    ConceptTree conceptTree

    @Value(Tables.I2B2_TAGS)
    SimpleJdbcInsert insert

    @Override
    void write(List<? extends Tag> items) throws Exception {
        jdbcTemplate.batchUpdate("delete from ${Tables.I2B2_TAGS} where path=? and tag_type=?".toString(),
                items.collect { Tag tag ->
                    [getWithPrependedStudyFolder(tag.conceptFragment), tag.tagTitle].toArray()
                })

        insert.executeBatch(
                items.collect { Tag tag ->
                    [path    : getWithPrependedStudyFolder(tag.conceptFragment),
                     tag     : tag.tagDescription,
                     tag_type: tag.tagTitle,
                     tags_idx: tag.index]
                }.toArray() as Map<String, Object>[])
    }

    private String getWithPrependedStudyFolder(ConceptFragment conceptFragment) {
        (conceptTree.topNodePath + conceptFragment).path
    }

}